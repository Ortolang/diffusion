package fr.ortolang.diffusion.runtime.engine.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBTransactionRolledbackException;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.core.AliasNotFoundException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class LockWorkspaceTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(LockWorkspaceTask.class.getName());
    public static final String NAME = "Lock Workspace";

    public static final String ACTION_LOCK = "lock";
    public static final String ACTION_UNLOCK = "unlock";

    private Expression action;

    public LockWorkspaceTask() {
    }

    public Expression getAction() {
        return action;
    }

    public void setAction(Expression action) {
        this.action = action;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        String wskey;
        if (execution.hasVariable(WORKSPACE_ALIAS_PARAM_NAME)) {
            String wsalias = execution.getVariable(WORKSPACE_ALIAS_PARAM_NAME, String.class);
            try {
                wskey = getCoreService().resolveWorkspaceAlias(wsalias);
            } catch (CoreServiceException | AccessDeniedException | AliasNotFoundException e) {
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(),
                        "Unexpected error occurred while resolving workspace alias: " + wsalias + " " + e.getMessage()));
                throw new RuntimeEngineTaskException("unexpected error occurred while resolving workspace alias: " + wsalias, e);
            }
        } else {
            wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
        }
        
        try {
            if (((String)action.getValue(execution)).equals(ACTION_LOCK)) {
                LOGGER.log(Level.FINE, "locking workspace with key: " + wskey);
                getCoreService().systemSetWorkspaceReadOnly(wskey, true);
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace locked"));
            } else if (((String)action.getValue(execution)).equals(ACTION_UNLOCK)) {
                LOGGER.log(Level.FINE, "unlocking workspace with key: " + wskey);
                getCoreService().systemSetWorkspaceReadOnly(wskey, false);
            } else {
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unable to perform action [" + action + "], only lock or unlock are allowed"));
                throw new RuntimeEngineTaskException("Unable to perform action [" + action + "], only lock or unlock are allowed");
            }
        } catch (SecurityException | IllegalStateException | EJBTransactionRolledbackException | CoreServiceException | KeyNotFoundException e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Unexpected error occured: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unexpected error occurred", e);
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
