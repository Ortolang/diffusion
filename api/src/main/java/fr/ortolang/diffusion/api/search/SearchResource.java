package fr.ortolang.diffusion.api.search;

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

import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.search.SearchServiceException;

import org.jboss.resteasy.annotations.GZIP;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/search")
@Produces({ MediaType.APPLICATION_JSON })
public class SearchResource {

    private static final Logger LOGGER = Logger.getLogger(SearchResource.class.getName());

    @EJB
    private SearchService search;

    public SearchResource() {
    }

    @GET
    @Path("/index")
    public Response plainTextSearch(@QueryParam(value = "query") String query) throws SearchServiceException {
        LOGGER.log(Level.INFO, "GET /search/index?query=" + query);
        List<OrtolangSearchResult> results;
        if (query != null && query.length() > 0) {
            results = search.indexSearch(query);
        } else {
            results = Collections.emptyList();
        }
        return Response.ok(results).build();
    }

    @POST
    @Path("/json")
    @Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
    @GZIP
    public Response jsonSearch(@FormParam(value = "query") String query) {
        LOGGER.log(Level.INFO, "POST /search/json");
        List<String> results;
        if (query != null && query.length() > 0) {
            try {
                results = search.jsonSearch(query);
            } catch (SearchServiceException e) {
                results = Collections.emptyList();
            }
        } else {
            results = Collections.emptyList();
        }
        return Response.ok(results).build();
    }
    

    @GET
    @Path("/collections")
    @GZIP
    public Response listCollections(@Context HttpServletRequest request) {
    	
    	String fields = null;
    	String content = null;
    	String group = null;
		HashMap<String, Object> fieldsMap = new HashMap<String, Object>();
    	for(Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
    		if(parameter.getKey().equals("fields")) {
    			fields = parameter.getValue()[0];
    		} else if(parameter.getKey().equals("content")) {
    			content = parameter.getValue()[0];
    		} else if(parameter.getKey().equals("group")) {
    			group = parameter.getValue()[0];
    		} else {
    			if(parameter.getKey().endsWith("[]")) {
    				List<String> paramArr = new ArrayList<String>();
    				for(String annotationLevel : parameter.getValue()) {
    					paramArr.add(annotationLevel);
    				}
    				fieldsMap.put("meta_ortolang-item-json."+parameter.getKey().substring(0, parameter.getKey().length()-2), paramArr);
    			} else {
    				fieldsMap.put("meta_ortolang-item-json."+parameter.getKey(), parameter.getValue()[0]);
    			}
    		}
    	}
    	LOGGER.log(Level.INFO, "GET /metadata/collections");
//    	LOGGER.log(Level.INFO, "GET /metdata/collections?type=" + type + "&content=" + content + "&producer=" 
//    		+ producer + "&contributor=" + contributor + "&group=" + group + "&fields=" + fields + "&statusOfUse=" + statusOfUse
//    		 + "&annotationLevels=" + annotationLevels);
    	
    	// Build the query
    	String query;
    	HashMap<String, String> fieldsProjection = new HashMap<String, String>();
		if(fields!=null) {
			for(String field : fields.split(",")) {
				String[] fieldPart = field.split(":");
				if(fieldPart.length>1) {
					fieldsProjection.put("meta_ortolang-item-json."+fieldPart[0], fieldPart[1]);
				} else {
					fieldsProjection.put("meta_ortolang-item-json."+field, field);
				}
				
			}
		}
    	if(content!=null) {
    		query = findByContent("Collection", content, fieldsProjection, fieldsMap, group);
    	} else {
    		query = findItemsByFields(fieldsProjection, fieldsMap, group);
    	}
    	
    	// Execute the query
    	List<String> results;
        if (query != null && query.length() > 0) {
            try {
                results = search.jsonSearch(query);
            } catch (SearchServiceException e) {
                results = Collections.emptyList();
                LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
            }
        } else {
            results = Collections.emptyList();
        }
        return Response.ok(results).build();
    }

    @GET
    @Path("/profiles")
    @GZIP
    public Response listProfiles(@QueryParam(value = "content") String content, @QueryParam(value = "fields") String fields) {
    	LOGGER.log(Level.INFO, "GET /metadata/profiles?content=" + content + "&fields=" + fields);

    	// Build the query
    	HashMap<String, String> fieldsProjection = new HashMap<String, String>();
    	if(fields!=null) {
			for(String field : fields.split(",")) {
				String[] fieldPart = field.split(":");
				if(fieldPart.length>1) {
					fieldsProjection.put("meta_ortolang-item-json."+fieldPart[0], fieldPart[1]);
				} else {
					fieldsProjection.put("meta_ortolang-item-json."+field, field);
				}
				
			}
		}
    	
    	HashMap<String, Object> fieldsMap = new HashMap<String, Object>();
    	String query = findByContent("Profile", content, fieldsProjection, fieldsMap, null);
    	
    	// Execute the query
    	List<String> results;
        if (query != null && query.length() > 0) {
            try {
                results = search.jsonSearch(query);
            } catch (SearchServiceException e) {
                results = Collections.emptyList();
                LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
            }
        } else {
            results = Collections.emptyList();
        }
        return Response.ok(results).build();
    }

    @GET
    @Path("/collections/{key}")
    @GZIP
    public Response getCollections(@PathParam(value = "key") String key) {
    	LOGGER.log(Level.INFO, "GET /metdata/collections/" + key);
    	//TODO check if alias is null or empty
    	// Build the query
    	String query = "SELECT * FROM collection WHERE key = '" + key + "'";
    	
    	// Execute the query
        try {
            List<String> results = search.jsonSearch(query);
            if(results.size() == 1) {
            	return Response.ok(results.get(0)).build();
            }
        } catch (SearchServiceException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        //TODO Get Exception and return message "KeyNotFound" or "CollectionNotFound"
        return Response.status(404).build();
    }
    

    @GET
    @Path("/workspaces/{alias}")
    @GZIP
    public Response getWorkspaces(@PathParam(value = "alias") String alias) {
    	LOGGER.log(Level.INFO, "GET /metdata/workspaces/" + alias);
    	//TODO check if alias is null or empty
    	// Build the query
    	String query = "SELECT * FROM workspace WHERE `meta_ortolang-workspace-json.wsalias` = '" + alias + "'";
    	
    	// Execute the query
        try {
            List<String> results = search.jsonSearch(query);
            if(results.size() == 1) {
            	return Response.ok(results.get(0)).build();
            }
        } catch (SearchServiceException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        //TODO Get Exception and return message "AliasNotFound" or "WorkspaceNotFound"
        return Response.status(404).build();
    }
    
    
	private String findItemsByFields(HashMap<String, String> fieldsProjection, Map<String,Object> fieldsValue, String group) {
    	StringBuilder queryBuilder = new StringBuilder();
    	
    	queryBuilder.append("SELECT ");
    	
    	queryBuilder.append(selectClause(fieldsProjection));
    	if(group!=null) {
    		queryBuilder.append(", count(*)");
    	}
    	
    	queryBuilder.append(" FROM collection");
    	queryBuilder.append(whereClause(fieldsValue));
    	
    	if(group!=null) {
    		queryBuilder.append(" GROUP BY ").append("`meta_ortolang-item-json.").append(group).append("`");
    	}
    	
    	return queryBuilder.toString();
    }

    private String findByContent(String cls, String content, HashMap<String, String> fieldsProjection, Map<String,Object> fieldsValue, String group) {
    	StringBuilder queryBuilder = new StringBuilder();
    	// SELECT FROM Collection LET $temp = (   SELECT FROM (     TRAVERSE * FROM $current WHILE $depth <= 7   )   WHERE any().toLowerCase().indexOf('dede') > -1 ) WHERE $temp.size() > 0
    	queryBuilder.append("SELECT ");
    	
    	queryBuilder.append(selectClause(fieldsProjection));
    	if(group!=null) {
    		queryBuilder.append(", count(*)");
    	}
    	
    	queryBuilder.append(" FROM ").append(cls).append(" LET $temp = ( SELECT FROM ( TRAVERSE * FROM $current WHILE $depth <= 21 ) ");
    	
    	StringBuilder whereClause = whereClause(fieldsValue);
    	queryBuilder.append(whereClause);
    	
    	if(content!=null) {
    		if(whereClause.length()==0) {
        		queryBuilder.append(" WHERE ");
        	} else {
        		queryBuilder.append(" AND ");
        	}
        	queryBuilder.append("any().toLowerCase().indexOf('").append(content).append("') > -1 ");
    	}
    	
    	if(group!=null) {
    		queryBuilder.append(" GROUP BY ").append("`meta_ortolang-item-json.").append(group).append("`");
    	}
    	
//    	if(type!=null) {
//    		queryBuilder.append("AND `meta_ortolang-item-json.type` = '").append(type).append("'");
//    	}
    	queryBuilder.append(" ) WHERE $temp.size() > 0");
    	
    	return queryBuilder.toString();
    }

    private StringBuilder selectClause(HashMap<String, String> fieldsProjection) {
    	StringBuilder selectStr = new StringBuilder();
    	if(fieldsProjection.isEmpty()) {
    		selectStr.append("*");
    	} else {
    		for(Map.Entry<String, String> fieldProjectionEntry : fieldsProjection.entrySet()) {
    			if(selectStr.length()>0)
    				selectStr.append(",");
    			selectStr.append("`").append(fieldProjectionEntry.getKey()).append("`");
    			if(!fieldProjectionEntry.getKey().equals(fieldProjectionEntry.getValue())) {
    				selectStr.append(" AS ").append(fieldProjectionEntry.getValue()).append(" ");
    			}
    		}
    	}
    	return selectStr;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private StringBuilder whereClause(Map<String,Object> fields) {
    	StringBuilder whereStr = new StringBuilder();
    	for(Map.Entry<String, Object> field : fields.entrySet()) {
			if(whereStr.length()>0)
				whereStr.append(" AND ");
			else
				whereStr.append(" WHERE ");
    		if(field.getValue() instanceof String) {
    			whereStr.append("`").append(field.getKey()).append("` = '").append(field.getValue()).append("'");
    		} else if(field.getValue() instanceof List && ((List) field.getValue()).size()>0) {
    			whereStr.append("`").append(field.getKey()).append("` IN [")
    				.append(arrayValues((List<String>) field.getValue())).append("]");
    		}
    	}
    	return whereStr;
    }
    
    private StringBuilder arrayValues(List<String> values) {
		StringBuilder valuesStr = new StringBuilder();
		for(String value : values) {
			if(valuesStr.length()>0)
				valuesStr.append(",");
			valuesStr.append("'").append(value).append("'");
		}
		return valuesStr;
    }
    

}
