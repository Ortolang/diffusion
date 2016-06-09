package fr.ortolang.diffusion.search;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.index.IndexStoreServiceException;
import fr.ortolang.diffusion.store.json.JsonStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreServiceException;

@Local(SearchService.class)
@Stateless(name = SearchService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class SearchServiceBean implements SearchService {

	private static final Logger LOGGER = Logger.getLogger(SearchServiceBean.class.getName());
	
	private static final String[] OBJECT_TYPE_LIST = new String[] { };
	private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
	
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private IndexStoreService indexStore;
	@EJB
	private JsonStoreService jsonStore;
	@Resource
	private SessionContext ctx;
	
	public SearchServiceBean() {
	}
	
	public IndexStoreService getIndexStoreService() {
		return indexStore;
	}

	public void setIndexStoreService(IndexStoreService store) {
		this.indexStore = store;
	}

	public JsonStoreService getJsonStoreService() {
		return jsonStore;
	}

	public void setJsonStoreService(JsonStoreService jsonStore) {
		this.jsonStore = jsonStore;
	}

	public MembershipService getMembershipService() {
		return membership;
	}

	public void setMembershipService(MembershipService membership) {
		this.membership = membership;
	}

	public AuthorisationService getAuthorisationService() {
		return authorisation;
	}

	public void setAuthorisationService(AuthorisationService authorisation) {
		this.authorisation = authorisation;
	}
	
	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
	}

	public SessionContext getSessionContext() {
		return this.ctx;
	}
	
	@Override
	public List<OrtolangSearchResult> indexSearch(String query) throws SearchServiceException {
		LOGGER.log(Level.FINE, "Performing index search with query: " + query);
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			List<OrtolangSearchResult> checkedResults = new ArrayList<OrtolangSearchResult>();
			long timestamp = System.currentTimeMillis();
			List<OrtolangSearchResult> results = indexStore.search(query);
			LOGGER.log(Level.FINE, "Performed index search in : " + (System.currentTimeMillis()-timestamp));
			for ( OrtolangSearchResult result : results ) {
				try {
					authorisation.checkPermission(result.getKey(), subjects, "read");
					checkedResults.add(result);
				} catch ( AccessDeniedException e ) {
				}
			}
			return checkedResults;
		} catch ( IndexStoreServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException e ) {
			throw new SearchServiceException("unable to perform index search", e);
		}
	}

    @Override
	public List<String> findCollections(HashMap<String, String> fieldsProjection, String content, String group, String limit, String orderProp, String orderDir, HashMap<String, Object> fieldsMap) throws SearchServiceException {
        LOGGER.log(Level.FINE, "find collections");
        String query;
        if (content != null) {
            query = findByContent("Collection", content, fieldsProjection, fieldsMap, group, limit, orderProp, orderDir);
        } else {
            query = findItemsByFields(fieldsProjection, fieldsMap, group, limit, orderProp, orderDir);
        }

        // Execute the query
        List<String> results;
        if (query != null && query.length() > 0) {
            LOGGER.log(Level.FINE, "Performing json search with query : "+query);
            long timestamp1 = System.currentTimeMillis();
            try {
                results = jsonStore.search(query);
                LOGGER.log(Level.FINE, "Performed json search in : "+(System.currentTimeMillis()-timestamp1));
            } catch (JsonStoreServiceException e) {
                results = Collections.emptyList();
                LOGGER.log(Level.FINEST, e.getMessage(), e.fillInStackTrace());
            }
        } else {
            results = Collections.emptyList();
        }
        return results;
	}

    @Override
    public int countCollections(HashMap<String, Object> fieldsMap) throws SearchServiceException {
        LOGGER.log(Level.FINE, "count collections");
        String query = countItemsWithFields(fieldsMap);

        // Execute the query
        int count = 0;
        if (query != null && query.length() > 0) {
            LOGGER.log(Level.FINE, "Performing json search with query : "+query);
            try {
                List<String> results = jsonStore.search(query);
                if (results.size()>0) {
                    count = extractCount(results.get(0));
                }
                LOGGER.log(Level.FINE, "Count : "+count);
            } catch (JsonStoreServiceException | NumberFormatException e) {
                LOGGER.log(Level.FINEST, e.getMessage(), e.fillInStackTrace());
            }
        }
        return count;
    }

    @Override
    public List<String> findProfiles(String content, HashMap<String, String> fieldsProjection) throws SearchServiceException {
        LOGGER.log(Level.FINE, "Gets profile with content : " + content);
        HashMap<String, Object> fieldsMap = new HashMap<String, Object>();
        String query = findByContent("Profile", content, fieldsProjection, fieldsMap, null, null, null, null);

        // Execute the query
        List<String> results;
        if (query != null && query.length() > 0) {
            try {
                results = jsonStore.search(query);
            } catch (JsonStoreServiceException e) {
                results = Collections.emptyList();
                LOGGER.log(Level.FINEST, e.getMessage(), e.fillInStackTrace());
            }
        } else {
            results = Collections.emptyList();
        }
        return results;
    }

    @Override
    public String getCollection(String key) throws SearchServiceException {
        LOGGER.log(Level.FINE, "Gets collection with key : " + key);
        if(key!=null) {
            try {
            	return jsonStore.getDocument(key);
            } catch (JsonStoreServiceException e) {
                LOGGER.log(Level.FINEST, e.getMessage(), e.fillInStackTrace());
            }
        }
        return null;
    }

    @Override
    public String getWorkspace(String wsalias) throws SearchServiceException {
        LOGGER.log(Level.FINE, "Gets workspace with alias : " + wsalias);
        String result = null;
        if(wsalias!=null) {
            // Build the query
            String query = "SELECT * FROM workspace WHERE `meta_ortolang-workspace-json.wsalias` = '" + wsalias + "'";
            // Execute the query
            try {
                List<String> results = jsonStore.search(query);
                if (results.size() == 1) {
                    result = results.get(0);
                }
            } catch (JsonStoreServiceException e) {
                LOGGER.log(Level.FINEST, e.getMessage(), e.fillInStackTrace());
            }
        }
        return result;
    }

    @Override
    public String getEntity(String id) throws SearchServiceException {
        LOGGER.log(Level.FINE, "Gets entity with id : " + id);
        String result = null;
        if(id!=null) {
            // Build the query
            String query = "SELECT * FROM entity WHERE `meta_ortolang-referential-json.id` = '" + id + "'";
            // Execute the query
            try {
                List<String> results = jsonStore.search(query);
                if (results.size() == 1) {
                    result = results.get(0);
                }
            } catch (JsonStoreServiceException e) {
                LOGGER.log(Level.FINEST, e.getMessage(), e.fillInStackTrace());
            }
        }
        return result;
    }
    
	@Override
	public List<String> jsonSearch(String query) throws SearchServiceException {
		LOGGER.log(Level.FINE, "Performing json search with query: " + query);
		try {
			return jsonStore.search(query);
		} catch ( JsonStoreServiceException e ) {
			throw new SearchServiceException("unable to perform json search", e);
		}
	}

    private String findByContent(String cls, String content, HashMap<String, String> fieldsProjection, Map<String, Object> fieldsValue, String group, String limit, String orderProp, String orderDir) {
        StringBuilder queryBuilder = new StringBuilder();
        // SELECT FROM Collection LET $temp = (   SELECT FROM (     TRAVERSE * FROM $current WHILE $depth <= 7   )   WHERE any().toLowerCase().indexOf('dede') > -1 ) WHERE $temp.size() > 0
        queryBuilder.append("SELECT ");

        queryBuilder.append(selectClause(fieldsProjection));
        if (group != null) {
            queryBuilder.append(", count(*)");
        }

        queryBuilder.append(" FROM ").append(cls).append(" LET $temp = ( SELECT FROM ( TRAVERSE * FROM $current WHILE $depth <= 21 ) ");

        StringBuilder whereClause = whereClause(fieldsValue);
        queryBuilder.append(whereClause);

        if (content != null) {
            if (whereClause.length() == 0) {
                queryBuilder.append(" WHERE ");
            } else {
                queryBuilder.append(" AND ");
            }
            queryBuilder.append("any().toLowerCase().indexOf('").append(content.toLowerCase()).append("') > -1 ");
        }

        if (group != null) {
            queryBuilder.append(" GROUP BY ").append("`meta_ortolang-item-json.").append(group).append("`");
        }

        if (orderProp != null) {
            queryBuilder.append(" ORDER BY ").append("`meta_ortolang-item-json.").append(orderProp).append("`");
            if(orderDir!=null) {
                queryBuilder.append(" ").append(orderDir);
            }
        }

        if (limit != null) {
            queryBuilder.append(" LIMIT ").append(limit);
        }

        //      if(type!=null) {
        //          queryBuilder.append("AND `meta_ortolang-item-json.type` = '").append(type).append("'");
        //      }
        queryBuilder.append(" ) WHERE $temp.size() > 0");

        return queryBuilder.toString();
    }

    private String findItemsByFields(HashMap<String, String> fieldsProjection, Map<String, Object> fieldsValue, String group, String limit, String orderProp, String orderDir) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("SELECT ");

        queryBuilder.append(selectClause(fieldsProjection));
        if (group != null) {
            queryBuilder.append(", count(*)");
        }

        queryBuilder.append(" FROM collection");
        queryBuilder.append(whereClause(fieldsValue));
        
        if (group != null) {
            String[] groupPart = group.split("\\.");
            if (groupPart.length > 1) {
                queryBuilder.append(" GROUP BY ").append("`meta_").append(groupPart[0]).append(".").append(groupPart[1]).append("`");
            } else {
                queryBuilder.append(" GROUP BY `").append(group).append("`");
            }
        }

        if (orderProp != null) {
        	String[] orderPropPart = orderProp.split("\\.");
        	if (orderPropPart.length > 1) {
        		queryBuilder.append(" ORDER BY ").append("`meta_").append(orderPropPart[0]).append(".").append(orderPropPart[1]).append("`");
        	} else {
        		queryBuilder.append(" ORDER BY ").append("`").append(orderProp).append("`");
        	}
            
            if(orderDir!=null) {
                queryBuilder.append(" ").append(orderDir);
            }
        }

        if (limit != null) {
            queryBuilder.append(" LIMIT ").append(limit);
        }

        return queryBuilder.toString();
    }

    private String countItemsWithFields(Map<String, Object> fieldsValue) {
        StringBuilder queryBuilder = new StringBuilder();

        queryBuilder.append("SELECT count(*) FROM collection");
        queryBuilder.append(whereClause(fieldsValue));
        
        return queryBuilder.toString();
    }

    private StringBuilder selectClause(HashMap<String, String> fieldsProjection) {
        StringBuilder selectStr = new StringBuilder();
        if (fieldsProjection.isEmpty()) {
            selectStr.append("*");
        } else {
            for (Map.Entry<String, String> fieldProjectionEntry : fieldsProjection.entrySet()) {
                if (selectStr.length() > 0)
                    selectStr.append(",");
                selectStr.append("`").append(fieldProjectionEntry.getKey()).append("`");
                if (!fieldProjectionEntry.getKey().equals(fieldProjectionEntry.getValue())) {
                    selectStr.append(" AS ").append(fieldProjectionEntry.getValue()).append(" ");
                }
            }
        }
        return selectStr;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) private StringBuilder whereClause(Map<String, Object> fields) {
        StringBuilder whereStr = new StringBuilder();
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            if (whereStr.length() > 0)
                whereStr.append(" AND ");
            else
                whereStr.append(" WHERE ");
            if (field.getValue() instanceof String) {
                if (field.getValue().equals("")) {
                    whereStr.append("`").append(field.getKey()).append("` IS NOT NULL");
                } else {
                    whereStr.append("`").append(field.getKey()).append("` = '").append(field.getValue()).append("'");
                }
            } else if (field.getValue() instanceof List && ((List) field.getValue()).size() > 0) {
                whereStr.append("`").append(field.getKey()).append("` IN [").append(arrayValues((List<String>) field.getValue())).append("]");
            } else if (field.getValue() instanceof Long) {
                whereStr.append("`").append(field.getKey().substring(0, field.getKey().length()-2)).append("` ").append(field.getKey().substring(field.getKey().length()-2)).append(" '").append(field.getValue()).append("'");
            }
        }
        return whereStr;
    }

    private StringBuilder arrayValues(List<String> values) {
        StringBuilder valuesStr = new StringBuilder();
        for (String value : values) {
            if (valuesStr.length() > 0)
                valuesStr.append(",");
            valuesStr.append("'").append(value).append("'");
        }
        return valuesStr;
    }

    private int extractCount(String jsonContent) {
        int fieldValue = 0;
        StringReader reader = new StringReader(jsonContent);
        JsonReader jsonReader = Json.createReader(reader);
        try {
            JsonObject jsonObj = jsonReader.readObject();
            fieldValue = jsonObj.getInt("count");
            
        } catch(IllegalStateException | NullPointerException | ClassCastException e) {
            LOGGER.log(Level.WARNING, "No property 'count' in json object", e);
        } catch(JsonException e) {
            LOGGER.log(Level.WARNING, "No property 'count' in json object", e);
        } finally {
            jsonReader.close();
            reader.close();
        }

        return fieldValue;
    }

	@Override
    public String getServiceName() {
        return SearchService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        //TODO provide infos about active connections, config, ports, etc...
        return Collections.emptyMap();
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        return OBJECT_PERMISSIONS_LIST;
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException {
        throw new OrtolangException("This service does not manage any object");
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("This service does not manage any object");
    }

}
