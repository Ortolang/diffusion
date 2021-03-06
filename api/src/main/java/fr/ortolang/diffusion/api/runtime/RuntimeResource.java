package fr.ortolang.diffusion.api.runtime;

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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.ortolang.diffusion.membership.MembershipServiceException;
import org.jboss.resteasy.annotations.GZIP;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import fr.ortolang.diffusion.api.ApiUriBuilder;
import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.runtime.entity.ProcessType;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;

@Path("/runtime")
@Produces({ MediaType.APPLICATION_JSON })
public class RuntimeResource {

    private static final Logger LOGGER = Logger.getLogger(RuntimeResource.class.getName());

    @EJB
    private RuntimeService runtime;

    @GET
    @Path("/types")
    @GZIP
    public Response listDefinitions() throws RuntimeServiceException {
        LOGGER.log(Level.INFO, "GET /runtime/types");
        List<ProcessType> types = runtime.listProcessTypes(true);

        GenericCollectionRepresentation<ProcessTypeRepresentation> representation = new GenericCollectionRepresentation<ProcessTypeRepresentation>();
        for (ProcessType type : types) {
            representation.addEntry(ProcessTypeRepresentation.fromProcessType(type));
        }
        representation.setOffset(0);
        representation.setSize(types.size());
        representation.setLimit(types.size());
        return Response.ok(representation).build();
    }

    @GET
    @Path("/processes")
    @GZIP
    public Response listProcesses(@QueryParam("wskey") String wskey, @QueryParam("state") String state) throws RuntimeServiceException, AccessDeniedException {
        LOGGER.log(Level.INFO, "GET /runtime/processes");
        List<Process> instances;
        if ( wskey != null ) {
            if ( state != null ) {
                instances = runtime.listWorkspaceProcesses(wskey, State.valueOf(state));
            } else {
                instances = runtime.listWorkspaceProcesses(wskey, null);
            }
        } else {
            if ( state != null ) {
                instances = runtime.listCallerProcesses(State.valueOf(state));
            } else {
                instances = runtime.listCallerProcesses(null);
            }
        }

        GenericCollectionRepresentation<ProcessRepresentation> representation = new GenericCollectionRepresentation<ProcessRepresentation>();
        for (Process instance : instances) {
            ProcessRepresentation rep = ProcessRepresentation.fromProcess(instance);
            representation.addEntry(rep);
        }
        representation.setOffset(0);
        representation.setSize(instances.size());
        representation.setLimit(instances.size());
        return Response.ok(representation).build();
    }


    @POST
    @Path("/processes")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @GZIP
    public Response startInstance(MultivaluedMap<String, String> params) throws RuntimeServiceException, AccessDeniedException, KeyAlreadyExistsException, URISyntaxException {
        LOGGER.log(Level.INFO, "POST(application/x-www-form-urlencoded) /runtime/processes");
        String key = UUID.randomUUID().toString();

        String definition;
        if ( !params.containsKey("process-type") ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'process-type' is mandatory").build();
        } else {
            definition = params.remove("process-type").get(0);
        }
        String name;
        if ( !params.containsKey("process-name") ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'process-name' is mandatory").build();
        } else {
            name = params.remove("process-name").get(0);
        }
        String wskey = null;
        if ( params.containsKey("wskey") ) {
            wskey = params.remove("wskey").get(0);
        }

        Map<String, Object> mparams = new HashMap<String, Object> ();
        params.entrySet().stream().filter(entry -> !entry.getValue().isEmpty()).forEach(entry -> {
            StringBuilder values = new StringBuilder();
            for (String value : entry.getValue()) {
                values.append(value).append(",");
            }
            mparams.put(entry.getKey(), values.substring(0, values.length() - 1));
        });

        try {
            Process process = runtime.createProcess(key, definition, name, wskey);
            runtime.startProcess(key, mparams);
            URI newly = ApiUriBuilder.getApiUriBuilder().path(RuntimeResource.class).path("processes").path(key).build();
            return Response.created(newly).entity(ProcessRepresentation.fromProcess(process)).build();
        } catch (SecurityException | IllegalStateException e) {
            throw new RuntimeServiceException(e);
        }
    }

    @POST
    @Path("/processes")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @GZIP
    public Response startInstance(MultipartFormDataInput input)
            throws RuntimeServiceException, AccessDeniedException, KeyAlreadyExistsException, IOException, CoreServiceException, DataCollisionException, URISyntaxException {
        LOGGER.log(Level.INFO, "POST(multipart/form-data) /runtime/processes");
        String key = UUID.randomUUID().toString();

        Map<String, Object> mparams = new HashMap<String, Object> ();
        Map<String, List<InputPart>> form = input.getFormDataMap();

        String definition;
        if ( !form.containsKey("process-type") ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'process-type' is mandatory").build();
        } else {
            definition = form.remove("process-type").get(0).getBodyAsString();
        }
        String name;
        if ( !form.containsKey("process-name") ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'process-name' is mandatory").build();
        } else {
            name = form.remove("process-name").get(0).getBodyAsString();
        }
        String wskey = null;
        if ( form.containsKey("wskey") ) {
            wskey = form.remove("wskey").get(0).getBodyAsString();
        }

        for ( Entry<String, List<InputPart>> entry : form.entrySet() ) {
            if ( !entry.getValue().isEmpty() ) {
                StringBuilder values = new StringBuilder();
                for ( InputPart value : entry.getValue() ) {
                    if ( value.getHeaders().containsKey("Content-Disposition") && value.getHeaders().getFirst("Content-Disposition").contains("filename=") ) {
                        LOGGER.log(Level.FINE, "seems this part [" + entry.getKey() + "] is a file");
                        InputStream is = value.getBody(InputStream.class, null);
                        java.nio.file.Path file = Files.createTempFile("process-file", ".tmp");
                        Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
                        values.append(file).append(",");
                    } else {
                        LOGGER.log(Level.FINE, "seems this part  [" + entry.getKey() + "] is a simple text value");
                        values.append(value.getBodyAsString()).append(",");
                    }
                }
                mparams.put(entry.getKey(), values.substring(0, values.length()-1));
            }
        }
        Process process = runtime.createProcess(key, definition, name, wskey);
        runtime.startProcess(key, mparams);
        URI newly = ApiUriBuilder.getApiUriBuilder().path(RuntimeResource.class).path("processes").path(key).build();
        return Response.created(newly).entity(ProcessRepresentation.fromProcess(process)).build();
    }

    @GET
    @Path("/processes/{key}")
    @GZIP
    public Response readProcess(@PathParam("key") String key) throws RuntimeServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /runtime/processes/" + key);
        Process process = runtime.readProcess(key);
        ProcessRepresentation representation = ProcessRepresentation.fromProcess(process);
        return Response.ok(representation).build();
    }

    @DELETE
    @Path("/processes/{key}")
    @GZIP
    public Response abortProcess(@PathParam("key") String key) throws RuntimeServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "DELETE /runtime/processes/" + key);
        runtime.abortProcess(key);
        Process process = runtime.readProcess(key);
        ProcessRepresentation representation = ProcessRepresentation.fromProcess(process);
        return Response.ok(representation).build();
    }

    @GET
    @Path("/processes/{key}/trace")
    @GZIP
    public Response readProcessTrace(@PathParam("key") String key) throws RuntimeServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /runtime/processes/" + key + "/trace");
        File trace = runtime.readProcessTrace(key);
        return Response.ok(trace).header("Content-Type", "text/plain").header("Content-Length", trace.length()).header("Accept-Ranges", "bytes").build();
    }
    
    @GET
    @Path("/processes/{key}/variables")
    @GZIP
    public Response readProcessVariables(@PathParam("key") String key) throws RuntimeServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /runtime/processes/" + key + "/variables");
        Map<String, Object> vars = runtime.listProcessVariables(key);
        List<ProcessVariableRepresentation> variables = new ArrayList<ProcessVariableRepresentation> ();
        for ( Entry<String, Object> entry : vars.entrySet() ) {
            variables.add(ProcessVariableRepresentation.fromObject(entry.getKey(), entry.getValue()));
        }
        return Response.ok(variables).build();
    }

    @GET
    @Path("/tasks")
    @GZIP
    public Response listCandidateTasks() throws RuntimeServiceException, AccessDeniedException, MembershipServiceException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /runtime/tasks");
        List<HumanTask> ctasks = runtime.listCandidateTasks();
        List<HumanTask> atasks = runtime.listAssignedTasks();
        GenericCollectionRepresentation<HumanTaskRepresentation> representation = new GenericCollectionRepresentation<>();
        for (HumanTask task : ctasks) {
            representation.addEntry(HumanTaskRepresentation.fromHumanTask(task));
        }
        for (HumanTask task : atasks) {
            representation.addEntry(HumanTaskRepresentation.fromHumanTask(task));
        }
        representation.setOffset(0);
        representation.setSize(ctasks.size()+atasks.size());
        representation.setLimit(ctasks.size()+atasks.size());
        return Response.ok(representation).build();
    }

    @POST
    @Path("/tasks/{id}")
    @Consumes( MediaType.APPLICATION_JSON)
    public Response performTaskAction(@PathParam("id") String id, ProcessTaskActionRepresentation action) throws RuntimeServiceException {
        LOGGER.log(Level.INFO, "POST /runtime/tasks");
        switch (action.getAction()) {
        case "claim":
            runtime.claimTask(id);
            break;
        case "unclaim":
            runtime.unclaimTask(id);
            break;
        case "complete":
            Map<String, Object> params = new HashMap<String, Object>();
            for (ProcessVariableRepresentation variable : action.getVariables()) {
                params.put(variable.getName(), variable.getTypedValue());
            }
            runtime.completeTask(id, params);
            break;
        default:
            return Response.status(Status.BAD_REQUEST).entity("action unavailable").build();
        }
        return Response.ok().build();
    }

}
