package fr.ortolang.diffusion.api.referentiel;

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

import fr.ortolang.diffusion.api.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.referentiel.ReferentielService;
import fr.ortolang.diffusion.referentiel.ReferentielServiceException;
import fr.ortolang.diffusion.referentiel.entity.OrganizationEntity;
import fr.ortolang.diffusion.registry.KeyNotFoundException;


/**
 * @resourceDescription Operations on Referentiels
 */
@Path("/referentiels")
@Produces({ MediaType.APPLICATION_JSON })
public class ReferentielEntityResource {

    private static final Logger LOGGER = Logger.getLogger(ReferentielEntityResource.class.getName());

    @EJB
    private ReferentielService referentiel;

	@GET
	@Path("/organizations")
	public Response list() throws ReferentielServiceException {
		LOGGER.log(Level.INFO, "GET /organizations");
		
		List<OrganizationEntity> refs = referentiel.listOrganizationEntities();
		
		GenericCollectionRepresentation<OrganizationEntityRepresentation> representation = new GenericCollectionRepresentation<OrganizationEntityRepresentation> ();
		for(OrganizationEntity ref : refs) {
		    representation.addEntry(OrganizationEntityRepresentation.fromReferentielEntity(ref));
		}
		representation.setOffset(0);
		representation.setSize(refs.size());
		representation.setLimit(refs.size());

		return Response.ok(representation).build();
	}

	@GET
	@Path("/organizations/{key}")
	public Response get(@PathParam(value = "key") String key) throws ReferentielServiceException, KeyNotFoundException {
		LOGGER.log(Level.INFO, "GET /referentiels/" + key);
				
		OrganizationEntityRepresentation representation = OrganizationEntityRepresentation.fromReferentielEntity(referentiel.readOrganizationEntity(key));
		
		return Response.ok(representation).build();
	}

}
