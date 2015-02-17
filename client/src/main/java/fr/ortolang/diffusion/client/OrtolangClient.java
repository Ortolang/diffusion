package fr.ortolang.diffusion.client;

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
import javax.naming.AuthenticationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import fr.ortolang.diffusion.client.account.OrtolangClientAccountException;
import fr.ortolang.diffusion.client.account.OrtolangClientAccountManager;

public class OrtolangClient {

	private static Logger logger = Logger.getLogger(OrtolangClient.class.getName());

	private OrtolangClientAccountManager accountManager;
	private WebTarget base;
	private Client client;
	private String currentUser = null;
	
	public OrtolangClient(String id) {
		logger.log(Level.INFO, "Creating new OrtolangClient with id " + id);
		ResteasyClientBuilder builder = new ResteasyClientBuilder();
		builder.register(OrtolangClientCookieFilter.class);
		if (Boolean.valueOf(OrtolangClientConfig.getInstance().getProperty("trustmanager.disabled"))) {
			builder.disableTrustManager();
		}
		client = builder.build();
		
		base = client.target(OrtolangClientConfig.getInstance().getProperty("diffusion.api.url"));
		accountManager = OrtolangClientAccountManager.getInstance(id);
		
		logger.log(Level.INFO, "Client created");
	}
	
	public void close() {
		client.close();
	}

	public void login(String user) throws OrtolangClientException {
		if (accountManager.exists(user)) {
			currentUser = user;
			return;
		}
		throw new OrtolangClientException("user is unknown, use OrtolangClientAccountManager to set user authentication information");
	}

	public void logout() {
		currentUser = null;
	}

	private Invocation.Builder injectAuthHeader(Invocation.Builder builder) throws OrtolangClientException {
		try {
			if (currentUser != null) {
				builder.header("Authorization", accountManager.getHttpAuthorisationHeader(currentUser));
			}
			return builder;
		} catch (OrtolangClientAccountException e) {
			throw new OrtolangClientException("unable to inject authentication header", e);
		}
	}

	public String connectedProfile() throws OrtolangClientException {
		WebTarget target = base.path("/profiles/connected");
		Response response = injectAuthHeader(target.request(MediaType.APPLICATION_JSON_TYPE)).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String json = response.readEntity(String.class);
			JsonObject object = Json.createReader(new StringReader(json)).readObject();
			return object.getJsonString("key").getString();
		} else {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public boolean isObjectExists(String key) throws OrtolangClientException {
		WebTarget target = base.path("objects").path(key);
		Response response = injectAuthHeader(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).get();
		if (response.getStatus() == Status.OK.getStatusCode() || response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
			return true;
		} else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
			return false;
		} else {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public JsonObject getObject(String key) throws OrtolangClientException {
		WebTarget target = base.path("/objects").path(key);
		Response response = injectAuthHeader(target.request()).accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void createWorkspace(String key, String type, String name) throws OrtolangClientException {
		WebTarget target = base.path("/workspaces");
		Form form = new Form().param("key", key).param("type", type).param("name", name);
		Response response = injectAuthHeader(target.request(MediaType.APPLICATION_FORM_URLENCODED)).accept(MediaType.MEDIA_TYPE_WILDCARD).post(
				Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public JsonObject readWorkspace(String key) throws OrtolangClientException, AuthenticationException {
		WebTarget target = base.path("/workspaces").path(key);
		Response response = injectAuthHeader(target.request()).accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void writeCollection(String workspace, String path, String description) throws OrtolangClientException {
		WebTarget target = base.path("/workspaces/" + workspace + "/elements");
		MultipartFormDataOutput mdo = new MultipartFormDataOutput();
		mdo.addFormData("path", new ByteArrayInputStream(path.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("type", new ByteArrayInputStream("collection".getBytes()), MediaType.TEXT_PLAIN_TYPE);
		mdo.addFormData("description", new ByteArrayInputStream(description.getBytes()), MediaType.TEXT_PLAIN_TYPE);
		GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
		};
		Response response = injectAuthHeader(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
		if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void writeDataObject(String workspace, String path, String description, File content, File preview) throws OrtolangClientException {
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
		} catch (FileNotFoundException e) {
			throw new OrtolangClientException("unable to read file " + e.getMessage(), e);
		}
		GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
		};
		Response response = injectAuthHeader(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
		if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void writeMetaData(String workspace, String path, String name, String format, File content) throws OrtolangClientException {
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
		} catch (FileNotFoundException e) {
			throw new OrtolangClientException("unable to read file " + e.getMessage(), e);
		}
		GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
		};
		Response response = injectAuthHeader(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
		if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public JsonObject getWorkspaceElement(String workspace, String root, String path) throws OrtolangClientException {
		WebTarget target = base.path("workspaces").path(workspace).path("elements");
		Response response = injectAuthHeader(target.queryParam("path", path).queryParam("root", root).request(MediaType.APPLICATION_FORM_URLENCODED)).accept(MediaType.APPLICATION_JSON_TYPE)
				.get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void snapshotWorkspace(String workspace, String name) throws OrtolangClientException {
		WebTarget target = base.path("workspaces").path(workspace).path("snapshots");
		Form form = new Form().param("snapshotname", name);
		Response response = injectAuthHeader(target.request(MediaType.APPLICATION_FORM_URLENCODED)).accept(MediaType.MEDIA_TYPE_WILDCARD).post(
				Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() != Status.OK.getStatusCode()) {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public String createProcess(String type, String name, Map<String, String> params, Map<String, File> attachments) throws OrtolangClientException {
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
		} catch (FileNotFoundException e) {
			throw new OrtolangClientException("unable to read file " + e.getMessage(), e);
		}
		GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
		};
		Response response = injectAuthHeader(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		} else {
			String path = response.getLocation().getPath();
			return path.substring(path.lastIndexOf("/") + 1);
		}
	}

	public JsonObject getProcess(String key) throws OrtolangClientException {
		WebTarget target = base.path("/runtime/processes").path(key);
		Response response = injectAuthHeader(target.request()).accept(MediaType.APPLICATION_JSON_TYPE).get();
		if (response.getStatus() == Status.OK.getStatusCode()) {
			String object = response.readEntity(String.class);
			JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
			return jsonObject;
		} else {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

	public void submitToolJob(String key, String name, String status) throws OrtolangClientException {
		WebTarget target = base.path("/tools/" + key + "/job-new");
		Form form = new Form().param("key", key).param("status", status).param("name", name);
		Response response = injectAuthHeader(target.request(MediaType.APPLICATION_FORM_URLENCODED)).accept(MediaType.MEDIA_TYPE_WILDCARD).post(
				Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			throw new OrtolangClientException("unexpected response code: " + response.getStatus());
		}
	}

}