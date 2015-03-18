package fr.ortolang.diffusion.client.auth.ressource;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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

import fr.ortolang.diffusion.client.OrtolangClient;
import fr.ortolang.diffusion.client.OrtolangClientConfig;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;

@Path("/client")
@Produces({ MediaType.APPLICATION_JSON })
public class ClientResource {

	private static Logger logger = Logger.getLogger(ClientResource.class.getName());
	
	private static boolean initialized = false;
	private static String authUrl;
	private static String authRealm; 
	private static String appName; 
	private static String callbackUrl; 
	private static Map<String, String> states = new HashMap<String, String> ();
	private static OrtolangClient client = OrtolangClient.getInstance();
	
	@Context 
	private SecurityContext ctx;
	
	public ClientResource() {
		logger.log(Level.INFO, "Creating new ClientResource");
		if ( !initialized ) {
			authUrl = OrtolangClientConfig.getInstance().getProperty("diffusion.auth.url");
			authRealm = OrtolangClientConfig.getInstance().getProperty("diffusion.auth.realm");
			appName = OrtolangClientConfig.getInstance().getProperty("client.app.name");
			callbackUrl = OrtolangClientConfig.getInstance().getProperty("client.auth.callback.url");
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
		if ( !client.getAccountManager().exists(user) ) {
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
				client.getAccountManager().setAuthorisationCode(states.get(state), code);
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
