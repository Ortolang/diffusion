package fr.ortolang.diffusion.client.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.iharder.Base64;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import fr.ortolang.diffusion.client.OrtolangClientConfig;

public class OrtolangRestClient {

	private static Logger logger = Logger.getLogger(OrtolangRestClient.class.getName());

	private WebTarget base;
	private Client client;
	private String authorisation;

	public OrtolangRestClient() {
		ResteasyClientBuilder builder = new ResteasyClientBuilder();
		//builder.register(CookieFilter.class);

		StringBuffer url = new StringBuffer();
		if (Boolean.valueOf(OrtolangClientConfig.getInstance().getProperty("api.rest.ssl.enabled"))) {
			logger.log(Level.INFO, "SSL Client config");
			url.append("https://");
			
			if (!Boolean.valueOf(OrtolangClientConfig.getInstance().getProperty("api.rest.ssl.trustmanager.enabled"))) {
				builder.disableTrustManager();
			}
		} else {
			logger.log(Level.INFO, "No-SSL Client config");
			url.append("http://");
		}

		url.append(OrtolangClientConfig.getInstance().getProperty("api.rest.hostname"));
		url.append(":");
		url.append(OrtolangClientConfig.getInstance().getProperty("api.rest.port"));
		url.append(OrtolangClientConfig.getInstance().getProperty("api.rest.url"));

		client = builder.build();
		base = client.target(url.toString());

		logger.log(Level.INFO, "Client created");
	}

	public void close() {
		client.close();
	}

	public void setAutorisationHeader(String authorisation) throws OrtolangRestClientException {
		this.authorisation = authorisation;
		
		if (OrtolangClientConfig.getInstance().getProperty("api.rest.auth.method").equals("oauth")) {
			String url = OrtolangClientConfig.getInstance().getProperty("api.rest.oauth.server.url");
			String realm = OrtolangClientConfig.getInstance().getProperty("api.rest.oauth.realm");
			String appname = OrtolangClientConfig.getInstance().getProperty("api.rest.oauth.app.name");
			String appsecret = OrtolangClientConfig.getInstance().getProperty("api.rest.oauth.app.secret");

			WebTarget target = client.target(url).path("realms").path(realm).path("protocol/openid-connect/grants/access");

			Form form = new Form().param("username", "root").param("password", "tagada54");

			String authorization = Base64.encodeBytes((appname + ":" + appsecret).getBytes());

			Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
			invocationBuilder.header("Authorization", authorization);
			Response response = invocationBuilder.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));

			if (response.getStatus() == Status.OK.getStatusCode()) {
				String tokenResponse = response.readEntity(String.class);
				JsonObject object = Json.createReader(new StringReader(tokenResponse)).readObject();
				this.authorisation = "Bearer " + object.getString("access_token");
			} else {
				this.authorisation = null;
				logger.log(Level.SEVERE, "unexpected response code ("+response.getStatus()+") : "+response.getStatusInfo().getReasonPhrase());
				logger.log(Level.SEVERE, response.readEntity(String.class));
				throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
			}
		}
	}

	public void logout() throws OrtolangRestClientException {
		authorisation = null;
	}

	private Invocation.Builder injectAuthorisation(Invocation.Builder builder) {
		if (authorisation != null) {
			builder.header("Authorization", authorisation);
		}
		return builder;
	}

	public String connectedProfile() throws OrtolangRestClientException {
		WebTarget target = base.path("/profiles/connected");
		Response response = injectAuthorisation(target.request(MediaType.APPLICATION_JSON_TYPE)).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			JsonObject object = Json.createReader(new StringReader(json)).readObject();
			return object.getJsonString("key").getString();
		} else {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public boolean isObjectExists(String key) throws OrtolangRestClientException {
		WebTarget target = base.path("objects").path(key);
		Response response = injectAuthorisation(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).get();
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
		Response response = injectAuthorisation(target.request()).accept(MediaType.APPLICATION_JSON_TYPE).get();
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
		Response response = injectAuthorisation(target.request(MediaType.APPLICATION_FORM_URLENCODED)).accept(MediaType.MEDIA_TYPE_WILDCARD).post(
				Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public JsonObject readWorkspace(String key) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces").path(key);
		Response response = injectAuthorisation(target.request()).accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void writeCollection(String workspace, String path, String description) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces/" + workspace + "/elements");
		MultipartFormDataOutput mdo = new MultipartFormDataOutput();
		mdo.addFormData("path", new ByteArrayInputStream(path.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("type", new ByteArrayInputStream("collection".getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("description", new ByteArrayInputStream(description.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) { };
		Response response = injectAuthorisation(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
		if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void writeDataObject(String workspace, String path, String description, File content, File preview) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces/" + workspace + "/elements");
		MultipartFormDataOutput mdo = new MultipartFormDataOutput();
		mdo.addFormData("path", new ByteArrayInputStream(path.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("type", new ByteArrayInputStream("object".getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("description", new ByteArrayInputStream(description.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		try {
			if (content != null) {
				mdo.addFormData("stream", new FileInputStream(content), MediaType.APPLICATION_OCTET_STREAM_TYPE);
	    	}
			if (preview != null) {
				mdo.addFormData("preview", new FileInputStream(preview), MediaType.APPLICATION_OCTET_STREAM_TYPE);
			}
		} catch ( FileNotFoundException e ) {
			throw new OrtolangRestClientException("unable to read file " + e.getMessage(), e);
		}
		GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) { };
		Response response = injectAuthorisation(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
		if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void writeMetaData(String workspace, String path, String name, String format, File content) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces/" + workspace + "/elements");
		MultipartFormDataOutput mdo = new MultipartFormDataOutput();
		mdo.addFormData("path", new ByteArrayInputStream(path.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("type", new ByteArrayInputStream("metadata".getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("name", new ByteArrayInputStream(name.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("format", new ByteArrayInputStream(format.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		try {
			if (content != null) {
				mdo.addFormData("stream", new FileInputStream(content), MediaType.APPLICATION_OCTET_STREAM_TYPE);
	    	}
    	} catch ( FileNotFoundException e ) {
			throw new OrtolangRestClientException("unable to read file " + e.getMessage(), e);
		}
		GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) { };
		Response response = injectAuthorisation(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
		if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public JsonObject getWorkspaceElement(String workspace, String root, String path) throws OrtolangRestClientException {
		WebTarget target = base.path("workspaces").path(workspace).path("elements");
		Response response = injectAuthorisation(target.queryParam("path", path).queryParam("root", root).request(MediaType.APPLICATION_FORM_URLENCODED)).accept(MediaType.APPLICATION_JSON_TYPE).get();
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
		Response response = injectAuthorisation(target.request(MediaType.APPLICATION_FORM_URLENCODED)).accept(MediaType.MEDIA_TYPE_WILDCARD).post(
				Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

	public String createProcess(String type, String name, Map<String, String> params, Map<String, File> attachments) throws OrtolangRestClientException {
		WebTarget target = base.path("/runtime/processes");
		MultipartFormDataOutput mdo = new MultipartFormDataOutput();
		mdo.addFormData("process-type", new ByteArrayInputStream(type.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("process-name", new ByteArrayInputStream(name.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("name", new ByteArrayInputStream(name.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		for (Entry<String, String> param : params.entrySet()) {
			mdo.addFormData(param.getKey(), new ByteArrayInputStream(param.getValue().getBytes()), MediaType.TEXT_PLAIN_TYPE);
		}
		try {
			for (Entry<String, File> attachment : attachments.entrySet()) {
				mdo.addFormData(attachment.getKey(), new FileInputStream(attachment.getValue()), MediaType.APPLICATION_OCTET_STREAM_TYPE);
			}
		} catch ( FileNotFoundException e ) {
			throw new OrtolangRestClientException("unable to read file " + e.getMessage(), e);
		}
		GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) { };
		Response response = injectAuthorisation(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		} else {
			String path = response.getLocation().getPath();
			return path.substring(path.lastIndexOf("/") + 1);
		}
	}

	public JsonObject getProcess(String key) throws OrtolangRestClientException {
		WebTarget target = base.path("/runtime/processes").path(key);
		Response response = injectAuthorisation(target.request()).accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}
	
	public void submitToolJob(String key, String name, String status) throws OrtolangRestClientException {
		WebTarget target = base.path("/tools/" + key + "/job-new");
		Form form = new Form().param("key", key).param("status", status).param("name", name);
		Response response = injectAuthorisation(target.request(MediaType.APPLICATION_FORM_URLENCODED)).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			throw new OrtolangRestClientException("unexpected response code: " + response.getStatus());
		}
	}

}
