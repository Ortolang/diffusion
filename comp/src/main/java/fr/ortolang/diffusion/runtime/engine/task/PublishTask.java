package fr.ortolang.diffusion.runtime.engine.task;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangEvent.ArgumentsBuilder;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.publication.PublicationService;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class PublishTask extends RuntimeEngineTask {

    public static final String NAME = "Publish";

    private static final Logger LOGGER = Logger.getLogger(PublishTask.class.getName());

    private Expression wskey;
    private Expression snapshot;

    public PublishTask() {
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

        LOGGER.log(Level.FINE, "starting publication...");
        String caller = getMembershipService().getProfileKeyForConnectedIdentifier();
        
        LOGGER.log(Level.FINE, "building publication map...");
        Map<String, Map<String, List<String>>> map;
        try {
            map = getCoreService().buildWorkspacePublicationMap(wskey, snapshot);
        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to build workspace publication map: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unable to build workspace publication map", e);
        }
        
        try {
            if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                LOGGER.log(Level.FINE, "starting new user transaction.");
                getUserTransaction().begin();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
        }
        try {
            long tscommit = System.currentTimeMillis();
            //TODO log event or status to set process progression 
            for (Entry<String, Map<String, List<String>>> entry : map.entrySet()) {
            	try {
            		getPublicationService().publishKey(entry.getKey(), entry.getValue());
	            } catch (Exception e) {
	                throw new RuntimeEngineTaskException("unexpected error during publish task execution", e);
	            }
            	try {
                    if (System.currentTimeMillis() - tscommit > 30000 && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                        LOGGER.log(Level.FINE, "committing active user transaction.");
                        getUserTransaction().commit();
                        tscommit = System.currentTimeMillis();
                        getUserTransaction().begin();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
                }
            }

            try {
                LOGGER.log(Level.FINE, "committing active user transaction and starting new one.");
                LOGGER.log(Level.INFO, "[PublishTask] All object imported");
                getUserTransaction().commit();
                getUserTransaction().begin();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
            }
            LOGGER.log(Level.FINE, "publication done.");

            ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("snapshot", snapshot);
            getNotificationService().throwEvent(wskey, caller, Workspace.OBJECT_TYPE, 
                    OrtolangEvent.buildEventType(PublicationService.SERVICE_NAME, Workspace.OBJECT_TYPE, "publish-snapshot"), argumentsBuilder.build());
            
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "All elements published succesfully"));
        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to publish elements: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unable to publish elements", e);
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }
    
    @Override
    public int getTransactionTimeout() {
        return 5000;
    }

}
