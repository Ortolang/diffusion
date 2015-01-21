package fr.ortolang.diffusion.client.auth.ressource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.ortolang.diffusion.client.OrtolangClientConfig;
import fr.ortolang.diffusion.client.auth.AuthenticationException;
import fr.ortolang.diffusion.client.auth.AuthenticationManager;

@Path("/client")
@Produces({ MediaType.APPLICATION_JSON })
public class AuthenticationResource {

	private static Logger logger = Logger.getLogger(AuthenticationResource.class.getName());
	
	private static AuthenticationManager manager = AuthenticationManager.getInstance();
	private static Map<String, String> states = new HashMap<String, String> ();
	private String authUrl;
	private String authRealm;
	private String appname;
	private String callbackUrl;
	
	public AuthenticationResource() {
		authUrl = OrtolangClientConfig.getInstance().getProperty("api.rest.auth.server.url");
		authRealm = OrtolangClientConfig.getInstance().getProperty("api.rest.auth.realm");
		appname = OrtolangClientConfig.getInstance().getProperty("api.rest.auth.app.name");
		callbackUrl = OrtolangClientConfig.getInstance().getProperty("api.rest.auth.callback.url");
	}
	
	@GET
	@Path("/auth")
	public Response getAuthStatus(@Context HttpServletRequest request) {
		logger.log(Level.INFO, "Checking grant status");
		String user = null;
		if ( request.getUserPrincipal() != null ) {
			user = request.getUserPrincipal().getName();
		} else {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		if ( !manager.exists(user) ) {
			logger.log(Level.FINE, "Generating authentication url");
			String state = UUID.randomUUID().toString();
			states.put(state, user);
			StringBuffer url = new StringBuffer();
			url.append(authUrl).append("/realms/").append(authRealm);
			url.append("/tokens/login?client_id=").append(appname);
			url.append("&state=").append(state);
			url.append("&response_type=code");
			url.append("&redirect_uri=").append(callbackUrl);
	        return Response.ok(url.toString()).build();
		} else {
			return Response.ok().build();
		}
	}
	
	@GET
	@Path("/code")
	public Response setAuthCode(@Context HttpServletRequest request, @QueryParam("code") String code, @QueryParam("state") String state) {
		logger.log(Level.INFO, "Setting grant code");
		if ( states.containsKey(state) ) {
			try {
				manager.setAuthorisationCode(states.get(state), code);
			} catch (AuthenticationException e) {
				return Response.serverError().entity(e.getMessage()).build();
			}
			return Response.ok().build();
		} else {
			return Response.status(Status.UNAUTHORIZED).build();
		}
	}
	
	@GET
	@Path("/revoke")
	public Response revoke(@Context HttpServletRequest request) {
		logger.log(Level.INFO, "Revoking grant");
		String user = null;
		if ( request.getUserPrincipal() != null ) {
			user = request.getUserPrincipal().getName();
		} else {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		//TODO make something !!
		return Response.ok().build();
	}
	
}
