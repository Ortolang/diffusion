package fr.ortolang.diffusion.client.auth;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import net.iharder.Base64;
import fr.ortolang.diffusion.client.OrtolangClientConfig;
import fr.ortolang.diffusion.client.OrtolangClientCookieFilter;

public class AuthenticationManager {

	private static final Logger logger = Logger.getLogger(AuthenticationManager.class.getName());

	private static AuthenticationManager instance;
	private Client client;
	private String authUrl;
	private String authRealm;
	private String appname;
	private String appsecret;
	private Map<String, AuthenticationAccount> accounts = new HashMap<String, AuthenticationAccount> ();
	
	private AuthenticationManager() {
		ResteasyClientBuilder builder = new ResteasyClientBuilder();
		builder.register(OrtolangClientCookieFilter.class);
		client = builder.build();
		
		authUrl = OrtolangClientConfig.getInstance().getProperty("api.rest.auth.server.url");
		authRealm = OrtolangClientConfig.getInstance().getProperty("api.rest.auth.realm");
		appname = OrtolangClientConfig.getInstance().getProperty("api.rest.auth.app.name");
		appsecret = OrtolangClientConfig.getInstance().getProperty("api.rest.auth.app.secret");
		
		//TODO restore accounts from storage...
	}
	
	public static AuthenticationManager getInstance() {
		if (instance == null) {
			instance = new AuthenticationManager();
		}
		return instance;
	}

	public boolean exists(String user) {
		return accounts.containsKey(user);
	}

	public void setAuthorisationCode(String user, String code) throws AuthenticationException {
		logger.log(Level.INFO, "Asking AccessToken using authorisation code for user: " + user);
		
		WebTarget target = client.target(authUrl).path("realms").path(authRealm).path("protocol/openid-connect/access/codes");
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
			logger.log(Level.FINE, "Received grant response: " + object.toString());
			AuthenticationAccount account = new AuthenticationAccount();
			account.setUsername(user);
			account.setIdToken(object.getString("id_token"));
			account.setAccessToken(object.getString("access_token"));
			account.setRefreshToken(object.getString("refresh_token"));
			account.setSessionState(object.getString("session-state"));
			account.setExpires(System.currentTimeMillis() + ((object.getInt("expires_in") - 5) * 1000));
			accounts.put(user, account);
			logger.log(Level.FINE, "AccessToken stored for user: " + user);
		} else {
			logger.log(Level.SEVERE, "unexpected response code ("+response.getStatus()+") : "+response.getStatusInfo().getReasonPhrase());
			logger.log(Level.SEVERE, response.readEntity(String.class));
			throw new AuthenticationException("unexpected authorisation code grant response code: " + response.getStatus());
		}
	}

	public void setCredentials(String user, String password) throws AuthenticationException {
		logger.log(Level.INFO, "Asking AccessToken using credential grant for user: " + user);
			
		WebTarget target = client.target(authUrl).path("realms").path(authRealm).path("protocol/openid-connect/grants/access");
		Form form = new Form().param("username", user).param("password", password);
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
			logger.log(Level.FINE, "Received grant response: " + object.toString());
			AuthenticationAccount account = new AuthenticationAccount();
			account.setUsername(user);
			account.setIdToken(object.getString("id_token"));
			account.setAccessToken(object.getString("access_token"));
			account.setRefreshToken(object.getString("refresh_token"));
			account.setSessionState(object.getString("session-state"));
			account.setExpires(System.currentTimeMillis() + ((object.getInt("expires_in") - 5) * 1000));
			accounts.put(user, account);
			logger.log(Level.FINE, "AccessToken stored for user: " + user);
		} else {
			logger.log(Level.SEVERE, "unexpected response code ("+response.getStatus()+") : "+response.getStatusInfo().getReasonPhrase());
			logger.log(Level.SEVERE, response.readEntity(String.class));
			throw new AuthenticationException("unexpected credential grant response code: " + response.getStatus());
		}
	}

	public void revoke(String user) {
		if ( accounts.containsKey(user) ) {
			logger.log(Level.INFO, "Revoking AccessToken for user: " + user);
			accounts.remove(user);
		}
	}

	public String getHttpAuthorisationHeader(String user) throws AuthenticationException {
		if ( accounts.containsKey(user) ) {
			logger.log(Level.FINE, "Account found for user: " + user);
			AuthenticationAccount account = accounts.get(user);
			if (account.getExpires() < System.currentTimeMillis()) {
				logger.log(Level.FINE, "AccessToken expired for user: " + user);
				this.refresh(user);
				account = accounts.get(user);
			}
			return "Bearer " + account.getAccessToken();
		} else {
			throw new AuthenticationException("unable to find access token for user: " + user);
		}
	}
	
	private void refresh(String user) throws AuthenticationException {
		logger.log(Level.INFO, "Refreshing AccessToken for user: " + user);
		//TODO
	}

}
