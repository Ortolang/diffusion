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
import fr.ortolang.diffusion.api.config.ConfigResource;
import fr.ortolang.diffusion.api.object.ObjectResource;
import fr.ortolang.diffusion.api.profile.ProfileRepresentation;
import fr.ortolang.diffusion.api.runtime.HumanTaskRepresentation;
import fr.ortolang.diffusion.api.runtime.ProcessRepresentation;
import fr.ortolang.diffusion.api.runtime.ProcessTypeRepresentation;
import fr.ortolang.diffusion.api.workspace.WorkspaceRepresentation;
import fr.ortolang.diffusion.api.search.SearchResourceHelper;
import fr.ortolang.diffusion.api.search.SearchResultRepresentation;
import fr.ortolang.diffusion.api.search.SearchResultsRepresentation;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.MetadataFormatException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.event.EventService;
import fr.ortolang.diffusion.event.EventServiceException;
import fr.ortolang.diffusion.ftp.FtpService;
import fr.ortolang.diffusion.ftp.FtpServiceException;
import fr.ortolang.diffusion.ftp.FtpSession;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.jobs.JobService;
import fr.ortolang.diffusion.jobs.entity.Job;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.registry.*;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.search.SearchQuery;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.statistics.StatisticsService;
import fr.ortolang.diffusion.statistics.StatisticsServiceException;
import fr.ortolang.diffusion.store.binary.*;
import fr.ortolang.diffusion.store.handle.HandleNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;
import fr.ortolang.diffusion.store.handle.entity.Handle;
import fr.ortolang.diffusion.subscription.SubscriptionService;
import fr.ortolang.diffusion.subscription.SubscriptionServiceException;
import fr.ortolang.diffusion.worker.WorkerService;
import fr.ortolang.diffusion.xml.ImportExportService;
import fr.ortolang.diffusion.xml.ImportExportServiceException;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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
;
    @EJB
    private HandleStoreService handle;
    @EJB
    private SearchService search;
    @EJB
    private BinaryStoreService binary;
    @EJB
    private RegistryService registry;
    @EJB
    private SecurityService security;
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
    private ReferentialService referentialService;
    @EJB
    private WorkerService workerService;
    @EJB
    private FtpService ftpService;
    @EJB
    private StatisticsService statistics;
    @EJB
    private ImportExportService export;

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
    
    @PUT
    @Path("/security/rules")
    @GZIP
    public Response setRules(RulesRepresentation rules) throws SecurityServiceException, KeyNotFoundException {
    	security.systemSetRule(rules.getKey(), rules.getSubject(), rules.getPermissions(), true);
    	return Response.ok().build();
    }

    @GET
    @Path("/referential/entities/export")
    @Produces({ MediaType.TEXT_HTML, MediaType.WILDCARD })
    public Response dumpReferentialEntities() throws ImportExportServiceException, IOException, RegistryServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /admin/referential/entities/export");
        LOGGER.log(Level.FINE, "exporting referential entities");
        ResponseBuilder builder = Response.ok();
        builder.header("Content-Disposition", "attachment; filename*=UTF-8''ortolang-dump.tar.gz");
        builder.type("application/x-gzip");
        
        Set<String> keys = new HashSet<String>(registry.list(0, 1000000, "/referential/entity", null));

        StreamingOutput stream = output -> {
            try {
                export.dump(keys, output, new OrtolangImportExportLogger() {
                    @Override
                    public void log(LogType type, String message) {
                        LOGGER.log(Level.FINE, type + " : " + message);
                    }
                }, true, true);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                // TODO maybe include a warning in the archive...
            }
        };
        builder.entity(stream);

        return builder.build();
    }

    @GET
    @Path("/core/workspace/{key}/export")
    @Produces({ MediaType.TEXT_HTML, MediaType.WILDCARD })
    public Response dumpWorkspace(@PathParam("key") String key, @DefaultValue(value = "true") @QueryParam("binary") boolean withbinary) throws ImportExportServiceException, IOException, RegistryServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /admin/core/workspace/" + key + "/export");
        ResponseBuilder builder;
        OrtolangObjectIdentifier identifier = registry.lookup(key);
        if (!identifier.getService().equals(CoreService.SERVICE_NAME) || !identifier.getType().equals(Workspace.OBJECT_TYPE)) {
            builder = Response.status(Status.BAD_REQUEST).entity("Object is not a " + Workspace.OBJECT_TYPE);
        } else {
            LOGGER.log(Level.FINE, "exporting workspace using format tar");
            builder = Response.ok();
            builder.header("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(key, "utf-8") + "-dump.tar.gz");
            builder.type("application/x-gzip");

            StreamingOutput stream = output -> {
                try {
                    export.dump(Collections.singleton(key), output, new OrtolangImportExportLogger() {
                        @Override
                        public void log(LogType type, String message) {
                            LOGGER.log(Level.FINE, type + " : " + message);
                        }
                    }, true, withbinary);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                    // TODO maybe include a warning in the archive...
                }
            };
            builder.entity(stream);
        }

        return builder.build();
    }
    
    @POST
    @Path("/core/workspace/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response restoreWorkspace(@MultipartForm ImportFormRepresentation form) throws ImportExportServiceException, IOException, RegistryServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "POST /admin/core/workspace/import");
//        ResponseBuilder builder;
//        if (form.getDump() == null) {
//            builder = Response.status(Status.BAD_REQUEST);
//            builder.entity("missing dump file");
//            return builder.build();
//        } 
//        builder = Response.ok();
//        builder.type("application/json");
//        StreamingOutput stream = output -> {
//            JsonImportLogger logger = new JsonImportLogger();
//            logger.setOutputStream(output);
//            logger.start();
//            try {
//                export.restore(form.getDump(), logger);
//            } catch (Exception e) {
//                LOGGER.log(Level.SEVERE, "error during restore workspace", e);
//                logger.log(LogType.ERROR, "CRITICAL ERROR : " + e.getMessage());
//            }
//            logger.finish();
//        };
//        builder.entity(stream);
//        return builder.build();
        return Response.serverError().build();
    }

    @POST
    @Path("/core/metadata")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @GZIP
    public Response createMetadata(@MultipartForm MetadataObjectFormRepresentation form) throws OrtolangException, KeyNotFoundException, CoreServiceException, MetadataFormatException,
            DataNotFoundException, BinaryStoreServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException, RegistryServiceException, AuthorisationServiceException,
            IndexingServiceException {
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
            URI location = ApiUriBuilder.getApiUriBuilder().path(ObjectResource.class).path(form.getKey()).build();
            return Response.created(location).build();
        } catch (DataCollisionException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "an error occured while creating workspace element: " + e.getMessage(), e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("/core/workspace/{wskey}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateWorkspace(@PathParam(value = "wskey") String wskey, WorkspaceRepresentation representation) throws KeyNotFoundException {
        LOGGER.log(Level.INFO, "PUT /workspaces/" + wskey);
        if (representation.getKey() != null && representation.getKey().length() > 0) {
            try {
				core.systemUpdateWorkspace(wskey, representation.getAlias(), representation.isChanged(), representation.getHead(), representation.getMembers(), representation.getPrivileged(), representation.isReadOnly(), representation.getType());
			} catch (NotificationServiceException | CoreServiceException e) {
				LOGGER.log(Level.SEVERE, "an error occured while updating workspace element: " + e.getMessage(), e);
				return Response.serverError().entity(e.getMessage()).build();
			}
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("representation does not contains a valid key").build();
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
    @Path("/membership/profiles")
    public Response listProfiles() throws MembershipServiceException {
        LOGGER.log(Level.INFO, "GET /admin/membership/profiles");
        List<Profile> profiles = membership.systemListProfiles();
        List<ProfileRepresentation> representations = new ArrayList<>(profiles.size());
        List<String> connectedUsers = subscription.getConnectedUsers();
        for (Profile profile : profiles) {
            ProfileRepresentation representation = ProfileRepresentation.fromProfile(profile);
            representation.setKey(profile.getId());
            representation.setConnected(connectedUsers.contains(representation.getKey()));
            representations.add(representation);
        }
        return Response.ok().entity(representations).build();
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
    public Response searchEvents(@DefaultValue("0") @QueryParam("o") int offset, @DefaultValue("2000") @QueryParam("l") int limit, @QueryParam("ofrom") String ofrom,
            @QueryParam("otype") String otype, @QueryParam("etype") String etype, @QueryParam("throwed") String throwed, @DefaultValue("0") @QueryParam("after") long after)
            throws EventServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/events");
        long nbresults = event.systemCountEvents(etype, ofrom, otype, throwed, after);
        List<OrtolangEvent> events = (List<OrtolangEvent>) event.systemFindEvents(etype, ofrom, otype, throwed, after, offset, limit);

        GenericCollectionRepresentation<OrtolangEvent> representation = new GenericCollectionRepresentation<OrtolangEvent>();
        representation.setEntries(events);
        representation.setOffset((offset <= 0) ? 1 : offset);
        representation.setSize(nbresults);
        representation.setLimit(events.size());
        return Response.ok(representation).build();
    }

    @GET
    @Path("/store/handle")
    @GZIP
    public Response searchHandles(@DefaultValue("0") @QueryParam("o") int offset, @DefaultValue("10000") @QueryParam("l") int limit, @QueryParam("filter") String filter,
            @DefaultValue("name") @QueryParam("type") String type) throws HandleStoreServiceException {
        LOGGER.log(Level.INFO, "GET /admin/store/handle");
        List<Handle> handles = Collections.emptyList();
        if (type != null && "name".equals(type)) {
            handles = handle.findHandlesByName(filter);
        }
        if (type != null && "value".equals(type)) {
            handles = handle.findHandlesByValue(filter);
        }
        List<HandleRepresentation> handlesRepresentations = new ArrayList<>();
        for (Handle handle : handles) {
            HandleRepresentation handleRepresentation = HandleRepresentation.fromHandle(handle);
            handlesRepresentations.add(handleRepresentation);
        }
        return Response.ok(handlesRepresentations).build();
    }

    @POST
    @Path("/store/handle")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public Response createHandle(HandleRepresentation hdl) throws HandleStoreServiceException, UnsupportedEncodingException {
        LOGGER.log(Level.INFO, "POST /admin/store/handle");
        handle.recordHandle(hdl.getHandle(), hdl.getKey(), hdl.getUrl());
        return Response.ok().build();
    }

    @GET
    @Path("/store/handle/{id:.*}")
    @GZIP
    public Response readHandle(@PathParam("id") String id) throws HandleStoreServiceException, HandleNotFoundException {
        LOGGER.log(Level.INFO, "GET /admin/store/handle/" + id);
        Handle hdl = handle.readHandle(id);
        HandleRepresentation handleRepresentation = HandleRepresentation.fromHandle(hdl);
        return Response.ok(handleRepresentation).build();
    }

    @PUT
    @Path("/store/handle/{id:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public Response updateHandle(@PathParam("id") String id, HandleRepresentation hdl) throws HandleStoreServiceException, UnsupportedEncodingException {
        LOGGER.log(Level.INFO, "PUT /admin/store/handle/" + id);
        handle.dropHandle(hdl.getHandle());
        handle.recordHandle(hdl.getHandle(), hdl.getKey(), hdl.getUrl());
        return Response.ok(hdl).build();
    }

    @DELETE
    @Path("/store/handle/{id:.*}")
    @GZIP
    public Response deleteHandle(@PathParam("id") String id) throws HandleStoreServiceException {
        LOGGER.log(Level.INFO, "DELETE /admin/store/handle/" + id);
        handle.dropHandle(id);
        return Response.ok().build();
    }

    @GET
    @Path("/index/search")
    @GZIP
    public Response search(@Context HttpServletRequest request) {
        SearchQuery query = SearchResourceHelper.executeQuery(request);
		return Response.ok(SearchResultsRepresentation.fromSearchResult(search.systemSearch(query))).build();
    }

    @GET
    @Path("/subscription")
    @GZIP
    public Response addSubscriptionFilters() throws SubscriptionServiceException {
        LOGGER.log(Level.INFO, "GET /subscription");
        subscription.addAdminFilters();
        return Response.ok().build();
    }

    @POST
    @Path("/subscription/broadcast")
    @GZIP
    public Response broadcastMessage(@FormParam("users") String users, @FormParam("title") String title, @FormParam("body") String body) throws SubscriptionServiceException {
        LOGGER.log(Level.INFO, "POST /subscription/broadcast");
        subscription.broadcastMessage(Arrays.asList(users.split(",")), title, body);
        return Response.ok().build();
    }

    @GET
    @Path("/jobs")
    @GZIP
    public Response getJobs(@QueryParam("type") String type, @QueryParam("o") Integer offset, @QueryParam("l") Integer limit, @DefaultValue("false") @QueryParam("failed") boolean failed,
            @DefaultValue("false") @QueryParam("unprocessed") boolean unprocessed) {
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
    public Response countJobs(@QueryParam("type") String type, @DefaultValue("false") @QueryParam("unprocessed") boolean unprocessed, @DefaultValue("false") @QueryParam("failed") boolean failed)
            throws CoreServiceException {
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

    @GET
    @Path("/ftp/sessions")
    public Response getActiveFtpSessions() throws FtpServiceException {
        LOGGER.log(Level.INFO, "GET /admin/ftp/sessions");
        Set<FtpSession> sessions = ftpService.getActiveSessions();
        return Response.ok(sessions).build();
    }

    @GET
    @Path("/stats/piwik")
    public Response probePiwik() throws StatisticsServiceException, OrtolangException {
        LOGGER.log(Level.INFO, "GET /statistics/piwik");
        statistics.probePiwik();
        return Response.ok().build();
    }

    @POST
    @Path("/stats/piwik")
    public Response collectPiwikForRange(@FormParam("range") String range, @FormParam("timestamp") long timestamp) throws StatisticsServiceException, OrtolangException {
        LOGGER.log(Level.INFO, "POST /statistics/piwik");
        statistics.collectPiwikForRange(range, timestamp);
        return Response.ok().build();
    }

    @GET
    @Path("/config/refresh")
    public Response refreshConfig() throws IOException, OrtolangException {
        LOGGER.log(Level.INFO, "GET /config/refresh");
        OrtolangConfig.getInstance().refresh();
        ConfigResource.clear();
        return Response.ok().build();
    }
}
