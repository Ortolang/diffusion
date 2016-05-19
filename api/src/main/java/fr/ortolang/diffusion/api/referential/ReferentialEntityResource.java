package fr.ortolang.diffusion.api.referential;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.GZIP;

import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.referential.ReferentialServiceException;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

/**
 * @resourceDescription Operations on Referentials
 */
@Path("/referentialentities")
@Produces({ MediaType.APPLICATION_JSON })
public class ReferentialEntityResource {

    private static final Logger LOGGER = Logger.getLogger(ReferentialEntityResource.class.getName());

    @EJB
    private ReferentialService referential;

    @GET
    @GZIP
    public Response list(@QueryParam(value = "type") String type, @QueryParam(value = "term") String term,@DefaultValue(value= "FR") @QueryParam(value = "lang") String lang) throws ReferentialServiceException {
        LOGGER.log(Level.INFO, "GET /referentialentities?type=" + type + "&term=" + term + "&lang=" + lang);

        GenericCollectionRepresentation<ReferentialEntityRepresentation> representation = new GenericCollectionRepresentation<ReferentialEntityRepresentation> ();
        if(type!=null) {
	        ReferentialEntityType entityType = getEntityType(type);
	        
	    	if(entityType != null) {
	    	    
	    	    List<ReferentialEntity> refs = null;
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

    @GET
    @Path("/{name}")
    @GZIP
    public Response get(@PathParam(value = "name") String name) throws ReferentialServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /referentialentities/" + name);

        ReferentialEntity entity = referential.readEntity(name);
        ReferentialEntityRepresentation representation = ReferentialEntityRepresentation.fromReferentialEntity(entity);
        
        return Response.ok(representation).build();
    }

    @GET
    @Path("/entitytypes")
    @GZIP
    public Response listEntityTypes() {
        LOGGER.log(Level.INFO, "GET /referentialentities/entitytypes");
        return Response.ok(ReferentialEntityType.values()).build();
    }

    @PUT
    @Path("/{name}")
    public Response update(@PathParam(value = "name") String name, ReferentialEntityRepresentation entity) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException {
    	LOGGER.log(Level.INFO, "PUT /referentialentities/" + name);

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
        LOGGER.log(Level.INFO, "DELETE /referentialentities/" + name);
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
