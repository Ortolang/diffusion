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

import java.util.Collections;
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
    @Path("/profiles")
    @GZIP
    public Response searchProfiles(@Context HttpServletRequest request) {
		return Response.ok(executeQuery(request, MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE)).build();
    }

    @GET
    @Path("/profiles/{key}")
    @GZIP
    public Response getProfile(@PathParam(value = "key") String key) {
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
    	return executeQuery(request, index, null);
    }
    
    private SearchResultRepresentation executeQuery(HttpServletRequest request, String index, String type) {
        SearchQuery query = new SearchQuery();
        query.setIndex(index);
        if (type!=null) {
        	query.setType(type);
        }
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
            	query.addQuery(parameter.getKey(), parameter.getValue());
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
}
