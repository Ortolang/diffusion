package fr.ortolang.diffusion.api.admin;

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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import fr.ortolang.diffusion.jobs.entity.Job;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.api.ApiUriBuilder;
import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.Secured;
import fr.ortolang.diffusion.api.object.ObjectResource;
import fr.ortolang.diffusion.api.runtime.HumanTaskRepresentation;
import fr.ortolang.diffusion.api.runtime.ProcessRepresentation;
import fr.ortolang.diffusion.api.runtime.ProcessTypeRepresentation;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.MetadataFormatException;
import fr.ortolang.diffusion.event.EventService;
import fr.ortolang.diffusion.event.EventServiceException;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreContent;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;
import fr.ortolang.diffusion.store.handle.entity.Handle;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.index.IndexStoreServiceException;
import fr.ortolang.diffusion.store.index.IndexStoreServiceWorker;
import fr.ortolang.diffusion.store.json.JsonStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreServiceException;
import fr.ortolang.diffusion.store.json.JsonStoreServiceWorker;
import fr.ortolang.diffusion.subscription.SubscriptionService;
import fr.ortolang.diffusion.subscription.SubscriptionServiceException;

@Path("/admin")
@Produces({ MediaType.APPLICATION_JSON })
@Secured
@RolesAllowed("admin")
public class AdminResource {

    private static final Logger LOGGER = Logger.getLogger(AdminResource.class.getName());

    @EJB
    private JsonStoreService json;
    @EJB
    private JsonStoreServiceWorker jsonWorker;
    @EJB
    private IndexStoreService index;
    @EJB
    private IndexStoreServiceWorker indexWorker;
    @EJB
    private HandleStoreService handle;
    @EJB
    private BinaryStoreService binary;
    @EJB
    private RegistryService registry;
    @EJB
    private CoreService core;
    @EJB
    private RuntimeService runtime;
    @EJB
    private EventService event;
    @EJB
    private SubscriptionService subscription;
    @EJB
    private MembershipService membership;

    @GET
    @Path("/infos/{service}")
    @GZIP
    public Response getRegistryInfos(@PathParam(value = "service") String serviceName) throws OrtolangException {
        LOGGER.log(Level.INFO, "GET /admin/infos/" + serviceName);
        OrtolangService service = OrtolangServiceLocator.findService(serviceName);
        Map<String, String> infos = service.getServiceInfos();
        return Response.ok(infos).build();
    }

    @GET
    @Path("/registry/entries")
    @GZIP
    public Response listEntries(@QueryParam("kfilter") String kfilter, @QueryParam("ifilter") String ifilter) throws RegistryServiceException {
        LOGGER.log(Level.INFO, "GET /admin/registry/entries?kfilter=" + kfilter + "&ifilter=" + ifilter);
        List<RegistryEntry> entries = registry.systemListEntries(kfilter, ifilter);
        return Response.ok(entries).build();
    }
    
    @GET
    @Path("/registry/entries/{key}")
    @GZIP
    public Response readEntry(@PathParam("key") String key) throws RegistryServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /admin/registry/entries/" + key);
        RegistryEntry entry = registry.systemReadEntry(key);
        return Response.ok(entry).build();
    }

    @PUT
    @Path("/registry/entries/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public Response updateEntry(@PathParam("key") String key, RegistryEntry entry) throws RegistryServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "PUT /admin/registry/entries/" + key);
        // TODO compare what changes in order to update state...
        return Response.serverError().entity("NOT IMPLEMENTED").build();
    }

    @DELETE
    @Path("/registry/entries/{key}")
    public Response deleteEntry(@PathParam("key") String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException {
        LOGGER.log(Level.INFO, "DELETE /admin/registry/entries/" + key);
        registry.delete(key, true);
        return Response.ok().build();
    }
    

    @POST
    @Path("/core/metadata")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @GZIP
    public Response createMetadata(@MultipartForm MetadataObjectFormRepresentation form) throws OrtolangException, KeyNotFoundException, CoreServiceException, MetadataFormatException, DataNotFoundException, BinaryStoreServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException, RegistryServiceException, AuthorisationServiceException, IndexingServiceException {
    	LOGGER.log(Level.INFO, "POST /admin/core/metadata");	
    	try {
	    	if (form.getKey() == null) {
	            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'key' is mandatory").build();
	        }
	    	if (form.getName() == null) {
	            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'name' is mandatory").build();
	        }
	        if (form.getStream() != null) {
	            form.setStreamHash(core.put(form.getStream()));
	        }
	
	    	core.systemCreateMetadata(form.getKey(), form.getName(), form.getStreamHash(), form.getFilename());
	    	//TODO return with the metadata key
	    	URI location = ApiUriBuilder.getApiUriBuilder().path(ObjectResource.class).path(form.getKey()).build();
	    	return Response.created(location).build();
	    } catch (DataCollisionException e) {
	        LOGGER.log(Level.SEVERE, "an error occured while creating workspace element: " + e.getMessage(), e);
	        return Response.serverError().entity(e.getMessage()).build();
	    }
    }
    
    @DELETE
    @Path("/membership/profiles/{key}")
    public Response deleteProfile(@PathParam("key") String key) throws KeyNotFoundException, AccessDeniedException, MembershipServiceException {
        LOGGER.log(Level.INFO, "DELETE /admin/membership/profiles/" + key);
        membership.deleteProfile(key);
        return Response.ok().build();
    }
    
    @GET
    @Path("/runtime/types")
    @GZIP
    public Response listDefinitions() throws RuntimeServiceException {
        LOGGER.log(Level.INFO, "GET /admin/runtime/types");
        List<ProcessTypeRepresentation> types = runtime.listProcessTypes(false).stream().map(ProcessTypeRepresentation::fromProcessType).collect(Collectors.toCollection(ArrayList::new));
        return Response.ok(types).build();
    }

    @GET
    @Path("/runtime/processes")
    @GZIP
    public Response listProcesses(@QueryParam("state") String state) throws RegistryServiceException, RuntimeServiceException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /admin/runtime/processes?state=" + state);
        State estate = null;
        if (state != null && state.length() > 0) {
            try {
                estate = State.valueOf(state);
            } catch (IllegalArgumentException e) {
                return Response.status(Status.BAD_REQUEST).entity("unknown state: " + e.getMessage()).build();
            }
        }
        List<ProcessRepresentation> entries = runtime.systemListProcesses(estate).stream().map(ProcessRepresentation::fromProcess).collect(Collectors.toCollection(ArrayList::new));
        return Response.ok(entries).build();
    }

    @GET
    @Path("/runtime/tasks")
    @GZIP
    public Response listTasks() throws RegistryServiceException, RuntimeServiceException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /admin/runtime/tasks");
        List<HumanTaskRepresentation> entries = runtime.systemListTasks().stream().map(HumanTaskRepresentation::fromHumanTask).collect(Collectors.toCollection(ArrayList::new));
        return Response.ok(entries).build();
    }
    
    @GET
    @Path("/store/binary")
    @GZIP
    public Response browseBinaryStoreRoot() throws BinaryStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/binary");
        List<BinaryStoreContent> infos = binary.systemBrowse(null, null);
        return Response.ok(infos).build();
    }
    
    @GET
    @Path("/store/binary/{name}")
    @GZIP
    public Response browseBinaryStoreVolume(@PathParam("name") String name) throws BinaryStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/binary/" + name);
        List<BinaryStoreContent> infos = binary.systemBrowse(name, null);
        return Response.ok(infos).build();
    }
    
    @GET
    @Path("/store/binary/{name}/{prefix}")
    @GZIP
    public Response browseBinaryStorePrefix(@PathParam("name") String name, @PathParam("prefix") String prefix) throws BinaryStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/binary/" + name + "/" + prefix);
        List<BinaryStoreContent> infos = binary.systemBrowse(name, prefix);
        return Response.ok(infos).build();
    }
    
    @GET
    @Path("/store/binary/{name}/{prefix}/{hash}")
    @GZIP
    public Response getBinaryStoreContent(@PathParam("name") String name, @PathParam("prefix") String prefix, @PathParam("hash") String hash) throws BinaryStoreServiceException, DataNotFoundException {
        LOGGER.log(Level.INFO, "GET /admin/store/binary/" + name + "/" + prefix + "/" + hash);
        File content = binary.getFile(hash);
        return Response.ok(content).build();
    }
    
    @GET
    @Path("/store/events")
    @GZIP
    @SuppressWarnings("unchecked")
    public Response searchEvents(@DefaultValue("0") @QueryParam("o") int offset, @DefaultValue("2000") @QueryParam("l") int limit, @QueryParam("ofrom") String ofrom, @QueryParam("otype") String otype,
            @QueryParam("etype") String etype, @QueryParam("throwed") String throwed, @DefaultValue("0") @QueryParam("after") long after) throws EventServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/events");
        long nbresults = event.systemCountEvents(etype, ofrom, otype, throwed, after);
        List<OrtolangEvent> events = (List<OrtolangEvent>) event.systemFindEvents(etype, ofrom, otype, throwed, after, offset, limit);
        
        UriBuilder objects = ApiUriBuilder.getApiUriBuilder().path(AdminResource.class);
        GenericCollectionRepresentation<OrtolangEvent> representation = new GenericCollectionRepresentation<OrtolangEvent>();
        representation.setEntries(events);
        representation.setOffset((offset <= 0) ? 1 : offset);
        representation.setSize(nbresults);
        representation.setLimit(events.size());
//        representation.setFirst(objects.clone().queryParam("o", 0).queryParam("l", limit).queryParam("ofrom", ofrom).queryParam("otype", otype).queryParam("etype", etype).queryParam("throwed", throwed).queryParam("after", after).build());
//        representation.setPrevious(objects.clone().queryParam("o", Math.max(0, (offset - limit))).queryParam("l", limit).queryParam("ofrom", ofrom).queryParam("otype", otype).queryParam("etype", etype).queryParam("throwed", throwed).queryParam("after", after).build());
//        representation.setSelf(objects.clone().queryParam("o", offset).queryParam("l", limit).queryParam("ofrom", ofrom).queryParam("otype", otype).queryParam("etype", etype).queryParam("throwed", throwed).queryParam("after", after).build());
//        representation.setNext(objects.clone().queryParam("o", (nbresults > (offset + limit)) ? (offset + limit) : offset).queryParam("l", limit).queryParam("ofrom", ofrom).queryParam("otype", otype).queryParam("etype", etype).queryParam("throwed", throwed).queryParam("after", after).build());
//        representation.setLast(objects.clone().queryParam("o", ((nbresults - 1) / limit) * limit).queryParam("l", limit).queryParam("ofrom", ofrom).queryParam("otype", otype).queryParam("etype", etype).queryParam("throwed", throwed).queryParam("after", after).build());
        return Response.ok(representation).build();
    }
    
    @GET
    @Path("/store/handle")
    @GZIP
    public Response searchHandles(@DefaultValue("0") @QueryParam("o") int offset, @DefaultValue("10000") @QueryParam("l") int limit, @QueryParam("filter") String filter, @DefaultValue("name") @QueryParam("type") String type) throws HandleStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/handle");
        List<Handle> handles = Collections.emptyList();
        if ( type != null && type.equals("name") ) {
            handles = handle.findHandlesByName(offset, limit, filter);
        }
        if ( type != null && type.equals("value") ) {
            handles = handle.findHandlesByValue(offset, limit, filter);
        } 
        return Response.ok(handles).build();
    }
    
    @GET
    @Path("/store/json/worker")
    @GZIP
    public Response getJsonWorkerState() throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/worker/json");
        List<Job> jobs = jsonWorker.getQueueJobs();
        return Response.ok(jobs).build();
    }
    
    @GET
    @Path("/store/json/documents/{key}")
    @GZIP
    public Response getJsonDocumentForKey(@PathParam(value = "key") String key) throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/json/" + key);
        String document = json.systemGetDocument(key);
        return Response.ok(document).build();
    }

    @GET
    @Path("/store/index/worker")
    @GZIP
    public Response getIndexWorkerState() throws IndexStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/worker/index");
        List<Job> jobs = indexWorker.getQueueJobs();
        return Response.ok(jobs).build();
    }

    @GET
    @Path("/store/index/documents/{key}")
    @GZIP
    public Response getIndexDocumentForKey(@PathParam(value = "key") String key) throws IndexStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/index/" + key);
        String document = index.systemGetDocument(key);
        return Response.ok(document).build();
    }
    
    @GET
    @Path("/subscription")
    public Response addSubscriptionFilters() throws SubscriptionServiceException {
        LOGGER.log(Level.INFO, "GET /subscription");
        subscription.addAdminFilters();
        return Response.ok().build();
    }
    
//    @PUT
//    @Path("/core/workspace")
//    public Response updateWorkspace() throws CoreServiceException {
//        
//    }

}
