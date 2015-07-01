package fr.ortolang.diffusion.api.organization;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.api.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.referentiel.ReferentielService;
import fr.ortolang.diffusion.referentiel.ReferentielServiceException;
import fr.ortolang.diffusion.referentiel.entity.Organization;


/**
 * @resourceDescription Operations on Organization
 */
@Path("/organizations")
@Produces({ MediaType.APPLICATION_JSON })
public class OrganizationResource {

    private static final Logger LOGGER = Logger.getLogger(OrganizationResource.class.getName());

    @EJB
    private ReferentielService referentiel;

	@GET
	public Response list() throws ReferentielServiceException {
		LOGGER.log(Level.INFO, "GET /organizations");
		
		List<Organization> orgs = referentiel.listOrganizations();
		
		GenericCollectionRepresentation<OrganizationRepresentation> representation = new GenericCollectionRepresentation<OrganizationRepresentation> ();
		for(Organization org : orgs) {
		    representation.addEntry(OrganizationRepresentation.fromOrganization(org));
		}
		representation.setOffset(0);
		representation.setSize(orgs.size());
		representation.setLimit(orgs.size());

		return Response.ok(representation).build();
	}
	
}
