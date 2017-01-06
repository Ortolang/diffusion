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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
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

    private static final Logger LOGGER = Logger.getLogger(OrtolangClient.class.getName());

    private OrtolangClientAccountManager accountManager;
    private WebTarget base;
    private Client client;
    private String currentUser = null;
    private String authorization = null;

    private OrtolangClient() throws IOException {
        LOGGER.log(Level.INFO, "Creating new OrtolangClient");
        ResteasyClientBuilder builder = new ResteasyClientBuilder();
        builder.register(OrtolangClientCookieFilter.class);
        if (Boolean.valueOf(OrtolangClientConfig.getInstance().getProperty("trustmanager.disabled"))) {
            builder.disableTrustManager();
        }
        client = builder.build();

        base = client.target(OrtolangClientConfig.getInstance().getProperty("diffusion.api.url"));
        accountManager = new OrtolangClientAccountManager(client);

        LOGGER.log(Level.INFO, "Client created");
    }

    public static OrtolangClient getInstance() {
        return OrtolangClientHolder.INSTANCE;
    }

    private static class OrtolangClientHolder {
        static final OrtolangClient INSTANCE;
        static {
            OrtolangClient ortolangClient;
            try {
                ortolangClient = new OrtolangClient();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Cannot instantiate OrtolangClient", e);
                ortolangClient = null;
            }
            INSTANCE = ortolangClient;
        }

        private OrtolangClientHolder() {
        }
    }

    public OrtolangClientAccountManager getAccountManager() {
        return accountManager;
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

    private synchronized void updateAuthorization() throws OrtolangClientAccountException {
        if (currentUser != null) {
            authorization = accountManager.getHttpAuthorisationHeader(currentUser);
        }
    }

    private Invocation.Builder injectAuthHeader(Invocation.Builder builder) {
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return builder;
    }

    public synchronized String connectedProfile() throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/profiles/connected");
        Response response = injectAuthHeader(target.request(MediaType.APPLICATION_JSON_TYPE)).get();
        if (response.getStatus() == Status.OK.getStatusCode()) {
            String json = response.readEntity(String.class);
            JsonObject object = Json.createReader(new StringReader(json)).readObject();
            response.close();
            return object.getJsonString("key").getString();
        } else {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
    }

    public synchronized JsonObject listObjects(String service, String type, String status, int offset, int limit) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/objects");
        Builder request = target.queryParam("service", service).queryParam("type", type).queryParam("status", status)
                .queryParam("offset", offset).queryParam("limit", limit).request();
        Response response = injectAuthHeader(request).accept(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() == Status.OK.getStatusCode()) {
            String object = response.readEntity(String.class);
            JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
            response.close();
            return jsonObject;
        } else {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
    }

    public synchronized boolean isObjectExists(String key) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("objects").path(key);
        Response response = injectAuthHeader(target.request()).accept(MediaType.MEDIA_TYPE_WILDCARD).get();
        if (response.getStatus() == Status.OK.getStatusCode() || response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
            response.close();
            return true;
        } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            response.close();
            return false;
        } else {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
    }

    public synchronized JsonObject getObject(String key) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/objects").path(key);
        Response response = injectAuthHeader(target.request()).accept(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() == Status.OK.getStatusCode()) {
            String object = response.readEntity(String.class);
            JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
            response.close();
            return jsonObject;
        } else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            response.close();
            return null;
        } else {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
    }

    public synchronized void reindex(String key) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/objects").path(key).path("/index");
        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity("", "text/plain"));
        if (response.getStatus() != Status.OK.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
        response.close();
    }

    public synchronized Path downloadObject(String key) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("content").path("key").path(key).queryParam("fd", "true");
        Response response = injectAuthHeader(target.request()).accept(MediaType.WILDCARD_TYPE).get();
        if (response.getStatus() == Status.OK.getStatusCode()) {
            try {
                Path temp = Files.createTempFile("ortolang-client", ".tmp");
                try (InputStream is = response.readEntity(InputStream.class); OutputStream os = Files.newOutputStream(temp)) {
                    byte[] buffer = new byte[1024];
                    int nbreads;
                    while ( (nbreads = is.read(buffer)) > -1 ) {
                        os.write(buffer, 0, nbreads);
                    }
                    return temp;
                } finally {
                    response.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "unable to download file for key: " + key, e);
                throw new OrtolangClientException("unable to download file for key: " + key, e);
            }
        } else {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
    }

    public synchronized void createWorkspace(String key, String type, String name) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/workspaces");
        Form form = new Form().param("key", key).param("type", type).param("name", name);
        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
        if (response.getStatus() != Status.CREATED.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
    }

    public synchronized JsonObject readWorkspace(String key) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/workspaces").path(key);
        Response response = injectAuthHeader(target.request()).accept(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() == Status.OK.getStatusCode()) {
            String object = response.readEntity(String.class);
            JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
            response.close();
            return jsonObject;
        } else {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
    }

    public synchronized void writeCollection(String workspace, String path, String description) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/workspaces/" + workspace + "/elements");
        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData("path", new ByteArrayInputStream(path.getBytes()), MediaType.TEXT_PLAIN_TYPE);
        mdo.addFormData("type", new ByteArrayInputStream("collection".getBytes()), MediaType.TEXT_PLAIN_TYPE);
        mdo.addFormData("description", new ByteArrayInputStream(description.getBytes()), MediaType.TEXT_PLAIN_TYPE);
        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
        };
        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
        if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
        response.close();
    }

    /**
     * Uploads a file to the server.
     * @param workspace workspace key
     * @param path path of the destination
     * @param description a description
     * @param content the file to upload
     * @param preview the preview of the file
     * @throws OrtolangClientException
     * @throws OrtolangClientAccountException
     */
    public synchronized void writeDataObject(String workspace, String path, String description, File content, File preview) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
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
        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
        if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
        response.close();
    }

    public synchronized void writeMetaData(String workspace, String path, String name, String format, File content) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/workspaces/" + workspace + "/elements");
        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData("path", new ByteArrayInputStream(path.getBytes()), MediaType.TEXT_PLAIN_TYPE);
        mdo.addFormData("type", new ByteArrayInputStream("metadata".getBytes()), MediaType.TEXT_PLAIN_TYPE);
        mdo.addFormData("name", new ByteArrayInputStream(name.getBytes()), MediaType.TEXT_PLAIN_TYPE);
        if (format != null) {        	
        	mdo.addFormData("format", new ByteArrayInputStream(format.getBytes()), MediaType.TEXT_PLAIN_TYPE);
        }
        try {
            if (content != null) {
                mdo.addFormData("stream", new FileInputStream(content), MediaType.APPLICATION_OCTET_STREAM_TYPE);
            }
        } catch (FileNotFoundException e) {
            throw new OrtolangClientException("unable to read file " + e.getMessage(), e);
        }
        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
        };
        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
        if (response.getStatus() != Status.CREATED.getStatusCode() && response.getStatus() != Status.OK.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
        response.close();
    }

    public synchronized JsonObject getWorkspaceElement(String workspace, String root, String path) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("workspaces").path(workspace).path("elements");
        Response response = injectAuthHeader(target.queryParam("path", path).queryParam("root", root).request(MediaType.APPLICATION_JSON_TYPE)).get();
        if (response.getStatus() == Status.OK.getStatusCode()) {
            String object = response.readEntity(String.class);
            JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
            response.close();
            return jsonObject;
        } else {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
    }

    public synchronized void snapshotWorkspace(String workspace) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("workspaces").path(workspace).path("snapshots");
        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity(null, "text/plain"));
        if (response.getStatus() != Status.OK.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
        response.close();
    }

    public synchronized String createRemoteProcess(String toolId, String name, String toolKey) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/runtime/remote-processes");
        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        mdo.addFormData("tool-jobid", new ByteArrayInputStream(toolId.getBytes()), MediaType.TEXT_PLAIN_TYPE);
        mdo.addFormData("tool-key", new ByteArrayInputStream(toolKey.getBytes()), MediaType.TEXT_PLAIN_TYPE);
        mdo.addFormData("tool-name", new ByteArrayInputStream(name.getBytes()), MediaType.TEXT_PLAIN_TYPE);
        mdo.addFormData("name", new ByteArrayInputStream(name.getBytes()), MediaType.TEXT_PLAIN_TYPE);

        GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
        };
        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA));
        if (response.getStatus() != Status.CREATED.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        } else {
            String path = response.getLocation().getPath();
            response.close();
            return path.substring(path.lastIndexOf("/") + 1);
        }
    }

    public synchronized void updateRemoteProcess(String pid, String state, String log, long start, long stop, String activity) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/runtime/remote-processes/").path(pid);
        Form form = new Form();
        if(state != null){
            form.param("status", state);
        }
        if(log != null){
            form.param("log", log);
        }
        if(start != 0){
            form.param("start", Long.toString(start));
        }
        if(stop != 0){
            form.param("stop", Long.toString(stop));
        }
        if(activity != null){
            form.param("activity", activity);
        }

        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
        if (response.getStatus() != Status.OK.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        } else {
            response.close();
        }
    }


    public synchronized String createProcess(String type, String name, Map<String, String> params, Map<String, File> attachments) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
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
        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
        if (response.getStatus() != Status.CREATED.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        } else {
            String path = response.getLocation().getPath();
            response.close();
            return path.substring(path.lastIndexOf("/") + 1);
        }
    }

    public synchronized JsonObject getProcess(String key) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/runtime/processes").path(key);
        Response response = injectAuthHeader(target.request(MediaType.APPLICATION_JSON_TYPE)).get();
        if (response.getStatus() == Status.OK.getStatusCode()) {
            String object = response.readEntity(String.class);
            JsonObject jsonObject = Json.createReader(new StringReader(object)).readObject();
            response.close();
            return jsonObject;
        } else {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
    }

    public synchronized void submitToolJob(String key, String name, String status) throws OrtolangClientException, OrtolangClientAccountException {
        updateAuthorization();
        WebTarget target = base.path("/tools/" + key + "/job-new");
        Form form = new Form().param("key", key).param("status", status).param("name", name);
        Response response = injectAuthHeader(target.request(MediaType.MEDIA_TYPE_WILDCARD)).post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED));
        if (response.getStatus() != Status.CREATED.getStatusCode()) {
            response.close();
            throw new OrtolangClientException("unexpected response code: " + response.getStatus());
        }
        response.close();
    }

}
