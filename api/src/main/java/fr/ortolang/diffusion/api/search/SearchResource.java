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
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.core.indexing.OrtolangItemIndexableContent;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
import fr.ortolang.diffusion.search.SearchQuery;
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

@Path("/search") @Produces({ MediaType.APPLICATION_JSON }) public class SearchResource {

    private static final Logger LOGGER = Logger.getLogger(SearchResource.class.getName());

    @EJB private SearchService search;

    public SearchResource() {
    }

    @GET
    @Path("/items")
    public Response searchItems(@Context HttpServletRequest request) {
        
    	return Response.ok(executeQuery(request, OrtolangItemIndexableContent.INDEX)).build();
    }
    
    @GET
    @Path("/items/{id}")
    public Response getItem(@PathParam(value = "id") String id, @QueryParam(value = "type") String type, @QueryParam(value = "version") String version) {
    	if (type==null) {
    		return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'type' is mandatory").build();
    	}
    	if (id==null) {
    		return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'id' is mandatory").build();
    	}
    	
    	String index = OrtolangItemIndexableContent.INDEX;
    	
    	if (version!=null) {
    		index = OrtolangItemIndexableContent.INDEX_ALL;
    		id = id + "-" + version;
    	}
    	//TOOD find an item if type is not specify
    	return Response.ok(search.elasticGet(index, type, id)).build();
    }

    @GET
    @Path("/workspaces/{alias}")
    public Response getWorkspaces(@PathParam(value = "alias") String alias) {
    	if (alias==null) {
    		return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'alias' is mandatory").build();
    	}
    	String document = search.elasticGet(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, alias);
    	return Response.ok(document).build();
    }

    @GET
    @Path("/entities")
    @GZIP
    public Response searchEntities(@Context HttpServletRequest request) {
		return Response.ok(executeQuery(request, ReferentialService.SERVICE_NAME)).build();
    }

    @GET
    @Path("/entities/{id}")
    @GZIP
    public Response getEntity(@PathParam(value = "id") String id, @QueryParam(value = "type") String type) {
//        LOGGER.log(Level.INFO, "GET /search/entities/" + id);
    	if (type==null) {
    		return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'type' is mandatory").build();
    	}
    	if (id==null) {
    		return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'id' is mandatory").build();
    	}
    	//TOOD find an entity if type is not specify
    	String entity = search.elasticGet(ReferentialService.SERVICE_NAME, type, id);
        if (entity != null) {
            return Response.ok(entity).build();
        }
        return Response.status(404).build();
    }

    @GET
    @Path("/persons/{key}")
    @GZIP
    public Response getPerson(@PathParam(value = "key") String key) {
//        LOGGER.log(Level.INFO, "GET /search/entity/person/" + key);
    	if (key==null) {
    		return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'key' is mandatory").build();
    	}
        String person = search.elasticGet(ReferentialService.SERVICE_NAME, ReferentialEntityType.PERSON.name(), key);
        if (person != null) {
        	return Response.ok(person).build();
        }
        return Response.status(404).build();
    }

    @GET
    @Path("/organizations/{key}")
    @GZIP
    public Response getOrganization(@PathParam(value = "key") String key) {
//        LOGGER.log(Level.INFO, "GET /search/entity/person/" + key);
    	if (key==null) {
    		return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'key' is mandatory").build();
    	}
        String person = search.elasticGet(ReferentialService.SERVICE_NAME, ReferentialEntityType.ORGANIZATION.name(), key);
        if (person != null) {
        	return Response.ok(person).build();
        }
        return Response.status(404).build();
    }

    @GET
    @Path("/profiles/{key}")
    @GZIP
    public Response getProfile(@PathParam(value = "key") String key) {
//        LOGGER.log(Level.INFO, "GET /search/profiles/" + key);
    	if (key==null) {
    		return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'key' is mandatory").build();
    	}
        String profile = search.elasticGet(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, key);
        if (profile != null) {
        	return Response.ok(profile).build();
        }
        return Response.status(404).build();
    }

    private SearchResultRepresentation executeQuery(HttpServletRequest request, String index) {
//    	String type = null;
//    	String aggregation = null;
//    	Integer size = null;
//    	String[] includes = null;
//    	String[] excludes = null;
//        Map<String, String[]> queryMap = new HashMap<>();
        SearchQuery query = new SearchQuery();
        query.setIndex(index);
    	for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            if ("type".equals(parameter.getKey())) {
            	query.setType(parameter.getValue()[0]);
            } else if ("size".equals(parameter.getKey())) {
            	try {
            		query.setSize(Integer.valueOf(parameter.getValue()[0]));
            	} catch (NumberFormatException e) {
            	}
            } else if ("includes".equals(parameter.getKey())) {
            	query.setIncludes(parameter.getValue());
            } else if ("excludes".equals(parameter.getKey())) {
            	query.setExcludes(parameter.getValue());
            } else if ("orderProp".equals(parameter.getKey())) {
            	query.setOrderProp(parameter.getValue()[0]);
            } else if ("orderDir".equals(parameter.getKey())) {
            	query.setOrderDir(parameter.getValue()[0]);
            } else if ("aggregations".equals(parameter.getKey())) {
            	LOGGER.log(Level.INFO, "Aggregations : " + parameter.getValue());
            	query.setAggregations(parameter.getValue());
            } else if (!"scope".equals(parameter.getKey())) {
                // Ignore scope param
//            	queryMap.put(parameter.getKey(), parameter.getValue());
            	query.addQuery(parameter.getKey(), parameter.getValue());
//                if (parameter.getKey().endsWith("[]")) {
//                    List<String> paramArr = new ArrayList<String>();
//                    Collections.addAll(paramArr, parameter.getValue());
//                    String[] fieldPart = parameter.getKey().substring(0, parameter.getKey().length() - 2).split("\\.");
//                    if (fieldPart.length > 1) {
//                    queryMap.put(parameter.getKey().substring(0, parameter.getKey().length() - 2), paramArr);
//                    } else {
//                        fieldsMap.put("meta_ortolang-item-json." + parameter.getKey().substring(0, parameter.getKey().length() - 2), paramArr);
//                    }
//                } else {
//                	queryMap.put(parameter.getKey(), parameter.getValue()[0]);
//                    String[] fieldPart = parameter.getKey().split("\\.");
//                    if (fieldPart.length > 1) {
//                        fieldsMap.put("meta_" + parameter.getKey(), parameter.getValue()[0]);
//                    } else {
//                        fieldsMap.put("meta_ortolang-item-json." + parameter.getKey(), parameter.getValue()[0]);
//                    }
//                }
            }
    	}
    	return SearchResultRepresentation.fromSearchResult(search.elasticSearch(query));
    }
    
    

//    @GET
//    @Path("/index")
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
    
//    @GET
//    @Path("/collections")
//    @GZIP
    public Response findCollections(@Context HttpServletRequest request) {
        LOGGER.log(Level.INFO, "GET /search/collections");
        String fields = null;
        String content = null;
        String group = null;
        String limit = null;
        String orderProp = null;
        String orderDir = null;
        Map<String, Object> fieldsMap = new HashMap<>();
        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            if ("fields".equals(parameter.getKey())) {
                fields = parameter.getValue()[0];
            } else if ("content".equals(parameter.getKey())) {
                content = parameter.getValue()[0];
            } else if ("group".equals(parameter.getKey())) {
                group = parameter.getValue()[0];
            } else if ("limit".equals(parameter.getKey())) {
                limit = parameter.getValue()[0];
            } else if ("orderProp".equals(parameter.getKey())) {
                orderProp = parameter.getValue()[0];
            } else if ("orderDir".equals(parameter.getKey())) {
                orderDir = parameter.getValue()[0];
            } else if (!"scope".equals(parameter.getKey())) {
                // Ignore scope param
                if (parameter.getKey().endsWith("[]")) {
                    List<String> paramArr = new ArrayList<String>();
                    Collections.addAll(paramArr, parameter.getValue());
                    String[] fieldPart = parameter.getKey().substring(0, parameter.getKey().length() - 2).split("\\.");
                    if (fieldPart.length > 1) {
                        fieldsMap.put("meta_" + parameter.getKey().substring(0, parameter.getKey().length() - 2), paramArr);
                    } else {
                        fieldsMap.put("meta_ortolang-item-json." + parameter.getKey().substring(0, parameter.getKey().length() - 2), paramArr);
                    }
                } else {
                    String[] fieldPart = parameter.getKey().split("\\.");
                    if (fieldPart.length > 1) {
                        fieldsMap.put("meta_" + parameter.getKey(), parameter.getValue()[0]);
                    } else {
                        fieldsMap.put("meta_ortolang-item-json." + parameter.getKey(), parameter.getValue()[0]);
                    }
                }
            }
        }
        Map<String, String> fieldsProjection = new HashMap<>();
        if (fields != null) {
            for (String field : fields.split(",")) {
                String[] fieldPart = field.split(":");
                if (fieldPart.length > 1) {
                    String[] fieldNamePart = fieldPart[0].split("\\.");
                    if (fieldNamePart.length > 1) {
                        fieldsProjection.put("meta_"+fieldNamePart[0]+"." + fieldNamePart[1], fieldPart[1]);
                    }
                } else {
                    String[] fieldNamePart = field.split("\\.");
                    if (fieldNamePart.length > 1) {
                        fieldsProjection.put("meta_"+fieldNamePart[0]+"." + fieldNamePart[1], null);
                    } else {
                        fieldsProjection.put(field, null);
                    }
                }

            }
        }
        // Execute the query
        List<String> results;
        try {
            results = search.findCollections(fieldsProjection, content, group, limit, orderProp, orderDir, fieldsMap);
        } catch (SearchServiceException e) {
            results = Collections.emptyList();
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        return Response.ok(results).build();
    }

//    @GET
//    @Path("/collections/{key}")
//    @GZIP
    public Response getCollection(@PathParam(value = "key") String key) {
        LOGGER.log(Level.INFO, "GET /search/collections/" + key);
        try {
            String result = search.getCollection(key);
            if (result != null) {
                return Response.ok(result).build();
            }
        } catch (SearchServiceException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        return Response.status(404).build();
    }

//    @GET
//    @Path("/profiles")
//    @GZIP
    public Response findProfiles(@QueryParam(value = "content") String content, @QueryParam(value = "fields") String fields) {
        LOGGER.log(Level.INFO, "GET /search/profiles?content=" + content + (fields != null ? "&fields=" + fields : ""));
        // Sets projections
        Map<String, String> fieldsProjection = new HashMap<String, String>();
        if (fields != null) {
            for (String field : fields.split(",")) {
                String[] fieldPart = field.split(":");
                if (fieldPart.length > 1) {
                    fieldsProjection.put("meta_profile." + fieldPart[0], fieldPart[1]);
                } else {
                    fieldsProjection.put("meta_profile." + field, null);
                }
            }
        }
        // Execute the query
        List<String> results;
        try {
            results = search.findProfiles(content, fieldsProjection);
        } catch (SearchServiceException e) {
            results = Collections.emptyList();
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        return Response.ok(results).build();
    }

//    @GET
//    @Path("/workspaces")
//    @GZIP
    public Response findWorkspaces(@Context HttpServletRequest request) {
        LOGGER.log(Level.INFO, "GET /search/workspaces");
        String fields = null;
        String content = null;
        String group = null;
        String limit = null;
        String orderProp = null;
        String orderDir = null;
        Map<String, Object> fieldsMap = new HashMap<String, Object>();
        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            if ("fields".equals(parameter.getKey())) {
                fields = parameter.getValue()[0];
            } else if ("content".equals(parameter.getKey())) {
                content = parameter.getValue()[0];
            } else if ("group".equals(parameter.getKey())) {
                group = parameter.getValue()[0];
            } else if ("limit".equals(parameter.getKey())) {
                limit = parameter.getValue()[0];
            } else if ("orderProp".equals(parameter.getKey())) {
                orderProp = parameter.getValue()[0];
            } else if ("orderDir".equals(parameter.getKey())) {
                orderDir = parameter.getValue()[0];
            } else if (!"scope".equals(parameter.getKey())) {
                // Ignore scope param
                processFields(parameter, fieldsMap);
            }
        }
        
        Map<String, String> fieldsProjection = new HashMap<>();
        if (fields != null) {
            for (String field : fields.split(",")) {
                if (field.contains(":")) {
                    String[] fieldPart = field.split(":");
                    if (fieldPart[0].contains(".")) {
                        fieldsProjection.put("meta_"+fieldPart[0], fieldPart[1]);
                    } else {
                        fieldsProjection.put(fieldPart[0], fieldPart[1]);
                    }
                } else {
                    if (field.contains(".")) {
                        String[] fieldNamePart = field.split("\\.");
                        fieldsProjection.put("meta_"+field, fieldNamePart[fieldNamePart.length-1]);
                    } else {
                        fieldsProjection.put(field, null);
                    }
                }

            }
        }
        // Execute the query
        List<String> results;
        try {
            results = search.findWorkspaces(content, fieldsProjection, group, limit, orderProp, orderDir, fieldsMap);
        } catch (SearchServiceException e) {
            results = Collections.emptyList();
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        return Response.ok(results).build();
    }

//    @GET
//    @Path("/count/workspaces")
//    @GZIP
    public Response countWorkspaces(@Context HttpServletRequest request) {
        LOGGER.log(Level.INFO, "GET /search/count/workspaces");
        Map<String, Object> fieldsMap = new HashMap<String, Object>();
        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            if (!"fields".equals(parameter.getKey()) && !"content".equals(parameter.getKey()) && !"group".equals(parameter.getKey()) && !"scope".equals(parameter.getKey())) {
                processFields(parameter, fieldsMap);
            }
        }
        try {
            return Response.ok("{\"count\":"+search.countWorkspaces(fieldsMap)+"}").build();
        } catch (SearchServiceException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        return Response.status(400).build();
    }

//    @GET 
//    @Path("/entities") 
//    @GZIP 
    public Response findEntities(@QueryParam(value = "content") String content, @QueryParam(value = "fields") String fields) {
        LOGGER.log(Level.INFO, "GET /search/entities?content=" + content + "&fields=" + fields);
        // Sets projections
        Map<String, String> fieldsProjection = new HashMap<String, String>();
        if (fields != null) {
            for (String field : fields.split(",")) {
                String[] fieldPart = field.split(":");
                if (fieldPart.length > 1) {
                    fieldsProjection.put("meta_ortolang-referential-json." + fieldPart[0], fieldPart[1]);
                } else {
                    fieldsProjection.put("meta_ortolang-referential-json." + field, null);
                }
            }
        }
        // Execute the query
        List<String> results;
        try {
            results = search.findEntities(content, fieldsProjection);
        } catch (SearchServiceException e) {
            results = Collections.emptyList();
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        return Response.ok(results).build();
    }

    private void processFields(Map.Entry<String, String[]> parameter, Map<String, Object> fieldsMap) {
//        Map<String, Object> fieldsMap = new HashMap<>();
        if (parameter.getKey().endsWith("[]")) {
            List<String> paramArr = new ArrayList<String>();
            Collections.addAll(paramArr, parameter.getValue());
            String[] fieldPart = parameter.getKey().substring(0, parameter.getKey().length() - 2).split("\\.");
            if (fieldPart.length > 1) {
                fieldsMap.put("meta_"+parameter.getKey().substring(0, parameter.getKey().length() - 2), paramArr);
            } else {
                fieldsMap.put("meta_ortolang-workspace-json." + parameter.getKey().substring(0, parameter.getKey().length() - 2), paramArr);
            }
        } else {
            String[] fieldPart = parameter.getKey().split("\\.");
            if (fieldPart.length > 1) {
                fieldsMap.put("meta_"+parameter.getKey(), parameter.getValue()[0]);
            } else {
                fieldsMap.put("meta_ortolang-workspace-json." + parameter.getKey(), parameter.getValue()[0]);
            }
        }
//        return fieldsMap;
    }
}
