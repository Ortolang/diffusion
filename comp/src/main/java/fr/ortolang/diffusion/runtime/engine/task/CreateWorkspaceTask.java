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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class CreateWorkspaceTask extends RuntimeEngineTask {
	
	public static final String NAME = "Create Workspace";
	
	private static final Logger logger = Logger.getLogger(CreateWorkspaceTask.class.getName());

	public CreateWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(WORKSPACE_KEY_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		if ( !execution.hasVariable(WORKSPACE_NAME_PARAM_NAME) ) {
			execution.setVariable(WORKSPACE_NAME_PARAM_NAME, wskey);
		}
		String wsname = execution.getVariable(WORKSPACE_NAME_PARAM_NAME, String.class);
		if ( !execution.hasVariable(WORKSPACE_TYPE_PARAM_NAME) ) {
			execution.setVariable(WORKSPACE_TYPE_PARAM_NAME, "unknown");
		}
		String wstype = execution.getVariable(WORKSPACE_TYPE_PARAM_NAME, String.class);
		
		logger.log(Level.INFO, "Creating workspace");
		try {
			OrtolangObjectIdentifier wsidentifier = getRegistryService().lookup(wskey);
			if ( !wsidentifier.getService().equals(CoreService.SERVICE_NAME) && !wsidentifier.getType().equals(Workspace.OBJECT_TYPE) ) {
				logger.log(Level.SEVERE, "Workspace Key already exists but is not a workspace !!");
				throw new RuntimeEngineTaskException("Workspace Key already exists but is NOT a workspace !!");
			}
		} catch ( KeyNotFoundException e ) {
			try {
				getCoreService().createWorkspace(wskey, wsname, wstype);
			} catch ( Exception e2 ) {
				logger.log(Level.SEVERE, "unable to create workspace", e2);
				throw new RuntimeEngineTaskException("unable to create workspace", e2);
			}
		} catch (RegistryServiceException e) {
			throw new RuntimeEngineTaskException("unable to create workspace", e);
		}
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace created with key: " + wskey));
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}
