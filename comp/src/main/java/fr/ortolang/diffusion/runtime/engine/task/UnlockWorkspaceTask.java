package fr.ortolang.diffusion.runtime.engine.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBTransactionRolledbackException;
import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class UnlockWorkspaceTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(UnlockWorkspaceTask.class.getName());
    public static final String NAME = "Unlock Workspace";

    public UnlockWorkspaceTask() {
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
            LOGGER.log(Level.FINE, "unlocking workspace with key: " + wskey);
            getCoreService().systemSetWorkspaceReadOnly(wskey, false);
        } catch (SecurityException | IllegalStateException | EJBTransactionRolledbackException | CoreServiceException | KeyNotFoundException e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occured: " + e.getMessage()));
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
