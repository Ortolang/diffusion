package fr.ortolang.diffusion.api.referential;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.GZIP;

import fr.ortolang.diffusion.api.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.referential.ReferentialServiceException;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;

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
    @Path("/{type}")
    @GZIP
    public Response list(@PathParam(value = "type") String type) throws ReferentialServiceException {
        LOGGER.log(Level.INFO, "GET /referentialentities/" + type);

        ReferentialEntityType entityType = getEntityType(type);

        if(entityType != null) {
        	List<ReferentialEntity> refs = referential.listEntities(entityType);

            GenericCollectionRepresentation<ReferentialEntityRepresentation> representation = new GenericCollectionRepresentation<ReferentialEntityRepresentation> ();
            for(ReferentialEntity ref : refs) {
                representation.addEntry(ReferentialEntityRepresentation.fromReferentialEntity(ref));
            }
            representation.setOffset(0);
            representation.setSize(refs.size());
            representation.setLimit(refs.size());
            return Response.ok(representation).build();
        } else {
        	return Response.status(Status.NOT_FOUND).build();
        }
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
