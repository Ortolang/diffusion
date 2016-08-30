package fr.ortolang.diffusion.api.message;

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
 * Copyright (C) 2013 - 2016 Ortolang Team
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.google.common.io.Files;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.message.MessageService;
import fr.ortolang.diffusion.message.MessageServiceException;
import fr.ortolang.diffusion.message.entity.Message;
import fr.ortolang.diffusion.message.entity.MessageAttachment;
import fr.ortolang.diffusion.message.entity.Thread;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Path("/threads")
@Produces({ MediaType.APPLICATION_JSON })
public class MessageResource {

    private static final Logger LOGGER = Logger.getLogger(MessageResource.class.getName());

    @EJB
    private BrowserService browser;
    @EJB
    private MessageService service;
    @Context
    private UriInfo uriInfo;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createThread(@FormParam("wskey") String wskey, @FormParam("title") String title, @FormParam("body") String body) throws MessageServiceException, KeyNotFoundException,
            AccessDeniedException, KeyAlreadyExistsException {
        LOGGER.log(Level.INFO, "POST /threads");
        String key = UUID.randomUUID().toString();
        service.createThread(key, wskey, title, body, true);
        URI location = uriInfo.getBaseUriBuilder().path(this.getClass()).path(key).build();
        return Response.created(location).build();
    }

    @GET
    @GZIP
    public Response listThreads(@QueryParam(value = "wskey") String wskey) throws KeyNotFoundException, AccessDeniedException, MessageServiceException, BrowserServiceException, URISyntaxException {
        LOGGER.log(Level.INFO, "GET /threads?wskey=" + wskey);
        List<String> wsthreads = service.findThreadsForWorkspace(wskey);
        List<ThreadRepresentation> threads = new ArrayList<ThreadRepresentation> ();
        for (String tkey: wsthreads) {
            OrtolangObjectInfos infos = browser.getInfos(tkey);
            threads.add(ThreadRepresentation.fromThreadAndInfos(service.readThread(tkey), infos));
        }
        return Response.ok(threads).build();
    }

    @GET
    @Path("/{key}")
    @GZIP
    public Response getThread(@PathParam(value = "key") String key) throws KeyNotFoundException, AccessDeniedException, MessageServiceException, BrowserServiceException {
        LOGGER.log(Level.INFO, "GET /threads/" + key);
        Thread thread = service.readThread(key);
        OrtolangObjectInfos infos = browser.getInfos(key);
        ThreadRepresentation representation = ThreadRepresentation.fromThreadAndInfos(thread, infos);
        return Response.ok(representation).build();
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateThread(@PathParam(value = "key") String key, ThreadRepresentation representation) throws MessageServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "PUT /threads/" + key);
        service.updateThread(key, representation.getTitle());
        Thread thread = service.readThread(key);
        ThreadRepresentation newrepresentation = ThreadRepresentation.fromThread(thread);
        return Response.ok(newrepresentation).build();
    }

    @DELETE
    @Path("/{key}")
    public Response deleteThread(@PathParam(value = "key") String key) throws MessageServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "DELETE /threads/" + key);
        service.deleteThread(key);
        return Response.ok().build();
    }

    @GET
    @Path("/{key}/messages")
    @GZIP
    public Response browseThread(@PathParam(value = "key") String key) throws KeyNotFoundException, MessageServiceException, OrtolangException, BrowserServiceException, URISyntaxException {
        LOGGER.log(Level.INFO, "GET /threads/" + key + "/messages");
        Thread thread = service.readThread(key);
        List<Message> msgs = service.browseThread(key);
        List<MessageRepresentation> msgsrep = new ArrayList<MessageRepresentation>();
        for (Message message : msgs) {
            OrtolangObjectInfos infos = browser.getInfos(message.getKey());
            MessageRepresentation msgrep = MessageRepresentation.fromMessageAndInfos(message, infos);
            if (thread.getQuestion().equals(message.getKey())) {
                msgrep.setQuestion(true);
            }
            if (thread.getAnswer() != null && thread.getAnswer().equals(message.getKey())) {
                msgrep.setAnswer(true);
            }
            msgsrep.add(msgrep);
        }
        return Response.ok(msgsrep).build();
    }

    @POST
    @Path("/{key}/messages")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response postMessage(@PathParam(value = "key") String key, MultipartFormDataInput input) throws AccessDeniedException, MessageServiceException, KeyNotFoundException, IOException {
        LOGGER.log(Level.INFO, "POST /threads/" + key + "/messages");
        Map<String, List<InputPart>> form = input.getFormDataMap();
        String mkey = UUID.randomUUID().toString();
        String parent = null;
        String body = null;
        Map<String, InputStream> attachments = new HashMap<>();
        for (Map.Entry<String, List<InputPart>> entry : form.entrySet()) {
            InputPart inputPart = entry.getValue().get(0);
            switch (entry.getKey()) {
            case "parent":
                parent = inputPart.getBody(String.class, null);
                break;
            case "body":
                body = inputPart.getBody(String.class, null);
                break;
            default:
                InputStream inputStream = inputPart.getBody(InputStream.class, null);
                attachments.put(getFileName(inputPart.getHeaders()), inputStream);
            }
        }
        service.postMessage(key, mkey, parent, body, attachments);
        URI location = uriInfo.getBaseUriBuilder().path(this.getClass()).path(key).build();
        return Response.created(location).build();
    }

    @GET
    @Path("/{key}/messages/{mkey}")
    @GZIP
    public Response readMessage(@PathParam(value = "key") String key, @PathParam(value = "mkey") String mkey) throws AccessDeniedException, MessageServiceException, KeyNotFoundException,
            BrowserServiceException {
        LOGGER.log(Level.INFO, "GET /threads/" + key + "/messages/" + mkey);
        Message msg = service.readMessage(mkey);
        OrtolangObjectInfos infos = browser.getInfos(msg.getKey());
        return Response.ok(MessageRepresentation.fromMessageAndInfos(msg, infos)).build();
    }

    @PUT
    @Path("/{key}/messages/{mkey}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateMessage(@PathParam(value = "key") String key, @PathParam(value = "mkey") String mkey, MultipartFormDataInput input)
            throws AccessDeniedException, MessageServiceException, KeyNotFoundException, IOException, DataCollisionException {
        LOGGER.log(Level.INFO, "PUT /threads/" + key + "/messages/" + mkey);
        Map<String, List<InputPart>> form = input.getFormDataMap();
        String body = null;
        String[] removedAttachments = null;
        Map<String, InputStream> attachments = new HashMap<>();
        for (Map.Entry<String, List<InputPart>> entry : form.entrySet()) {
            InputPart inputPart = entry.getValue().get(0);
            switch (entry.getKey()) {
            case "body":
                body = inputPart.getBody(String.class, null);
                break;
            case "removed-attachments":
                removedAttachments = inputPart.getBody(String.class, null).split(",");
                break;
            default:
                InputStream inputStream = inputPart.getBody(InputStream.class, null);
                attachments.put(getFileName(inputPart.getHeaders()), inputStream);
            }
        }
        service.updateMessage(mkey, body, attachments, removedAttachments);
        Message msg = service.readMessage(mkey);
        return Response.ok(MessageRepresentation.fromMessage(msg)).build();
    }

    @DELETE
    @Path("/{key}/messages/{mkey}")
    public Response deleteMessage(@PathParam(value = "key") String key, @PathParam(value = "mkey") String mkey) throws AccessDeniedException, MessageServiceException,
            KeyNotFoundException {
        LOGGER.log(Level.INFO, "DELETE /threads/" + key + "/messages/" + mkey);
        service.deleteMessage(mkey);
        return Response.ok().build();
    }

    @GET
    @Path("/{key}/messages/{mkey}/attachments")
    public Response listAttachments(@PathParam(value = "key") String key, @PathParam(value = "mkey") String mkey) throws AccessDeniedException, MessageServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /threads/" + key + "/messages/" + mkey);
        Message msg = service.readMessage(mkey);
        return Response.ok(msg.getAttachments()).build();
    }

    @POST
    @Path("/{key}/messages/{mkey}/attachments")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addAttachments(@PathParam(value = "key") String key, @PathParam(value = "mkey") String mkey, MultipartFormDataInput input) throws IOException, AccessDeniedException,
            MessageServiceException, KeyNotFoundException, DataCollisionException {
        LOGGER.log(Level.INFO, "POST(multipart/form-data) /threads/" + key + "/messages/" + mkey + "/attachments");
        for (InputPart part : input.getParts()) {
            if (part.getHeaders().containsKey("Content-Disposition") && part.getHeaders().getFirst("Content-Disposition").contains("filename=")) {
                String name = "unknown";
                String[] contentDisposition = part.getHeaders().getFirst("Content-Disposition").split(";");
                for (String filename : contentDisposition) {
                    if (filename.trim().startsWith("filename")) {
                        String[] names = filename.split("=");
                        name = names[1].trim().replaceAll("\"", "");
                    }
                }
                InputStream data = part.getBody(InputStream.class, null);
                service.addMessageAttachment(mkey, name, data);
            }
        }
        return Response.ok().build();
    }

    @GET
    @Path("/{key}/messages/{mkey}/attachments/{name}")
    public Response downloadAttachment(@PathParam(value = "key") String key, @PathParam(value = "mkey") String mkey, @PathParam(value = "name") String name) throws AccessDeniedException,
            MessageServiceException, KeyNotFoundException, DataNotFoundException, UnsupportedEncodingException {
        LOGGER.log(Level.INFO, "GET /threads/" + key + "/messages/" + mkey + "/attachments/" + name);
        Message msg = service.readMessage(mkey);
        MessageAttachment attachment = msg.findAttachmentByName(name);
        ResponseBuilder builder = Response.ok();
        builder.header("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(attachment.getName(), "utf-8"));
        builder.header("Content-Lenght", attachment.getSize());
        builder.type(attachment.getType());
        File file = service.getMessageAttachment(mkey, name);
        StreamingOutput stream = output -> Files.copy(file, output);
        builder.entity(stream);
        return builder.build();
    }

    private String getFileName(MultivaluedMap<String, String> header) {

        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {

                String[] name = filename.split("=");

                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return "unknown";
    }
}