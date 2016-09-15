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

import fr.ortolang.diffusion.*;
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
import fr.ortolang.diffusion.jobs.JobService;
import fr.ortolang.diffusion.jobs.entity.Job;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.registry.*;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.*;
import fr.ortolang.diffusion.store.handle.HandleNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;
import fr.ortolang.diffusion.store.handle.entity.Handle;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.index.IndexStoreServiceException;
import fr.ortolang.diffusion.store.json.JsonStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreServiceException;
import fr.ortolang.diffusion.subscription.SubscriptionService;
import fr.ortolang.diffusion.subscription.SubscriptionServiceException;
import fr.ortolang.diffusion.worker.WorkerService;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("/admin")
@Produces({ MediaType.APPLICATION_JSON })
@Secured
@RolesAllowed("admin")
public class AdminResource {

    private static final Logger LOGGER = Logger.getLogger(AdminResource.class.getName());

    @EJB
    private JsonStoreService json;
    @EJB
    private IndexStoreService index;
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
    @EJB
    private JobService jobService;
    @EJB
    private WorkerService workerService;

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
        } catch (DataCollisionException | URISyntaxException e) {
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

        //        UriBuilder objects = ApiUriBuilder.getApiUriBuilder().path(AdminResource.class);
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
        if ( type != null && "name".equals(type)) {
            handles = handle.findHandlesByName(offset, limit, filter);
        }
        if ( type != null && "value".equals(type)) {
            handles = handle.findHandlesByValue(offset, limit, filter);
        }
        return Response.ok(handles).build();
    }
    
    @POST
    @Path("/store/handle")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public Response createHandle(Handle hdl) throws HandleStoreServiceException, UnsupportedEncodingException {
        LOGGER.log(Level.INFO, "POST /admin/store/handle");
        handle.recordHandle(hdl.getHandleString(), hdl.getKey(), hdl.getDataString());
        return Response.ok().build();
    }
    
    @GET
    @Path("/store/handle/{id}")
    @GZIP
    public Response readHandle(@PathParam("id") String id) throws HandleStoreServiceException, HandleNotFoundException {
        LOGGER.log(Level.INFO, "GET /admin/store/handle/" + id);
        Handle hdl = handle.readHandle(id);
        return Response.ok(hdl).build();
    }
    
    @PUT
    @Path("/store/handle/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public Response updateHandle(@PathParam("id") String id, Handle hdl) throws HandleStoreServiceException, UnsupportedEncodingException {
        LOGGER.log(Level.INFO, "PUT /admin/store/handle/" + id);
        handle.dropHandle(id);
        handle.recordHandle(id, hdl.getKey(), hdl.getDataString());
        return Response.ok(hdl).build();
    }
    
    @DELETE
    @Path("/store/handle/{id}")
    @GZIP
    public Response deleteHandle(@PathParam("id") String id) throws HandleStoreServiceException {
        LOGGER.log(Level.INFO, "DELETE /admin/store/handle/" + id);
        handle.dropHandle(id);
        return Response.ok().build();
    }

    @GET
    @Path("/store/json/documents/{key}")
    @GZIP
    public Response getJsonDocumentForKey(@PathParam(value = "key") String key) throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/json/" + key);
        String document = json.systemGetDocument(key);
        return Response.ok(document).build();
    }

    @DELETE
    @Path("/store/json/documents/{key}")
    @GZIP
    public Response deleteJsonDocumentForKey(@PathParam(value = "key") String key) throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "DELETE /admin/store/json/" + key);
        json.remove(key);
        return Response.ok().build();
    }

    @GET
    @Path("/store/json/search")
    @GZIP
    public Response searchInJsonStore(@QueryParam(value = "query") String query) throws JsonStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/json/search?query=" + query);
        List<String> documents = json.search(query);
        return Response.ok(documents).build();
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
    @GZIP
    public Response addSubscriptionFilters() throws SubscriptionServiceException {
        LOGGER.log(Level.INFO, "GET /subscription");
        subscription.addAdminFilters();
        return Response.ok().build();
    }

    @GET
    @Path("/jobs")
    @GZIP
    public Response getJobs(@QueryParam("type") String type, @QueryParam("o") Integer offset, @QueryParam("l") Integer limit, @DefaultValue("false") @QueryParam("failed") boolean failed, @DefaultValue("false") @QueryParam("unprocessed") boolean unprocessed) {
        LOGGER.log(Level.INFO, "GET /admin/jobs");
        List<Job> jobs;
        if (failed) {
            jobs = jobService.getFailedJobsOfType(type, offset, limit);
        } else if (unprocessed) {
            jobs = jobService.getUnprocessedJobsOfType(type, offset, limit);
        } else {
            if (type != null) {
                jobs = jobService.getJobsOfType(type, offset, limit);
            } else {
                jobs = jobService.getJobs(offset, limit);
            }
        }
        return Response.ok().entity(jobs).build();
    }

    @GET
    @Path("/jobs/{id}")
    @GZIP
    public Response getJob(@PathParam(value = "id") Long id) {
        LOGGER.log(Level.INFO, "GET /admin/jobs/" + id);
        Job job = jobService.read(id);
        return Response.ok().entity(job).build();
    }

    @DELETE
    @Path("/jobs")
    public Response removeJobs(@QueryParam(value = "e") String exception) {
        LOGGER.log(Level.INFO, "DELETE /admin/jobs");
        for (Job job : jobService.getFailedJobs()) {
            if (exception == null || exception.equals(job.getParameter("failedCausedBy"))) {
                jobService.remove(job.getId());
            }
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/jobs/{id}")
    public Response removeJob(@PathParam(value = "id") Long id) {
        LOGGER.log(Level.INFO, "DELETE /admin/jobs/" + id);
        jobService.remove(id);
        return Response.ok().build();
    }

    @GET
    @Path("/jobs/retry")
    public Response restoreJobs(@QueryParam("e") String exception) throws OrtolangException {
        LOGGER.log(Level.INFO, "GET /admin/jobs/retry");
        if (exception == null) {
            for (OrtolangWorker worker : workerService.getWorkers()) {
                worker.retryAll(false);
            }
        } else {
            List<Job> failedJobs = jobService.getFailedJobs();
            for (Job failedJob : failedJobs) {
                if (exception.equals(failedJob.getParameter("failedCausedBy"))) {
                    workerService.getWorkerForJobType(failedJob.getType()).retry(failedJob.getId());
                }
            }
        }
        return Response.ok().build();
    }

    @GET
    @Path("/jobs/{id}/retry")
    public Response retryJob(@PathParam(value = "id") Long id) throws OrtolangException {
        LOGGER.log(Level.INFO, "GET /admin/jobs/" + id + "/retry");
        Job job = jobService.read(id);
        OrtolangWorker worker = workerService.getWorkerForJobType(job.getType());
        worker.retry(id);
        return Response.ok().build();
    }

    @GET
    @Path("/jobs/count")
    public Response countJobs(@QueryParam("type") String type, @DefaultValue("false") @QueryParam("unprocessed") boolean unprocessed, @DefaultValue("false") @QueryParam("failed") boolean failed) throws CoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/jobs/count");
        Map<String, Long> map = new HashMap<>(1);
        if (failed) {
            map.put("count", jobService.countFailedJobs());
        } else if (unprocessed) {
            map.put("count", jobService.countUnprocessedJobs());
        } else {
            if (type != null) {
                map.put("count", jobService.countJobsOfType(type));
            } else {
                map.put("count", jobService.countJobs());
            }
        }
        return Response.ok().entity(map).build();
    }

    @GET
    @Path("/jobs/workers")
    public Response getWorkersState() throws OrtolangException {
        LOGGER.log(Level.INFO, "GET /admin/jobs/workers");
        Map<String, String> workersState = new HashMap<>();
        for (OrtolangWorker worker : workerService.getWorkers()) {
            if (worker != null) {
                workersState.put(worker.getName(), worker.getState());
            }
        }
        return Response.ok().entity(workersState).build();
    }

    @GET
    @Path("/jobs/workers/{id}/start")
    public Response restartWorker(@PathParam("id") String id) throws OrtolangException {
        LOGGER.log(Level.INFO, "GET /admin/jobs/workers/" + id + "/start");
        workerService.startWorker(id);
        return Response.ok().build();
    }

    @GET
    @Path("/jobs/workers/queue")
    public Response getQueues() throws OrtolangException {
        LOGGER.log(Level.INFO, "GET /admin/jobs/workers/queue");
        List<OrtolangJob> queue = new ArrayList<>();
        for (OrtolangWorker worker : workerService.getWorkers()) {
             queue.addAll(worker.getQueue());
        }
        return Response.ok(queue).build();
    }

    @GET
    @Path("/jobs/workers/{id}/queue")
    public Response getWorkerQueue(@PathParam("id") String id) throws OrtolangException {
        LOGGER.log(Level.INFO, "GET /admin/jobs/workers/" + id + "/queue");
        List<OrtolangJob> queue = workerService.getQueue(id);
        return Response.ok(queue).build();
    }
}
