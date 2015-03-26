package fr.ortolang.diffusion.runtime.engine;

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

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.publication.PublicationService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public abstract class RuntimeEngineTask implements JavaDelegate {

	public static final String PRETEND = "pretend";
	
	public static final String BAG_PATH_PARAM_NAME = "bagpath";
	public static final String BAG_VERSIONS_PARAM_NAME = "bagversions";
	public static final String BAG_VERSION_PARAM_NAME = "bagversion";
	
	public static final String IMPORT_OPERATIONS_PARAM_NAME = "operations";

	public static final String ROOT_COLLECTION_PARAM_NAME = "root";
	public static final String SNAPSHOT_NAME_PARAM_NAME = "snapshot";

	public static final String WORKSPACE_KEY_PARAM_NAME = "wskey";
	public static final String WORKSPACE_NAME_PARAM_NAME = "wsname";
	public static final String WORKSPACE_TYPE_PARAM_NAME = "wstype";
	public static final String WORKSPACE_ALIAS_PARAM_NAME = "wsalias";

	private static final Logger LOGGER = Logger.getLogger(RuntimeEngineTask.class.getName());

	protected UserTransaction userTx;
	protected RuntimeEngine engine;
	protected RuntimeService runtime;
	protected MembershipService membership;
	protected BinaryStoreService store;
	protected CoreService core;
	protected BrowserService browser;
	protected RegistryService registry;
	protected PublicationService publication;

	public MembershipService getMembershipService() throws RuntimeEngineTaskException {
		try {
			if (membership == null) {
				membership = (MembershipService) OrtolangServiceLocator.findService(MembershipService.SERVICE_NAME);
			}
			return membership;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public BinaryStoreService getBinaryStore() throws RuntimeEngineTaskException {
		try {
			if (store == null) {
				store = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME);
			}
			return store;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public CoreService getCoreService() throws RuntimeEngineTaskException {
		try {
			if (core == null) {
				core = (CoreService) OrtolangServiceLocator.findService(CoreService.SERVICE_NAME);
			}
			return core;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public RuntimeService getRuntimeService() throws RuntimeEngineTaskException {
		try {
			if (runtime == null) {
				runtime = (RuntimeService) OrtolangServiceLocator.findService(RuntimeService.SERVICE_NAME);
			}
			return runtime;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public BrowserService getBrowserService() throws RuntimeEngineTaskException {
		try {
			if (browser == null) {
				browser = (BrowserService) OrtolangServiceLocator.findService(BrowserService.SERVICE_NAME);
			}
			return browser;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public RegistryService getRegistryService() throws RuntimeEngineTaskException {
		try {
			if (registry == null) {
				registry = (RegistryService) OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME);
			}
			return registry;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public PublicationService getPublicationService() throws RuntimeEngineTaskException {
		try {
			if (publication == null) {
				publication = (PublicationService) OrtolangServiceLocator.findService("publication");
			}
			return publication;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	public UserTransaction getUserTransaction() throws RuntimeEngineTaskException {
		try {
			if (userTx == null) {
				userTx = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
			}
			return userTx;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	private RuntimeEngine getRuntimeEngine() throws RuntimeEngineTaskException {
		try {
			if (engine == null) {
				engine = (RuntimeEngine) OrtolangServiceLocator.lookup(RuntimeEngine.SERVICE_NAME);
			}
			return engine;
		} catch (Exception e) {
			throw new RuntimeEngineTaskException(e);
		}
	}

	@Override
	public void execute(DelegateExecution execution) {
		try {
			LOGGER.log(Level.INFO, "Starting RuntimeTask execution");
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityStartEvent(execution.getProcessBusinessKey(), getTaskName(), "* SERVICE TASK " + execution.getCurrentActivityName() + " STARTED"));

			try {
				LOGGER.log(Level.FINE, "Executing task");
				executeTask(execution);
				LOGGER.log(Level.FINE, "Task executed");
			} catch (RuntimeEngineTaskException e) {
				LOGGER.log(Level.SEVERE, "RuntimeTask exception intercepted, putting task in error and aborting process", e);
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityErrorEvent(execution.getProcessBusinessKey(), getTaskName(), "* SERVICE TASK " + execution.getCurrentActivityName() + " IN ERROR: " + e.getMessage()));
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessAbortEvent(execution.getProcessBusinessKey(), e.getMessage()));
				throw new BpmnError("RuntimeTaskExecutionError", e.getMessage());
			}

			LOGGER.log(Level.FINE, "Sending events of process evolution");
			throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityCompleteEvent(execution.getProcessBusinessKey(), getTaskName(), "* SERVICE TASK " + execution.getCurrentActivityName() + " COMPLETED"));
		} catch (RuntimeEngineTaskException e) {
			LOGGER.log(Level.SEVERE, "Unexpected runtime task exception", e);
			throw new BpmnError("Unexpected RuntimeTaskExecutionError", e.getMessage());
		}
	}

	public void throwRuntimeEngineEvent(RuntimeEngineEvent event) throws RuntimeEngineTaskException {
		try {
			getRuntimeEngine().notify(event);
		} catch (RuntimeEngineException e) {
			throw new RuntimeEngineTaskException("unexpected error while trying to throw event", e);
		}
	}

	public abstract String getTaskName();

	public abstract void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException;

}
