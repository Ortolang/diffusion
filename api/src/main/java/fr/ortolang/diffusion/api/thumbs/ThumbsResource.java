package fr.ortolang.diffusion.api.thumbs;

import java.io.File;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.thumbnail.ThumbnailService;
import fr.ortolang.diffusion.thumbnail.ThumbnailServiceException;

@Path("/thumbs")
@Produces({ MediaType.APPLICATION_JSON })
public class ThumbsResource {
	
	private static final Logger LOGGER = Logger.getLogger(ThumbsResource.class.getName());
	
	@EJB
	private BrowserService browser;
	@EJB
	private ThumbnailService service;

	public ThumbsResource() {
	}
	
	@GET
	@Path("/{key}")
	public Response get(@PathParam(value = "key") String key, @QueryParam("size") @DefaultValue("300") int size, @Context Request request) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, OrtolangException, ThumbnailServiceException {
		LOGGER.log(Level.INFO, "GET /thumbs/" + key);

		OrtolangObjectState state = browser.getState(key);
		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		if (state.isLocked()) {
			cc.setMaxAge(691200);
			cc.setMustRevalidate(false);
		} else {
			cc.setMaxAge(0);
			cc.setMustRevalidate(true);
		}
		Date lmd = new Date(state.getLastModification() / 1000 * 1000);
		ResponseBuilder builder = null;
		if (System.currentTimeMillis() - state.getLastModification() > 1000) {
			builder = request.evaluatePreconditions(lmd);
		}
		if (builder == null) {
			//TODO execute this in async !!
			File thumb = service.getThumbnail(key, size);
			builder = Response.ok(thumb).header("Content-Type", ThumbnailService.THUMBS_MIMETYPE);
			builder.lastModified(lmd);
		}

		builder.cacheControl(cc);
		return builder.build();
	}
	
}
