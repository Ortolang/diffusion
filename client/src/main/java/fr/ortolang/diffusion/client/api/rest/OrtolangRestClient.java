package fr.ortolang.diffusion.client.api.rest;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import net.iharder.Base64;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.keycloak.util.BasicAuthHelper;

import fr.ortolang.diffusion.client.OrtolangClientConfig;

public class OrtolangRestClient {

	private static Logger logger = Logger.getLogger(OrtolangRestClient.class.getName());

	private WebTarget base;
	private Client client;
	private String authorisation;
	private Map<String, String> authCache = new HashMap<String, String>();

	public OrtolangRestClient() {
		ClientBuilder builder = ClientBuilder.newBuilder();

		ClientConfig clientConfig = new ClientConfig();
		clientConfig.register(MultiPartFeature.class);
		clientConfig.register(CookieFilter.class);

		StringBuffer url = new StringBuffer();
		if (Boolean.valueOf(OrtolangClientConfig.getInstance().getProperty("api.rest.ssl.enabled"))) {
			logger.log(Level.INFO, "SSL Client config");
			SslConfigurator sslConfig = SslConfigurator.newInstance();
			try {
				URI uri = OrtolangRestClient.class.getClassLoader().getResource("cacerts.ts").toURI();
				
				logger.log(Level.INFO, "Uri to the certificate : "+uri.toString());
				Path trustStore = null;
				if(uri.toString().startsWith("file:")) {
					trustStore = Paths.get(uri);
				} else {
					final Map<String, String> env = new HashMap<>();
					final String[] array = uri.toString().split("!");
					final FileSystem fs = FileSystems.newFileSystem(URI.create(array[0]), env);
					trustStore = fs.getPath(array[1]);
				}
				sslConfig.trustStoreBytes(Files.readAllBytes(trustStore));
				sslConfig.trustStorePassword("tagada");
				SSLContext sslContext = sslConfig.createSSLContext();
				builder.sslContext(sslContext);
				url.append("https://");
			} catch (URISyntaxException | IOException e) {
				logger.log(Level.WARNING, "Unable to load SSL config, falling back to No-SSL config !!", e);
				url.append("http://");
			}
		} else {
			logger.log(Level.INFO, "No-SSL Client config");
			url.append("http://");
		}

		url.append(OrtolangClientConfig.getInstance().getProperty("api.rest.hostname"));
		url.append(":");
		url.append(OrtolangClientConfig.getInstance().getProperty("api.rest.port"));
		url.append(OrtolangClientConfig.getInstance().getProperty("api.rest.url"));

		client = builder.withConfig(clientConfig).build();
		base = client.target(url.toString());

		logger.log(Level.INFO, "Client created");
	}

	public void close() {
		client.close();
	}

	public void login(String username, String password) throws OrtolangRestClientException {
		if (authCache.containsKey(username)) {
			authorisation = authCache.get(username);
		}

		if (OrtolangClientConfig.getInstance().getProperty("api.rest.auth.method").equals("basic")) {
			String credentials = username + ":" + password;
			authorisation = "Basic " + Base64.encodeBytes(credentials.getBytes());
		}

		if (OrtolangClientConfig.getInstance().getProperty("api.rest.auth.method").equals("oauth")) {
			String url = OrtolangClientConfig.getInstance().getProperty("api.rest.oauth.server.url");
			String realm = OrtolangClientConfig.getInstance().getProperty("api.rest.oauth.realm");
			String appname = OrtolangClientConfig.getInstance().getProperty("api.rest.oauth.app.name");
			String appsecret = OrtolangClientConfig.getInstance().getProperty("api.rest.oauth.app.secret");

			WebTarget target = client.target(url).path("realms").path(realm).path("protocol/openid-connect/grants/access");

			Form form = new Form().param("username", username).param("password", password);

			String authorization = BasicAuthHelper.createHeader(appname, appsecret);

			Invocation.Builder invocationBuilder = target.request(MediaType.APPLICATION_JSON_TYPE);
			invocationBuilder.header("Authorization", authorization);
			Response response = invocationBuilder.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));

			if (response.getStatus() == Status.OK.getStatusCode()) {
				String tokenResponse = response.readEntity(String.class);
				JsonObject object = Json.createReader(new StringReader(tokenResponse)).readObject();
				authorisation = "Bearer " + object.getString("access_token");
			} else {
				authorisation = null;
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

	@SuppressWarnings("resource")
	public void writeCollection(String workspace, String path, String description) throws OrtolangRestClientException {
		WebTarget target = base.path("/workspaces/" + workspace + "/elements");
		MultiPart form = new FormDataMultiPart().field("path", path).field("type", "collection").field("description", description);
		Response response = injectAuthorisation(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(form, form.getMediaType()));
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
		Response response = injectAuthorisation(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(form, form.getMediaType()));
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
		Response response = injectAuthorisation(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(form, form.getMediaType()));
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

	@SuppressWarnings("resource")
	public String createProcess(String type, String name, Map<String, String> params, Map<String, File> attachments) throws OrtolangRestClientException {
		WebTarget target = base.path("/runtime/processes");
		FormDataMultiPart form = new FormDataMultiPart().field("process-type", type).field("process-name", name);
		for (Entry<String, String> param : params.entrySet()) {
			form.field(param.getKey(), param.getValue());
		}
		for (Entry<String, File> attachment : attachments.entrySet()) {
			FileDataBodyPart contentPart = new FileDataBodyPart(attachment.getKey(), attachment.getValue());
			form.bodyPart(contentPart);
		}
		Response response = injectAuthorisation(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(form, form.getMediaType()));
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
