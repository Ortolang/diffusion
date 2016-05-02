package fr.ortolang.diffusion.runtime.engine.task;

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

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.transaction.Status;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.core.*;
import fr.ortolang.diffusion.core.entity.*;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import org.activiti.engine.impl.util.json.JSONObject;

public class LoadSnapshotTask extends RuntimeEngineTask {

    public static final String NAME = "Snapshot Workspace";
    private static final Logger LOGGER = Logger.getLogger(LoadSnapshotTask.class.getName());

    public LoadSnapshotTask() {
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        checkParameters(execution);
        String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);

        try {
            LOGGER.log(Level.FINE, "User Transaction Status: " + getUserTransaction().getStatus());
            if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                LOGGER.log(Level.FINE, "START User Transaction");
                getUserTransaction().begin();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
        }

        try {
            Workspace workspace = getCoreService().readWorkspace(wskey);
            if ( workspace.getAlias() != null && workspace.getAlias().length() > 0 ) {
                execution.setVariable(WORKSPACE_ALIAS_PARAM_NAME, workspace.getAlias());
            } else {
                execution.setVariable(WORKSPACE_ALIAS_PARAM_NAME, wskey);
            }

            String snapshotName;
            String rootCollection;
            if (!execution.hasVariable(SNAPSHOT_NAME_PARAM_NAME)) {
                LOGGER.log(Level.FINE, "Updating publicationDate and datasize fields in item metadata");
                MetadataElement ortolangItemMetadata = getCoreService().readCollection(workspace.getHead()).findMetadataByName(MetadataFormat.ITEM);
                InputStream metadataInputStream = core.download(ortolangItemMetadata.getKey());
                String json = new BufferedReader(new InputStreamReader(metadataInputStream)).lines().collect(Collectors.joining("\n"));
                metadataInputStream.close();
                JSONObject jsonObject = new JSONObject(json);
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = new Date();
                jsonObject.put("publicationDate", dateFormat.format(date));
                jsonObject.put("datasize", Long.toString(core.getSize(workspace.getHead()).getSize()));
                json = jsonObject.toString();
                String hash = getBinaryStore().put(new ByteArrayInputStream(json.getBytes()));
                core.updateMetadataObject(wskey, "/", MetadataFormat.ITEM, hash, MetadataFormat.ITEM + ".json", false);

                LOGGER.log(Level.FINE, "Snapshot name NOT provided and workspace has changed since last snapshot, generating a new snapshot");
                snapshotName = getCoreService().snapshotWorkspace(wskey);
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "New snapshot [" + snapshotName + "] created"));
                execution.setVariable(SNAPSHOT_NAME_PARAM_NAME, snapshotName);
                try {
                    LOGGER.log(Level.FINE, "User Transaction Status: " + getUserTransaction().getStatus());
                    if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                        LOGGER.log(Level.FINE, "START User Transaction");
                        getUserTransaction().begin();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
                }
            }
            snapshotName = execution.getVariable(SNAPSHOT_NAME_PARAM_NAME, String.class);
            SnapshotElement snapshot = workspace.findSnapshotByName(snapshotName);
            if (snapshot == null) {
                throw new RuntimeEngineTaskException("unable to find a snapshot with name " + snapshotName + " in workspace " + wskey);
            }
            rootCollection = snapshot.getKey();

            String publicationStatus = getRegistryService().getPublicationStatus(rootCollection);
            if (!publicationStatus.equals(OrtolangObjectState.Status.DRAFT.value())) {
                throw new RuntimeEngineTaskException("Snapshot publication status is not " + OrtolangObjectState.Status.DRAFT
                        + ", maybe already published or involved in another publication process");
            }
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Snapshot loaded and publication status is good for publication, starting publication"));

        } catch (CoreServiceException | KeyNotFoundException | RegistryServiceException | WorkspaceReadOnlyException | DataNotFoundException | OrtolangException | DataCollisionException | BinaryStoreServiceException | MetadataFormatException | InvalidPathException | PathNotFoundException | IOException e) {
            throw new RuntimeEngineTaskException("unexpected error during snapshot task execution", e);
        }

        try {
            LOGGER.log(Level.FINE, "COMMIT Active User Transaction.");
            getUserTransaction().commit();
            getUserTransaction().begin();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
        }
    }

    private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
        if (!execution.hasVariable(WORKSPACE_KEY_PARAM_NAME)) {
            throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
