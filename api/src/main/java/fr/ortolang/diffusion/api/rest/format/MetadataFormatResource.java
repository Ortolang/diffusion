package fr.ortolang.diffusion.api.rest.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

/**
 * @resourceDescription Operations on Objects
 */
@Path("/metadataformats")
@Produces({ MediaType.APPLICATION_JSON })
public class MetadataFormatResource {

	private static final Logger LOGGER = Logger.getLogger(MetadataFormatResource.class.getName());

	@EJB
	private CoreService core;
	@EJB
	private BinaryStoreService store;

	@GET
	public Response listMetadataFormat() throws CoreServiceException {
		LOGGER.log(Level.INFO, "GET /metadataformats");
		
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
	
	@GET
	@Path("/download")
	public void download(final @QueryParam(value = "id") String id, final @QueryParam(value = "name") String name, @Context HttpServletResponse response) throws OrtolangException, CoreServiceException, DataNotFoundException, IOException, BinaryStoreServiceException {
		LOGGER.log(Level.INFO, "GET /metadataformats/download");
		
		MetadataFormat format = null;
		if ( id != null && id.length() > 0 ) {
			format = core.findMetadataFormatById(id);
		} else if ( name != null && name.length() > 0 ) {
			format = core.getMetadataFormat(name);
		} else {
			throw new DataNotFoundException("either id or name must be provided in order to find metadata format");
		}
		if ( format != null ) {
			response.setHeader("Content-Disposition", "attachment; filename=" + format.getName());
			response.setContentType(format.getMimeType());
			response.setContentLength((int)format.getSize());
			InputStream input = store.get(format.getSchema());
			try {
				IOUtils.copy(input, response.getOutputStream());
			} finally {
				IOUtils.closeQuietly(input);
			}
		} else {
			throw new DataNotFoundException("unable to find a metadata format for this name or this id");
		}
		
	}
	
}
