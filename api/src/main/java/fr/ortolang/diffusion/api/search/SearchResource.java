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
import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
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

    @GET @Path("/index") public Response plainTextSearch(@QueryParam(value = "query") String query) throws SearchServiceException {
        LOGGER.log(Level.INFO, "GET /search/index?query=" + query);
        List<OrtolangSearchResult> results;
        if (query != null && query.length() > 0) {
            results = search.indexSearch(query);
        } else {
            results = Collections.emptyList();
        }
        return Response.ok(results).build();
    }

    @GET @Path("/collections") @GZIP public Response findCollections(@Context HttpServletRequest request) {
        LOGGER.log(Level.INFO, "GET /search/collections");
        String fields = null;
        String content = null;
        String group = null;
        String limit = null;
        String orderProp = null;
        String orderDir = null;
        HashMap<String, Object> fieldsMap = new HashMap<String, Object>();
        for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            if (parameter.getKey().equals("fields")) {
                fields = parameter.getValue()[0];
            } else if (parameter.getKey().equals("content")) {
                content = parameter.getValue()[0];
            } else if (parameter.getKey().equals("group")) {
                group = parameter.getValue()[0];
            } else if (parameter.getKey().equals("limit")) {
                limit = parameter.getValue()[0];
            } else if (parameter.getKey().equals("orderProp")) {
                orderProp = parameter.getValue()[0];
            } else if (parameter.getKey().equals("orderDir")) {
                orderDir = parameter.getValue()[0];
            } else if (parameter.getKey().equals("scope")) {
                // Ignore scope param
            } else {
                if (parameter.getKey().endsWith("[]")) {
                    List<String> paramArr = new ArrayList<String>();
                    for (String annotationLevel : parameter.getValue()) {
                        paramArr.add(annotationLevel);
                    }
                    fieldsMap.put("meta_ortolang-item-json." + parameter.getKey().substring(0, parameter.getKey().length() - 2), paramArr);
                } else {
                    fieldsMap.put("meta_ortolang-item-json." + parameter.getKey(), parameter.getValue()[0]);
                }
            }
        }
        HashMap<String, String> fieldsProjection = new HashMap<String, String>();
        if (fields != null) {
            for (String field : fields.split(",")) {
                String[] fieldPart = field.split(":");
                if (fieldPart.length > 1) {
                    fieldsProjection.put("meta_ortolang-item-json." + fieldPart[0], fieldPart[1]);
                } else {
                    fieldsProjection.put("meta_ortolang-item-json." + field, field);
                }

            }
        }
        // Execute the query
        List<String> results;
        long count = 0;
        try {
            results = search.findCollections(fieldsProjection, content, group, limit, orderProp, orderDir, fieldsMap);
            count = search.countCollections(fieldsMap);
        } catch (SearchServiceException e) {
            results = Collections.emptyList();
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }

        GenericCollectionRepresentation<String> representation = new GenericCollectionRepresentation<String>();
        for (String key : results) {
            representation.addEntry(key);
        }
        representation.setSize(count);
        return Response.ok(representation).build();
    }

    @GET @Path("/profiles") @GZIP public Response findProfiles(@QueryParam(value = "content") String content, @QueryParam(value = "fields") String fields) {
        LOGGER.log(Level.INFO, "GET /search/profiles?content=" + content + "&fields=" + fields);
        // Sets projections
        HashMap<String, String> fieldsProjection = new HashMap<String, String>();
        if (fields != null) {
            for (String field : fields.split(",")) {
                String[] fieldPart = field.split(":");
                if (fieldPart.length > 1) {
                    fieldsProjection.put("meta_ortolang-item-json." + fieldPart[0], fieldPart[1]);
                } else {
                    fieldsProjection.put("meta_ortolang-item-json." + field, field);
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

    @GET @Path("/collections/{key}") @GZIP public Response getCollection(@PathParam(value = "key") String key) {
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

    @GET @Path("/workspaces/{alias}") @GZIP public Response getWorkspace(@PathParam(value = "alias") String alias) {
        LOGGER.log(Level.INFO, "GET /metdata/workspaces/" + alias);
        try {
            String result = search.getWorkspace(alias);
            if (result != null) {
                return Response.ok(result).build();
            }
        } catch (SearchServiceException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        return Response.status(404).build();
    }

    @GET @Path("/entities/{id}") @GZIP public Response getEntity(@PathParam(value = "id") String id) {
        LOGGER.log(Level.INFO, "GET /search/entities/" + id);
        try {
            String result = search.getEntity(id);
            if (result != null) {
                return Response.ok(result).build();
            }
        } catch (SearchServiceException e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
        }
        return Response.status(404).build();
    }

}
