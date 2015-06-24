package fr.ortolang.diffusion.api.preview;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
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

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.preview.PreviewService;
import fr.ortolang.diffusion.preview.PreviewServiceException;
import fr.ortolang.diffusion.preview.entity.Preview;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/previews")
@Produces({ MediaType.APPLICATION_JSON })
public class PreviewResource {
	
	private static final Logger LOGGER = Logger.getLogger(PreviewResource.class.getName());
	
	@EJB
	private BrowserService browser;
	@EJB
	private PreviewService preview;

	public PreviewResource() {
	}
	
	@GET
	@Path("/{key}")
	public Response get(@PathParam(value = "key") String key, @Context Request request) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, OrtolangException, PreviewServiceException {
		LOGGER.log(Level.INFO, "GET /previews/" + key);

		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		cc.setMaxAge(0);
		cc.setMustRevalidate(true);
		Date lmd = new Date(preview.getLastModification() / 1000 * 1000);
		ResponseBuilder builder = null;
		if (System.currentTimeMillis() - preview.getLastModification() > 1000) {
			builder = request.evaluatePreconditions(lmd);
		}

		if (builder == null) {
			List<PreviewRepresentation> representation = new ArrayList<PreviewRepresentation>();
			OrtolangObject object = browser.findObject(key);
			if ( object.getObjectIdentifier().getService().equals(CoreService.SERVICE_NAME) ) {
				if ( object.getObjectIdentifier().getType().equals(Collection.OBJECT_TYPE ) ) {
					List<Preview> previews = preview.list(extractKeys(((Collection)object).getElements()));
					for ( Preview preview : previews ) {
						representation.add(new PreviewRepresentation(preview.getKey(), getLargePreviewUrl(key), getSmallPreviewUrl(key)));
					}
				} else if ( object.getObjectIdentifier().getType().equals(DataObject.OBJECT_TYPE ) ) {
					Preview prev = preview.getPreview(key);
					representation.add(new PreviewRepresentation(prev.getKey(), getLargePreviewUrl(key), getSmallPreviewUrl(key)));
				}
			}
			builder = Response.ok(representation);
			builder.lastModified(lmd);
		}

		builder.cacheControl(cc);
		return builder.build();
	}
	
	private List<String> extractKeys(Set<CollectionElement> elements) {
		List<String> keys = new ArrayList<String> ();
		for ( CollectionElement element : elements ) {
			keys.add(element.getKey());
		}
		return keys;
	}
	
	private String getBaseContentApiUrl() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(OrtolangConfig.getInstance().getProperty("server.protocol"));
		buffer.append("://");
		buffer.append(OrtolangConfig.getInstance().getProperty("server.host"));
		buffer.append(":");
		buffer.append(OrtolangConfig.getInstance().getProperty("server.port"));
		buffer.append(OrtolangConfig.getInstance().getProperty("api.content.base.path"));
		return buffer.toString();
	}
	
	private String getLargePreviewUrl(String key) {
		return getBaseContentApiUrl() + "/" + key + "?preview=large";
	}
	
	private String getSmallPreviewUrl(String key) {
		return getBaseContentApiUrl() + "/" + key + "?preview=small";
	}

}
