package fr.ortolang.diffusion.tool.job.client;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import fr.ortolang.diffusion.tool.job.entity.ToolJob;
import fr.ortolang.diffusion.tool.resource.ToolDescription;

/**
 * Client REST for the tool API
 */
public class ToolJobRestClient {

	private String url;
	private Client client;
	private WebTarget base;

	/**
	 * ToolJob REST Client constructor
	 * @param url URL of the REST API
	 */
	public ToolJobRestClient( String url) {
		this.url = url;      
		init();
	}
	
	/**
	 * Initialize REST client connection
	 */
	private void init() {
		client = ClientBuilder.newClient();
		client.register(MultiPartFeature.class);
		base = client.target(url);
	}

	/**
	 * Get tool description from REST API
	 * @return ToolDescription
	 * @throws ToolJobRestClientException
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public ToolDescription getDescription() throws ToolJobRestClientException, JsonParseException, JsonMappingException, IOException {
		WebTarget target = base.path("/description");
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(json,ToolDescription.class);	
			
		} else {
			throw new ToolJobRestClientException("unexpected response code: " + response.getStatus());
		}
	}
	
	/**
	 * Get execution form of the tool from REST API
	 * @return JsonArray
	 * @throws ToolJobRestClientException
	 */
	public JsonArray getExecutionForm() throws ToolJobRestClientException {
		WebTarget target = base.path("/execution-form");
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			JsonArray object = Json.createReader(new StringReader(json)).readArray();	
			return object;
			
		} else {
			throw new ToolJobRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	/**
	 * Get result form of the tool from REST API
	 * @return JsonArray
	 * @throws ToolJobRestClientException
	 */
	public JsonArray getResultForm() throws ToolJobRestClientException {
		WebTarget target = base.path("/result-form");
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			JsonArray object = Json.createReader(new StringReader(json)).readArray();	
			return object;
			
		} else {
			throw new ToolJobRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	/**
	 * Post execution's jobs of the tool from REST API
	 * @param name	Name of the job
	 * @param priority	Priority of the job
	 * @param Map<String, String> params parameters of the tool
	 * @throws ToolJobRestClientException
	 */
	public void postExecutions(String name, int priority, Map<String, String> params) throws ToolJobRestClientException {
		WebTarget target = base.path("/jobs");
		Form form = new Form();
		for(Entry<String, String> entry : params.entrySet()) {
		    form.param(entry.getKey(), entry.getValue());
		}
		Response response = target.queryParam("name", name).queryParam("priority", priority)
				.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD)
				.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() != Status.OK.getStatusCode()) {
			throw new ToolJobRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	/**
	 * Get execution's jobs of the tool from REST API
	 * @return List<ToolJob>
	 * @throws ToolJobRestClientException
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public List<ToolJob> getExecutions() throws ToolJobRestClientException, JsonParseException, JsonMappingException, IOException {
		WebTarget target = base.path("/jobs");
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(json,new TypeReference<List<ToolJob>>() { });	
			
		} else {
			throw new ToolJobRestClientException("unexpected response code: " + response.getStatus());
		}
	}
	
	/**
	 * Close REST client connection
	 */
	public void close() {
		client.close();
	}

}
