package fr.ortolang.diffusion.api.rest.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

/**
 * @resourceDescription Operations on Objects
 */
@Path("/metadataformats")
@Produces({ MediaType.APPLICATION_JSON })
public class MetadataFormatResource {

	private Logger logger = Logger.getLogger(MetadataFormatResource.class.getName());

	@EJB
	private CoreService core;
	@EJB
	private BrowserService browser;

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
	
	@GET
	@Path("/{key}/download")
	public void download(final @PathParam(value = "key") String key, @Context HttpServletResponse response) throws OrtolangException, KeyNotFoundException, AccessDeniedException, CoreServiceException, DataNotFoundException, IOException {
		logger.log(Level.INFO, "GET /metadataformats/" + key + "/download");
		
		OrtolangObject object = browser.findObject(key);
		if (object instanceof MetadataFormat) {
			response.setHeader("Content-Disposition", "attachment; filename=" + object.getObjectName());
			response.setContentType(((MetadataFormat) object).getMimeType());
			response.setContentLength((int) ((MetadataFormat) object).getSize());
		}
		InputStream input = core.download(key);
		try {
			IOUtils.copy(input, response.getOutputStream());
		} finally {
			IOUtils.closeQuietly(input);
		}
	}
	
}
