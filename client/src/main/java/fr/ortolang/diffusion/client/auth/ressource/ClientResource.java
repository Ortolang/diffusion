package fr.ortolang.diffusion.client.auth.ressource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import fr.ortolang.diffusion.client.OrtolangClientConfig;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountManager;

@Path("/client")
@Produces({ MediaType.APPLICATION_JSON })
public class ClientResource {

	private static Logger logger = Logger.getLogger(ClientResource.class.getName());
	
	private static boolean initialized = false;
	private static OrtolangClientAccountManager manager;
	private static String authUrl;
	private static String authRealm; 
	private static String appName; 
	private static String callbackUrl; 
	private static Map<String, String> states = new HashMap<String, String> ();
	
	@Context 
	private SecurityContext ctx;
	
	public ClientResource() {
		logger.log(Level.INFO, "Creating new ClientResource");
		if ( !initialized ) {
			String id = "client";
			manager = OrtolangClientAccountManager.getInstance(id);
			authUrl = OrtolangClientConfig.getInstance().getProperty("diffusion.auth.url");
			authRealm = OrtolangClientConfig.getInstance().getProperty("diffusion.auth.realm");
			appName = OrtolangClientConfig.getInstance().getProperty(id + ".app.name");
			callbackUrl = OrtolangClientConfig.getInstance().getProperty(id + ".auth.callback.url");
			initialized = true;
		}
	}
	
	@GET
	@Path("/grant")
	public Response getAuthStatus() {
		logger.log(Level.INFO, "Checking grant status");
		String user = null;
		if ( ctx.getUserPrincipal() != null ) {
			user = ctx.getUserPrincipal().getName();
		} else {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		if ( !manager.exists(user) ) {
			logger.log(Level.FINE, "Generating authentication url");
			String state = UUID.randomUUID().toString();
			states.put(state, user);
			StringBuffer url = new StringBuffer();
			url.append(authUrl).append("/realms/").append(authRealm);
			url.append("/tokens/login?client_id=").append(appName);
			url.append("&state=").append(state);
			url.append("&response_type=code");
			url.append("&redirect_uri=").append(callbackUrl);
			JsonObject jsonObject = Json.createObjectBuilder().add("url", url.toString()).build();
			return Response.ok(jsonObject).build();
		} else {
			return Response.ok().build();
		}
	}
	
	@GET
	@Path("/code")
	@Produces(MediaType.TEXT_HTML)
	public Response setAuthCode(@QueryParam("code") String code, @QueryParam("state") String state) {
		logger.log(Level.INFO, "Setting grant code");
		if ( states.containsKey(state) ) {
			try {
				manager.setAuthorisationCode(states.get(state), code);
			} catch (OrtolangClientAccountException e) {
				return Response.serverError().entity(e.getMessage()).build();
			}
			return Response.ok("<HTML><HEAD></HEAD><BODY onload=\"javascript:window.close();\"></BODY></HTML>").build();
		} else {
			return Response.status(Status.UNAUTHORIZED).build();
		}
	}
	
	@GET
	@Path("/revoke")
	public Response revoke() {
		logger.log(Level.INFO, "Revoking grant");
		String user = null;
		if ( ctx.getUserPrincipal() != null ) {
			user = ctx.getUserPrincipal().getName();
		} else {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		//TODO make something !!
		return Response.ok().build();
	}
	
}
