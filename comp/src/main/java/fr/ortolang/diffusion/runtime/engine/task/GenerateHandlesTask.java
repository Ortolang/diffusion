package fr.ortolang.diffusion.runtime.engine.task;

import java.util.HashSet;
import java.util.Iterator;

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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectPid;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.MetadataSource;
import fr.ortolang.diffusion.core.entity.TagElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

public class GenerateHandlesTask extends RuntimeEngineTask {

    public static final String NAME = "Generate Handles";

    private static final Logger LOGGER = Logger.getLogger(GenerateHandlesTask.class.getName());

    private Expression wskey;
    private Expression snapshot;

    public GenerateHandlesTask() {
    }

    public Expression getWskey() {
        return wskey;
    }

    public void setWskey(Expression wskey) {
        this.wskey = wskey;
    }

    public Expression getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(Expression snapshot) {
        this.snapshot = snapshot;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {

        String wskey = (String) getWskey().getValue(execution);
        String snapshot = (String) getSnapshot().getValue(execution);
        StringBuilder report = new StringBuilder();
        try {
            if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                LOGGER.log(Level.FINE, "starting new user transaction.");
                getUserTransaction().begin();
                report.append("[BEGIN-TRAN]\r\n");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
        }
        
        try {
            LOGGER.log(Level.FINE, "building handles list...");
            Workspace workspace = getCoreService().readWorkspace(wskey);
            TagElement te = workspace.findTagBySnapshot(snapshot);
            LOGGER.log(Level.INFO, "[GenerateHandlesTask] Generating PID list");
            Set<OrtolangObjectPid> pids = buildWorkspacePidList(wskey, te.getName());
            LOGGER.log(Level.FINE, "handles list built with: " + pids.size() + " entries");
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "handles list built with: " + pids.size() + " entries"));

            LOGGER.log(Level.FINE, "starting handle creation...");
            LOGGER.log(Level.INFO, "[GenerateHandlesTask] Count of handle to import : " + pids.size());
            long tscommit = System.currentTimeMillis();
            long pidCount = 0;
            for (OrtolangObjectPid pid : pids) {
            	pidCount++;
                try {
                    getHandleStore().recordHandle(pid.getName(), pid.getKey(), pid.getTarget());
                    report.append(pid).append(" OK\r\n");
                } catch (Exception e) {
                    throw new RuntimeEngineTaskException("unexpected error during publish task execution", e);
                }
                if (pidCount%500 == 0) {
                	LOGGER.log(Level.INFO, "[GenerateHandlesTask] Count of object imported : " + pidCount);
                }
                try {
                    if (System.currentTimeMillis() - tscommit > 30000 && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                        report.append("[COMMIT-TRAN]\r\n");
                        LOGGER.log(Level.FINE, "committing active user transaction.");
                        LOGGER.log(Level.INFO, "[GenerateHandlesTask] Total of pid imported : " + pidCount);
                        getUserTransaction().commit();
                        tscommit = System.currentTimeMillis();
                        getUserTransaction().begin();
                        report.append("[BEGIN-TRAN]\r\n");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
                }
            }
            try {
                LOGGER.log(Level.FINE, "committing active user transaction and starting new one.");
                LOGGER.log(Level.INFO, "[GenerateHandlesTask] All pid imported");
                getUserTransaction().commit();
                report.append("[COMMIT-TRAN]\r\n");
                getUserTransaction().begin();
                report.append("[BEGIN-TRAN]\r\n");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
            }
            LOGGER.log(Level.FINE, "handles created");
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "handle generation done"));
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Handle Generation report : \r\n" + report.toString(), null));
        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to generate handles: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unable to generate handles", e);
        } finally {
            getExtractionServiceWorker().start();
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }
    
    public Set<OrtolangObjectPid> buildWorkspacePidList(String wskey, String tag) throws RuntimeEngineTaskException {
        LOGGER.log(Level.FINE, "building pid list for workspace [" + wskey + "]");
        try {
            Workspace workspace = getCoreService().readWorkspace(wskey);
            if (workspace == null) {
                throw new CoreServiceException(
                        "unable to load workspace with key [" + wskey + "] from storage");
            }
            if (!workspace.containsTagName(tag)) {
                throw new CoreServiceException(
                        "the workspace with key: " + wskey + " does not containt a tag with name: " + tag);
            }
            String snapshot = workspace.findTagByName(tag).getSnapshot();
            String root = workspace.findSnapshotByName(snapshot).getKey();

            long tscommit = System.currentTimeMillis();
            Set<OrtolangObjectPid> pids = new HashSet<OrtolangObjectPid>();
            String apiUrlBase = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_SSL)
                    + OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_PATH_CONTENT);
            String marketUrlBase = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.MARKET_SERVER_URL)
                    + "market/item";
            buildHandleList(workspace.getAlias(), tag, root, pids, PathBuilder.newInstance(), apiUrlBase,
                    marketUrlBase, tscommit);
            return pids;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to build workspace pid list of workspace" + wskey, e);
            throw new RuntimeEngineTaskException("unable to build workspace pid list of workspace " + wskey +" : " + e.getMessage(), e);
        }
    }

    private void buildHandleList(String wsalias, String tag, String key, Set<OrtolangObjectPid> pids, PathBuilder path,
            String apiUrlBase, String marketUrlBase, long tscommit)
            throws RuntimeEngineTaskException {
        try {
            OrtolangObject object = getCoreService().findObject(key);
            LOGGER.log(Level.FINE, "Generating pid for key: " + key);
            String target = ((path.isRoot()) ? marketUrlBase : apiUrlBase) + "/" + wsalias + "/" + tag
                    + ((path.isRoot()) ? "" : path.build());
            String dynHandle = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX) + "/"
                    + wsalias + ((path.isRoot()) ? "" : path.build());
            String staticHandle = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX) + "/"
                    + wsalias + "/" + tag + ((path.isRoot()) ? "" : path.build());
            OrtolangObjectPid dpid = new OrtolangObjectPid(OrtolangObjectPid.Type.HANDLE, dynHandle, key, target,
                    false);
            boolean adddpid = true;
            for (OrtolangObjectPid pid : pids) {
                if (pid.getName().equals(dpid.getName()) && pid.isUserbased()) {
                    adddpid = false;
                    break;
                }
            }
            if (adddpid) {
                pids.add(dpid);
            }
            OrtolangObjectPid spid = new OrtolangObjectPid(OrtolangObjectPid.Type.HANDLE, staticHandle, key, target,
                    false);
            boolean addspid = true;
            for (OrtolangObjectPid pid : pids) {
                if (pid.getName().equals(spid.getName()) && pid.isUserbased()) {
                    addspid = false;
                    break;
                }
            }
            if (addspid) {
                pids.add(spid);
            }
            if (object instanceof MetadataSource) {
                MetadataElement mde = ((MetadataSource) object).findMetadataByName(MetadataFormat.PID);
                if (mde != null) {
                    LOGGER.log(Level.FINE, "PID metadata found, load json and generate corresponding pids");
                    MetadataObject md = getCoreService().readMetadataObject(mde.getKey());
                    try {
                        JsonReader reader = Json.createReader(getBinaryStore().get(md.getStream()));
                        JsonObject json = reader.readObject();
                        if (json.containsKey("pids")) {
                            JsonArray jpids = json.getJsonArray("pids");
                            for (int i = 0; i < jpids.size(); i++) {
                                JsonObject jpid = jpids.getJsonObject(i);
                                LOGGER.log(Level.FINE, "Generating metadata based pid for key: " + key);
                                String ctarget = ((path.isRoot()) ? marketUrlBase : apiUrlBase) + "/" + wsalias + "/"
                                        + tag + ((path.isRoot()) ? "" : path.build());
                                OrtolangObjectPid upid = new OrtolangObjectPid(OrtolangObjectPid.Type.HANDLE,
                                        jpid.getString("value"), key, ctarget, true);
                                Iterator<OrtolangObjectPid> iter = pids.iterator();
                                while (iter.hasNext()) {
                                    OrtolangObjectPid pid = iter.next();
                                    if (pid.getName().equals(upid.getName())) {
                                        iter.remove();
                                    }
                                }
                                pids.add(upid);
                            }
                        }
                        reader.close();
                    } catch (BinaryStoreServiceException | DataNotFoundException e) {
                        LOGGER.log(Level.SEVERE, "unable to read pid metadata", e);
                    }
                }
            }
            if (object instanceof Collection) {
                for (CollectionElement element : ((Collection) object).getElements()) {
                    buildHandleList(wsalias, tag, element.getKey(), pids, path.clone().path(element.getName()),
                            apiUrlBase, marketUrlBase, tscommit);
                    try {
                    	if (System.currentTimeMillis() - tscommit > 30000 && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                    		LOGGER.log(Level.FINE, "committing active user transaction.");
                    		LOGGER.log(Level.INFO, "[GenerateHandlesTask > buildHandleList] Intermediate commit of user transaction");
                    		getUserTransaction().commit();
                    		tscommit = System.currentTimeMillis();
                    		getUserTransaction().begin();
                    	}
                    } catch (Exception e) {
                    	LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to build workspace pid list of workspace" + wskey, e);
            throw new RuntimeEngineTaskException("unable to build workspace pid list of workspace " + wskey +" : " + e.getMessage(), e);
        }
    }
}
