package fr.ortolang.diffusion.api.rest.format;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.MetadataFormat;

/**
 * @resourceDescription Operations on Objects
 */
@Path("/metadataformats")
@Produces({ MediaType.APPLICATION_JSON })
public class MetadataFormatResource {

	private Logger logger = Logger.getLogger(MetadataFormatResource.class.getName());

	@EJB
	private CoreService core;

	@GET
	@Template( template="metadataformats/list.vm", types={MediaType.TEXT_HTML})
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response listMetadataFormat() throws CoreServiceException {
		logger.log(Level.INFO, "GET /metadataformats");
		
		List<MetadataFormat> mdfs = core.listMetadataFormat();
		
		GenericCollectionRepresentation<MetadataFormat> representation = new GenericCollectionRepresentation<MetadataFormat>();
		for (MetadataFormat mdf : mdfs) {
			representation.addEntry(mdf);
		}
		representation.setOffset(0);
		representation.setSize(mdfs.size());
		representation.setLimit(mdfs.size());
		return Response.ok(representation).build();
	}
	
}
