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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.OrtolangObjectPid;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.TagElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class HandleSnapshotTask extends RuntimeEngineTask {

	public static final String NAME = "Generate Handles";
	
	private static final Logger LOGGER = Logger.getLogger(HandleSnapshotTask.class.getName());

	public HandleSnapshotTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(WORKSPACE_KEY_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		if (!execution.hasVariable(SNAPSHOT_NAME_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + SNAPSHOT_NAME_PARAM_NAME + " is not set");
		}
		String snapshot = execution.getVariable(SNAPSHOT_NAME_PARAM_NAME, String.class);
		
		try {
    		try {
    			if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
    				LOGGER.log(Level.FINE, "starting new user transaction.");
    				getUserTransaction().begin();
    			}
    		} catch (Exception e) {
    			LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
    		}
    		
    		Set<OrtolangObjectPid> pids;
    		try {
    			LOGGER.log(Level.FINE, "generating workspace handles...");
    			Workspace workspace = getCoreService().readWorkspace(wskey);
    			TagElement te = workspace.findTagBySnapshot(snapshot);
    			pids = getCoreService().buildWorkspacePidList(wskey, te.getName());
    		} catch (CoreServiceException | AccessDeniedException | KeyNotFoundException e) {
    			throw new RuntimeEngineTaskException("unexpected error while trying to generate handle list", e);
    		}
    		LOGGER.log(Level.FINE, "pids list containing " + pids.size() + " entries");
    		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "pids list built, containing " + pids.size() + " entries"));
    		
    		boolean needcommit;
    		long tscommit = System.currentTimeMillis();
    		StringBuilder report = new StringBuilder();
    		LOGGER.log(Level.FINE, "starting pids creation");
    		for (OrtolangObjectPid pid : pids) {
    			needcommit = false;
    			try {
    				getHandleStore().recordHandle(pid.getName(), pid.getKey(), pid.getTarget());
    				LOGGER.log(Level.FINE, pid + " created");
    				report.append(pid).append(" created\r\n");
    			} catch (Exception e) {
    				LOGGER.log(Level.WARNING, "handle [" + pid.getName() + "] failed to generate: " + e.getMessage());
    				report.append(pid).append(" creation FAILED: ").append(e.getMessage()).append("\r\n");
    			}
    			if ( System.currentTimeMillis() - tscommit > 30000 ) {
    				LOGGER.log(Level.FINE, "current transaction exceed 30sec, need commit.");
    				needcommit = true;
    			}
    			try {
    				if (needcommit && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
    					LOGGER.log(Level.FINE, "commiting active user transaction.");
    					getUserTransaction().commit();
    					tscommit = System.currentTimeMillis();
    					getUserTransaction().begin();
    				}
    			} catch (Exception e) {
    				LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
    			}
    		}
    		try {
    			LOGGER.log(Level.FINE, "commiting active user transaction.");
    			getUserTransaction().commit();
    			getUserTransaction().begin();
    		} catch (Exception e) {
    			LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
    		}
    		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "handle generation done"));
    		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Handle Generation report : \r\n" + report.toString(), null));
    		
		} catch (SecurityException | IllegalStateException  | EJBTransactionRolledbackException e) {
            throw new RuntimeEngineTaskException("unexpected error occured", e);
        }
	}
	
	@Override
	public String getTaskName() {
		return NAME;
	}
	
}
