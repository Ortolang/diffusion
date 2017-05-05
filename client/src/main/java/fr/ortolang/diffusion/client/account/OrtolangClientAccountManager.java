package fr.ortolang.diffusion.client.account;

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

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.iharder.Base64;
import fr.ortolang.diffusion.client.OrtolangClientConfig;

public class OrtolangClientAccountManager {

	private static final Logger LOGGER = Logger.getLogger(OrtolangClientAccountManager.class.getName());
	
	private Client client;
	private String authurl;
	private String authrealm;
	private String appname;
	private String appsecret;
	private Map<String, OrtolangClientAccount> accounts = new HashMap<String, OrtolangClientAccount> ();
	
	public OrtolangClientAccountManager(Client client) throws IOException {
		LOGGER.log(Level.INFO, "Creating OrtolangClientAccountManager");
		
		this.authurl = OrtolangClientConfig.getInstance().getProperty("diffusion.auth.url");
		this.authrealm = OrtolangClientConfig.getInstance().getProperty("diffusion.auth.realm");
		this.appname = OrtolangClientConfig.getInstance().getProperty("client.app.name");
		this.appsecret = OrtolangClientConfig.getInstance().getProperty("client.app.secret");
		this.client = client;
	}
	
	public boolean exists(String user) {
		return accounts.containsKey(user);
	}
	
	public void removeExpired(String user) {
		LOGGER.log(Level.FINE, "Removing expired user");
		accounts.remove(user);
	}
	
	public boolean sessionExpired(String user) {
		LOGGER.log(Level.INFO, "Checking validity of session");
		OrtolangClientAccount account = accounts.get(user);
		if (account.getExpires() < System.currentTimeMillis()) {
			LOGGER.log(Level.FINE, "AccessToken expired for user: " + user);
			try {
				this.refresh(account);
				return false;
			} catch (OrtolangClientAccountException e) {
				LOGGER.log(Level.SEVERE, "RefreshToken expired for user: " + user);
				this.removeExpired(user);
				return true;
			}
		}
		return false;
	}

	public synchronized void setUserAccount(String user, String id_token, String access_token, String refresh_token, String session_state, long expiration) {
		OrtolangClientAccount account = new OrtolangClientAccount();
		account.setUsername(user);
		account.setIdToken(id_token);
		account.setAccessToken(access_token);
		account.setRefreshToken(refresh_token);
		account.setSessionState(session_state);
		account.setExpires(expiration);
		accounts.put(user, account);
		LOGGER.log(Level.FINE, "AccessToken stored for user: " + user);
	}
	
	public synchronized void setAuthorisationCode(String user, String code) throws OrtolangClientAccountException {
		LOGGER.log(Level.INFO, "Asking AccessToken using authorisation code for user: " + user);
		
		WebTarget target = client.target(authurl).path("realms").path(authrealm).path("protocol/openid-connect/access/codes");
		Form form = new Form().param("code", code);
		Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
		if ( appsecret != null && appsecret.length() > 0 ) {
			String authz = Base64.encodeBytes((appname + ":" + appsecret).getBytes());
			invocationBuilder.header("Authorization", authz);
		} else {
			form.param("client_id", appname);
		}
		Response response = invocationBuilder.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String tokenResponse = response.readEntity(String.class);
			JsonObject object = Json.createReader(new StringReader(tokenResponse)).readObject();
			response.close();
			LOGGER.log(Level.FINE, "Received grant response: " + object.toString());
			OrtolangClientAccount account = new OrtolangClientAccount();
			account.setUsername(user);
			account.setIdToken(object.getString("id_token"));
			account.setAccessToken(object.getString("access_token"));
			account.setRefreshToken(object.getString("refresh_token"));
			account.setSessionState(object.getString("session-state"));
			account.setExpires(System.currentTimeMillis() + ((object.getInt("expires_in") - 5) * 1000));
			accounts.put(user, account);
			LOGGER.log(Level.FINE, "AccessToken stored for user: " + user);
		} else {
			LOGGER.log(Level.SEVERE, "unexpected response code ("+response.getStatus()+") : "+response.getStatusInfo().getReasonPhrase());
			LOGGER.log(Level.SEVERE, response.readEntity(String.class));
			throw new OrtolangClientAccountException("unexpected authorisation code grant response code: " + response.getStatus());
		}
	}

	public synchronized void setCredentials(String user, String password) throws OrtolangClientAccountException {
		LOGGER.log(Level.INFO, "Asking AccessToken using credential grant for user: " + user);

		WebTarget target = client.target(authurl).path("realms").path(authrealm).path("protocol/openid-connect/token");
		Form form = new Form().param("username", user).param("password", password).param("grant_type", "password");
		Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
		if ( appsecret != null && appsecret.length() > 0 ) {
			String authz = Base64.encodeBytes((appname + ":" + appsecret).getBytes());
			invocationBuilder.header("Authorization", authz);
		} else {
			form.param("client_id", appname);
		}
		Response response = invocationBuilder.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String tokenResponse = response.readEntity(String.class);
			response.close();
			JsonObject object = Json.createReader(new StringReader(tokenResponse)).readObject();
			LOGGER.log(Level.FINE, "Received grant response: " + object.toString());
			OrtolangClientAccount account = new OrtolangClientAccount();
			account.setUsername(user);
			account.setIdToken(object.getString("id_token"));
			account.setAccessToken(object.getString("access_token"));
			account.setRefreshToken(object.getString("refresh_token"));
			if (object.containsKey("session-state")) {
				account.setSessionState(object.getString("session-state"));
			}
			account.setExpires(System.currentTimeMillis() + ((object.getInt("expires_in") - 5) * 1000));
			accounts.put(user, account);
			LOGGER.log(Level.FINE, "AccessToken stored for user: " + user);
		} else {
			LOGGER.log(Level.SEVERE, "unexpected response code ("+response.getStatus()+") : "+response.getStatusInfo().getReasonPhrase());
			LOGGER.log(Level.SEVERE, response.readEntity(String.class));
			throw new OrtolangClientAccountException("unexpected credential grant response code: " + response.getStatus());
		}
	}

	public void revoke(String user) {
		if ( accounts.containsKey(user) ) {
			LOGGER.log(Level.INFO, "Revoking AccessToken for user: " + user);
			accounts.remove(user);
		}
	}

	public String getHttpAuthorisationHeader(String user) throws OrtolangClientAccountException {
		if ( accounts.containsKey(user) ) {
			LOGGER.log(Level.FINE, "Account found for user: " + user);
			OrtolangClientAccount account = accounts.get(user);
			if (account.getExpires() < System.currentTimeMillis()) {
				LOGGER.log(Level.FINE, "AccessToken expired for user: " + user);
				this.refresh(account);
				account = accounts.get(user);
			}
			return "Bearer " + account.getAccessToken();
		} else {
			throw new OrtolangClientAccountException("unable to find access token for user: " + user);
		}
	}
	
	private synchronized void refresh(OrtolangClientAccount account) throws OrtolangClientAccountException {
		LOGGER.log(Level.INFO, "Refreshing AccessToken for user: " + account.getUsername());
		
		WebTarget target = client.target(authurl).path("realms").path(authrealm).path("protocol/openid-connect/refresh");
		Form form = new Form().param("refresh_token", account.getRefreshToken());
		LOGGER.log(Level.INFO, account.getRefreshToken() + " - expire_in : " + account.getExpires());
		Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
		if ( appsecret != null && appsecret.length() > 0 ) {
			String authz = Base64.encodeBytes((appname + ":" + appsecret).getBytes());
			invocationBuilder.header("Authorization", authz);
			LOGGER.log(Level.INFO, authz);
		} else {
			form.param("client_id", appname);
			LOGGER.log(Level.INFO, appname);
		}
		Response response = invocationBuilder.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String tokenResponse = response.readEntity(String.class);
			response.close();
			JsonObject object = Json.createReader(new StringReader(tokenResponse)).readObject();
			LOGGER.log(Level.FINE, "Received refresh response: " + object.toString());
			account.setIdToken(object.getString("id_token"));
			account.setAccessToken(object.getString("access_token"));
			account.setRefreshToken(object.getString("refresh_token"));
			account.setSessionState(object.getString("session-state"));
			account.setExpires(System.currentTimeMillis() + ((object.getInt("expires_in") - 5) * 1000));
			LOGGER.log(Level.FINE, "AccessToken refreshed for user: " + account.getUsername());
		} else {
			LOGGER.log(Level.SEVERE, "unexpected response code ("+response.getStatus()+") : "+response.getStatusInfo().getReasonPhrase());
			LOGGER.log(Level.SEVERE, response.readEntity(String.class));
			throw new OrtolangClientAccountException("unexpected credential grant response code: " + response.getStatus());
		}
	}

}
