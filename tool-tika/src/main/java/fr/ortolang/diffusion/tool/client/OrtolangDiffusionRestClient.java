package fr.ortolang.diffusion.tool.client;

import java.io.InputStream;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.ortolang.diffusion.tool.job.ToolJobException;

/**
 * Client REST for the tool API
 */
public class OrtolangDiffusionRestClient {

	private String url;
	private Client client;
	private WebTarget base;
	private final String username;
	private final String password;

	/**
	 * ToolJob REST Client constructor
	 * @param url URL of the REST API
     * @param user
     * @param password
	 */
	public OrtolangDiffusionRestClient( String url, String username, String password) {
		this.url = url;  
		this.username = username;
		this.password = password;
	}
	
	/**
	 * Initialize REST client connection
	 */
	private void init() {
		client = ClientBuilder.newClient();
	    client.register(new AuthHeadersRequestFilter(username, password));
		base = client.target(url);
	}

	/**
	 * Checks if a dataobject exists on ortolang diffusion
	 * @param key of the dataobject
	 * @return Boolean
	 * @throws ToolJobException
	 */
	public boolean objectExists(String key) throws ToolJobException {
		try{   
			init();
			WebTarget target = base.path("/objects").path(key);
			Response response = target.request()
					.accept(MediaType.MEDIA_TYPE_WILDCARD)
                    .get();
			if (response.getStatus() == Status.OK.getStatusCode() || response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
				return true;
			} else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
				return false;
			} else {
				throw new ToolJobException("unexpected response code: " + response.getStatus());
			}
		}
		 finally {
			client.close();
		}
	}

	/**
	 * Get a dataobject
	 * @param key of the dataobject
	 * @return JsonObject
	 * @throws ToolJobException
	 */
	public JsonObject getObject(String key) throws ToolJobException {
		try{   
			init();
			WebTarget target = base.path("/objects").path(key);
			Response response = target.request()
					.accept(MediaType.APPLICATION_JSON_TYPE)
                    .get();
			if (response.getStatus() == Status.OK.getStatusCode()) {
				String object = response.readEntity(String.class);
				JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
				return jsonObject;
			} else {
				throw new ToolJobException("unexpected response code: " + response.getStatus());
			}
		}
		 finally {
			client.close();
		}
	}
	
	/**
	 * Download a dataobject from ortolang diffusion
	 * @param key of the dataobject
	 * @return InputStream of the dataobject
	 * @throws ToolJobException
	 */
	public InputStream downloadObject(String key) throws ToolJobException{
		try{   
			init();
			WebTarget target = base.path("/objects").path(key).path("/download");
	        Response response = target.request().get();     
	        if (response.getStatus() == Status.OK.getStatusCode()) {
				InputStream input =  response.readEntity(InputStream.class);
				close();
				return input;
	        } else {
	        	throw new ToolJobException("unexpected response code: " + response.getStatus());
	        }
		}
		 finally {
			client.close();
		}
	}
	
	
	/**
	 * Close REST client connection
	 */
	public void close() {
		client.close();
	}

}
