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
import javax.transaction.SystemException;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.AliasNotFoundException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.WorkspaceReadOnlyException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.core.entity.WorkspaceType;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class DeleteWorkspaceTask extends RuntimeEngineTask {
	
	private static final Logger LOGGER = Logger.getLogger(DeleteWorkspaceTask.class.getName());
	public static final String NAME = "Delete Workspace";
	
	public DeleteWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		String wskey;
		String wsalias = null;
		if (execution.hasVariable(WORKSPACE_ALIAS_PARAM_NAME)) {
			wsalias = execution.getVariable(WORKSPACE_ALIAS_PARAM_NAME, String.class);
			try {
				wskey = getCoreService().resolveWorkspaceAlias(wsalias);
			} catch (CoreServiceException | AccessDeniedException | AliasNotFoundException e) {
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occurred while resolving workspace alias: " + wsalias + " " + e.getMessage()));
				throw new RuntimeEngineTaskException("unexpected error occurred while resolving workspace alias: " + wsalias, e);
			}
		} else {
	        wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		}
		if ( !execution.hasVariable(WORKSPACE_NAME_PARAM_NAME) ) {
			execution.setVariable(WORKSPACE_NAME_PARAM_NAME, wskey);
		}
		boolean force = false;
		if ( execution.hasVariable(FORCE_PARAM_NAME) ) {
            force = Boolean.parseBoolean(execution.getVariable(FORCE_PARAM_NAME, String.class));
        }
		
		try {
			try {
				getUserTransaction().setTransactionTimeout(2000);
				LOGGER.log(Level.FINE, "Reading workspace");
		        Workspace workspace = getCoreService().readWorkspace(wskey);
				if ( workspace.getType().equals(WorkspaceType.SYSTEM.toString())) {
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace [" + ((wsalias != null)?wsalias:wskey) + "] is a SYSTEM workspace and it is forbidden to delete it"));
					throw new RuntimeEngineTaskException("deleting a system workspace is forbidden");
				}
				LOGGER.log(Level.FINE, "Listing workspace [" + ((wsalias != null)?wsalias:wskey) + "] keys");
				Set<String> keys = getCoreService().systemListWorkspaceKeys(wskey);
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace [" + ((wsalias != null)?wsalias:wskey) + "] content retrieved"));
				StringBuilder trace = new StringBuilder();
				getCoreService().deleteWorkspace(wskey, force);
                for ( String key : keys ) {
					LOGGER.log(Level.FINE, "Deleting content key: " + key);
					getRegistryService().delete(key, true);
					getIndexingService().remove(key);
					trace.append("key [").append(key).append("] deleted and removed from index");
				}
				trace.append("workspace with key [").append(wskey).append("] deleted");
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace [" + ((wsalias != null)?wsalias:wskey) + "] deleted"));
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), trace.toString(), null));
			} catch (KeyNotFoundException | IndexingServiceException | AccessDeniedException | CoreServiceException | RegistryServiceException | KeyLockedException | WorkspaceReadOnlyException e) {
				getUserTransaction().rollback();
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occured: " + e.getMessage()));
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "unexpected error during delete workspace [" + ((wsalias != null)?wsalias:wskey) + "] task", e));
				throw new RuntimeEngineTaskException("unexpected error during delete workspace [" + ((wsalias != null)?wsalias:wskey) + "] task", e);
			} 
		} catch (SystemException | SecurityException | IllegalStateException  | EJBTransactionRolledbackException  e) {
		    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occured: " + e.getMessage()));
			throw new RuntimeEngineTaskException("unexpected error occurred", e);
		}
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}