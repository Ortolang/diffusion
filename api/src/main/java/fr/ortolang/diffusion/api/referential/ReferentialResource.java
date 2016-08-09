package fr.ortolang.diffusion.api.referential;

import java.net.URI;

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
 * Copyright (C) 2013 - 2016 Ortolang Team
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

import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;

import org.jboss.resteasy.annotations.GZIP;

import fr.ortolang.diffusion.api.ApiUriBuilder;
import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.referential.ReferentialServiceException;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

/**
 * @resourceDescription Operations on Referentials
 */
@Path("/referential")
@Produces({ MediaType.APPLICATION_JSON })
public class ReferentialResource {

    private static final Logger LOGGER = Logger.getLogger(ReferentialResource.class.getName());

    @EJB
    private ReferentialService referential;

    @GET
    @GZIP
    public Response list(@QueryParam(value = "type") String type, @QueryParam(value = "term") String term,@DefaultValue(value= "FR") @QueryParam(value = "lang") String lang) throws ReferentialServiceException {
        LOGGER.log(Level.INFO, "GET /referential?type=" + type + "&term=" + term + "&lang=" + lang);

        GenericCollectionRepresentation<ReferentialEntityRepresentation> representation = new GenericCollectionRepresentation<> ();
        if(type!=null) {
	        ReferentialEntityType entityType = getEntityType(type);
	        
	    	if(entityType != null) {
	    	    
	    	    List<ReferentialEntity> refs;
	    	    if(term!=null) {
	    	        refs = referential.findEntitiesByTerm(entityType, term, lang);
	    	    } else {
	    	        refs = referential.listEntities(entityType);
	    	    }
	            
	            for(ReferentialEntity ref : refs) {
	                representation.addEntry(ReferentialEntityRepresentation.fromReferentialEntity(ref));
	            }
	            representation.setOffset(0);
	            representation.setSize(refs.size());
	            representation.setLimit(refs.size());
	        } else {
	            throw new ReferentialServiceException("type unknown");
	        }
        }
        return Response.ok(representation).build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @GZIP
    public Response create(@FormParam(value = "name") String name, @FormParam("type") String type, @FormParam("content") String content) throws ReferentialServiceException, AccessDeniedException, KeyAlreadyExistsException, IllegalArgumentException, UriBuilderException, URISyntaxException {
    	LOGGER.log(Level.INFO, "POST /referential");
    	if (name == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'name' is mandatory").build();
        }
    	if (type == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'type' is mandatory").build();
        }
    	ReferentialEntity entity;
    	ReferentialEntityType entityType = getEntityType(type);
    	if(entityType!=null) {
    		entity = referential.createEntity(name, entityType, content);
    	} else {
    		return Response.status(Response.Status.BAD_REQUEST).entity("representation does not contains a valid type").build();
    	}
    	URI location = ApiUriBuilder.getApiUriBuilder().path(ReferentialResource.class).path(entity.getKey()).build();
    	ReferentialEntityRepresentation entityRepresentation = ReferentialEntityRepresentation.fromReferentialEntity(entity);
    	return Response.created(location).entity(entityRepresentation).build();
    }


    @GET
    @Path("/{name}")
    @GZIP
    public Response get(@PathParam(value = "name") String name) throws ReferentialServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /referential/" + name);

        ReferentialEntity entity = referential.readEntity(name);
        ReferentialEntityRepresentation representation = ReferentialEntityRepresentation.fromReferentialEntity(entity);
        
        return Response.ok(representation).build();
    }

    @GET
    @Path("/types")
    @GZIP
    public Response listEntityTypes() {
        LOGGER.log(Level.INFO, "GET /referential/types");
        return Response.ok(ReferentialEntityType.values()).build();
    }

    @PUT
    @Path("/{name}")
    public Response update(@PathParam(value = "name") String name, ReferentialEntityRepresentation entity) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException {
    	LOGGER.log(Level.INFO, "PUT /referential/" + name);

    	ReferentialEntityType entityType = getEntityType(entity.getType());
    	if(entityType!=null) {
    		referential.updateEntity(name, entityType, entity.getContent(), entity.getBoost());
    	} else {
    		return Response.status(Response.Status.BAD_REQUEST).entity("representation does not contains a valid type").build();
    	}
    	return Response.ok().build();
    }

    @DELETE
    @Path("/{name}")
    public Response delete(@PathParam(value = "name") String name) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "DELETE /referential/" + name);
        if (name == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'name' is mandatory").build();
        }
        referential.deleteEntity(name);
        return Response.noContent().build();
    }
    
    private ReferentialEntityType getEntityType(String type) {
    	try {
    		return ReferentialEntityType.valueOf(type);
    	} catch(IllegalArgumentException e) {
    		LOGGER.log(Level.WARNING, "Asking entity type unknown : " + type);
    		return null;
    	}
    }
}
