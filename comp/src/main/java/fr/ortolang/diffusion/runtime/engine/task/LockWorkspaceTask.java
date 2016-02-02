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

import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.Status;

import fr.ortolang.diffusion.notification.NotificationServiceException;
import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class LockWorkspaceTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(LockWorkspaceTask.class.getName());
    public static final String NAME = "Lock Workspace";

    public LockWorkspaceTask() {
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
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
            LOGGER.log(Level.FINE, "locking workspace with key: " + wskey);
            getCoreService().systemSetWorkspaceReadOnly(wskey, true);
        } catch (SecurityException | IllegalStateException | EJBTransactionRolledbackException | CoreServiceException | KeyNotFoundException | NotificationServiceException e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occurred: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unexpected error occurred", e);
        }

        try {
            LOGGER.log(Level.FINE, "COMMIT Active User Transaction.");
            getUserTransaction().commit();
            getUserTransaction().begin();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
