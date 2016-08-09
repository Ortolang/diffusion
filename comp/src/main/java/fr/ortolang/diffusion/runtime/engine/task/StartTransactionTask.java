package fr.ortolang.diffusion.runtime.engine.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class StartTransactionTask extends RuntimeEngineTask {

    public static final String NAME = "Commit";

    private static final Logger LOGGER = Logger.getLogger(StartTransactionTask.class.getName());

    public StartTransactionTask() {
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        try {
            if (getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                LOGGER.log(Level.FINE, "Active transaction already exists, commit first");
                getUserTransaction().commit();
            }

            if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                LOGGER.log(Level.FINE, "Start new transaction");
                getUserTransaction().begin();
            }

        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to start transaction: " + e.getMessage()));
            throw new RuntimeEngineTaskException("error while starting transaction", e);
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
