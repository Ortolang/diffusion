package fr.ortolang.diffusion.api.client;

import java.io.File;
import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

import fr.ortolang.diffusion.api.bench.CookieFilter;

public class OrtolangRestClient {

	private static Logger logger = Logger.getLogger(OrtolangRestClient.class.getName());
	
	private String username;
	private String password;
	private String url;
	private Client client;
	private WebTarget base;

	public OrtolangRestClient(String username, String password, String url) {
		this.username = username;
		this.password = password;
		this.url = url;
		init();
	}

	private void init() {
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(username, password);
		client = ClientBuilder.newClient();
		client.register(feature);
		client.register(MultiPartFeature.class);
		client.register(CookieFilter.class);
		base = client.target(url);
	}

	public void checkAuthentication() throws OrtolangRestClientException {
		WebTarget target = base.path("/profiles/connected");
		Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			JsonObject object = Json.createReader(new StringReader(json)).readObject();
			String identifier = object.getJsonString("key").getString();
			if ( !identifier.equals(username) ) {
				throw new OrtolangRestClientException("connected identifier is not the expected one: " + identifier);
			}
		} else {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public boolean objectExists(String key) throws OrtolangRestClientException {
		WebTarget target = base.path("objects").path(key);
		Response response = target.request().accept(MediaType.MEDIA_TYPE_WILDCARD).get();
		if (response.getStatus() == Status.OK.getStatusCode() || response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
			return true;
		} else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
			return false;
		} else {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public JsonObject getObject(String key) throws OrtolangRestClientException {
		WebTarget target = base.path("/objects").path(key);
		Response response = target.request().accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void createWorkspace(String key, String type, String name) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces");
		Form form = new Form().param("key", key).param("type", type).param("name", name);
		Response response = target.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD)
				.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public JsonObject readWorkspace(String key) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces").path(key);
		Response response = target.request().accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	@SuppressWarnings("resource")
	public void writeCollection(String workspace, String path, String description) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces/" + workspace + "/elements");
		MultiPart form = new FormDataMultiPart().field("path", path).field("type", "collection").field("description", description);
		Response response = target.request().accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(form, form.getMediaType()));
		if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	@SuppressWarnings("resource")
	public void writeDataObject(String workspace, String path, String description, File content, File preview) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces/" + workspace + "/elements");
		MultiPart form = new FormDataMultiPart().field("path", path).field("type", "object").field("description", description);
		if (content != null) {
			FileDataBodyPart contentPart = new FileDataBodyPart("stream", content);
			form.bodyPart(contentPart);
		}
		if (preview != null) {
			FileDataBodyPart previewPart = new FileDataBodyPart("preview", preview);
			form.bodyPart(previewPart);
		}
		Response response = target.request().accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(form, form.getMediaType()));
		if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}
	
	@SuppressWarnings("resource")
	public void writeMetaData(String workspace, String path, String name, String format, File content) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces/" + workspace + "/elements");
		MultiPart form = new FormDataMultiPart().field("path", path).field("type", "metadata").field("name", name).field("format", format);
		if (content != null) {
			FileDataBodyPart contentPart = new FileDataBodyPart("stream", content);
			form.bodyPart(contentPart);
		}
		Response response = target.request().accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(form, form.getMediaType()));
		if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}
	
	public JsonObject getWorkspaceElement(String workspace, String root, String path) throws OrtolangRestClientException {
		WebTarget target = base.path("workspaces").path(workspace).path("elements");
		Response response = target.queryParam("path", path).queryParam("root", root).request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}
	
	public void snapshotWorkspace(String workspace, String name) throws OrtolangRestClientException {
		WebTarget target = base.path("workspaces").path(workspace).path("snapshots");
		Form form = new Form().param("snapshotname", name);
		Response response = target.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD)
				.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}
	
	@SuppressWarnings("resource")
	public String createProcess(String type, String name, Map<String, String> params, Map<String, File> attachments) throws OrtolangRestClientException {
		logger.log(Level.INFO, "Creating process on "+url+" with parameters ("+type+","+name+")");
		WebTarget target = base.path("/runtime/processes");
		FormDataMultiPart form = new FormDataMultiPart().field("process-type", type).field("process-name", name);
		for (Entry<String, String> param : params.entrySet()) {
			form.field(param.getKey(), param.getValue());
		}
		if(attachments!=null) {
			for (Entry<String, File> attachment : attachments.entrySet()) {
				FileDataBodyPart contentPart = new FileDataBodyPart(attachment.getKey(), attachment.getValue());
				form.bodyPart(contentPart);
			}
		}
		Response response = target.request().accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(form, form.getMediaType()));
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		} else {
			String path = response.getLocation().getPath(); 
			return path.substring(path.lastIndexOf("/")+1);
		}
	}
	
	public JsonObject getProcess(String key) throws OrtolangRestClientException {
		WebTarget target = base.path("/runtime/processes").path(key);
		Response response = target.request().accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}
	
	public void close() {
		client.close();
	}

}
