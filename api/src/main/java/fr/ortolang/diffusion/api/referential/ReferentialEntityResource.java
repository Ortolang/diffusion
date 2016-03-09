package fr.ortolang.diffusion.api.referential;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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

/**
 * @resourceDescription Operations on Referentiels
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
    
    private ReferentialEntityType getEntityType(String type) {
    	try {
    		return ReferentialEntityType.valueOf(type);
    	} catch(IllegalArgumentException e) {
    		LOGGER.log(Level.WARNING, "Asking entity type unknown : " + type);
    		return null;
    	}
    }
}
