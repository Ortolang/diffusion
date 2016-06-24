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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.google.common.io.Files;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.api.ApiUriBuilder;
import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
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
    public Response createThread(@FormParam("wskey") String wskey, @FormParam("name") String name, @FormParam("description") String description) throws MessageServiceException, KeyNotFoundException,
            AccessDeniedException, KeyAlreadyExistsException {
        LOGGER.log(Level.INFO, "POST /threads");
        String key = UUID.randomUUID().toString();
        service.createThread(key, wskey, name, description, true);
        URI location = uriInfo.getBaseUriBuilder().path(this.getClass()).path(key).build();
        return Response.created(location).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createThread(ThreadRepresentation representation) throws MessageServiceException, KeyNotFoundException, AccessDeniedException, KeyAlreadyExistsException {
        LOGGER.log(Level.INFO, "POST /threads");
        String key = UUID.randomUUID().toString();
        service.createThread(key, representation.getWorkspace(), representation.getName(), representation.getDescription(), true);
        URI location = uriInfo.getBaseUriBuilder().path(this.getClass()).path(key).build();
        return Response.created(location).build();
    }

    @GET
    @GZIP
    public Response listThreads(@QueryParam(value = "wskey") String wskey, @QueryParam(value = "o") @DefaultValue(value = "0") int offset,
            @QueryParam(value = "l") @DefaultValue(value = "10") int limit) throws KeyNotFoundException, AccessDeniedException, MessageServiceException, BrowserServiceException {
        LOGGER.log(Level.INFO, "GET /threads?wskey=" + wskey);
        List<String> wsfeeds = service.findThreadsForWorkspace(wskey);
        GenericCollectionRepresentation<ThreadRepresentation> representation = new GenericCollectionRepresentation<ThreadRepresentation>();
        for (int i = offset; i < limit; i++) {
            if (i < wsfeeds.size()) {
                OrtolangObjectInfos infos = browser.getInfos(wsfeeds.get(i));
                representation.addEntry(ThreadRepresentation.fromThreadAndInfos(service.readThread(wsfeeds.get(i)), infos));
            }
        }
        UriBuilder messages = ApiUriBuilder.getApiUriBuilder().path(MessageResource.class);
        representation.setOffset((offset < 0) ? 0 : offset);
        representation.setLimit(limit);
        representation.setSize(wsfeeds.size());
        representation.setFirst(messages.clone().queryParam("wskey", wskey).queryParam("o", 0).queryParam("l", limit).build());
        representation.setPrevious(messages.clone().queryParam("wskey", wskey).queryParam("o", Math.max(0, (offset - limit))).queryParam("l", limit).build());
        representation.setSelf(messages.clone().queryParam("wskey", wskey).queryParam("o", offset).queryParam("l", limit).build());
        representation.setNext(messages.clone().queryParam("wskey", wskey).queryParam("o", (wsfeeds.size() > (offset + limit)) ? (offset + limit) : offset).queryParam("l", limit).build());
        representation.setLast(messages.clone().queryParam("wskey", wskey).queryParam("o", ((wsfeeds.size() - 1) / limit) * limit).queryParam("l", limit).build());
        return Response.ok(representation).build();
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
        service.updateThread(key, representation.getName(), representation.getDescription());
        Thread feed = service.readThread(key);
        ThreadRepresentation newrepresentation = ThreadRepresentation.fromThread(feed);
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
    public Response browseThread(@PathParam(value = "key") String key, @QueryParam(value = "fromdate") Date from, @QueryParam(value = "o") @DefaultValue(value = "0") int offset,
            @QueryParam(value = "l") @DefaultValue(value = "10") int limit) throws KeyNotFoundException, MessageServiceException, OrtolangException, BrowserServiceException {
        LOGGER.log(Level.INFO, "GET /threads/" + key + "/messages");
        GenericCollectionRepresentation<MessageRepresentation> representation = new GenericCollectionRepresentation<MessageRepresentation>();
        UriBuilder content = ApiUriBuilder.getApiUriBuilder().path(MessageResource.class).path(key).path("messages");
        List<Message> msgs;
        if (from == null) {
            msgs = service.browseThread(key, offset, limit);
            long size = service.getSize(key).getSize();
            representation.setOffset((offset < 0) ? 0 : offset);
            representation.setLimit(limit);
            representation.setSize(size);
            representation.setFirst(content.clone().queryParam("key", key).queryParam("o", 0).queryParam("l", limit).build());
            representation.setPrevious(content.clone().queryParam("key", key).queryParam("o", Math.max(0, (offset - limit))).queryParam("l", limit).build());
            representation.setSelf(content.clone().queryParam("key", key).queryParam("o", offset).queryParam("l", limit).build());
            representation.setNext(content.clone().queryParam("key", key).queryParam("o", (size > (offset + limit)) ? (offset + limit) : offset).queryParam("l", limit).build());
            representation.setLast(content.clone().queryParam("key", key).queryParam("o", ((size - 1) / limit) * limit).queryParam("l", limit).build());
        } else {
            msgs = service.browseThreadSinceDate(key, from);
            representation.setSize(msgs.size());
            representation.setOffset(0);
            representation.setLimit(msgs.size());

        }
        List<MessageRepresentation> msgsrep = new ArrayList<MessageRepresentation> ();
        for (Message message : msgs) {
            OrtolangObjectInfos infos = browser.getInfos(message.getKey());
            msgsrep.add(MessageRepresentation.fromMessageAndInfos(message, infos));
        }
        representation.setEntries(msgsrep);
        return Response.ok(representation).build();
    }

    @POST
    @Path("/{key}/messages")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postMessage(@PathParam(value = "key") String key, @FormParam("parent") String parent, @FormParam("title") String title, @FormParam("body") String body)
            throws AccessDeniedException, MessageServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "POST /threads/" + key + "/messages");
        String mkey = UUID.randomUUID().toString();
        service.postMessage(key, mkey, parent, title, body);
        URI location = uriInfo.getBaseUriBuilder().path(this.getClass()).path(key).build();
        return Response.created(location).build();
    }

    @GET
    @Path("/{key}/messages/{mkey}")
    @GZIP
    public Response readMessage(@PathParam(value = "key") String key, @PathParam(value = "mkey") String mkey) throws AccessDeniedException, MessageServiceException, KeyNotFoundException, BrowserServiceException {
        LOGGER.log(Level.INFO, "GET /threads/" + key + "/messages/" + mkey);
        Message msg = service.readMessage(mkey);
        OrtolangObjectInfos infos = browser.getInfos(msg.getKey());
        return Response.ok(MessageRepresentation.fromMessageAndInfos(msg, infos)).build();
    }

    @PUT
    @Path("/{key}/messages/{mkey}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateMessage(@PathParam(value = "key") String key, @PathParam(value = "mkey") String mkey, @FormParam("title") String title, @FormParam("body") String body)
            throws AccessDeniedException, MessageServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "PUT /threads/" + key + "/messages/" + mkey);
        service.updateMessage(mkey, title, body);
        Message msg = service.readMessage(mkey);
        return Response.ok(MessageRepresentation.fromMessage(msg)).build();
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
                    if ((filename.trim().startsWith("filename"))) {
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

    @DELETE
    @Path("/{key}/messages/{mkey}/attachments/{name}")
    public Response deleteAttachment(@PathParam(value = "key") String key, @PathParam(value = "mkey") String mkey, @PathParam(value = "name") String name) throws AccessDeniedException,
            MessageServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "DELETE /threads/" + key + "/messages/" + mkey + "/attachments/" + name);
        service.removeMessageAttachment(mkey, name);
        return Response.ok().build();
    }

}