package fr.ortolang.diffusion.runtime.engine;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;

public abstract class TransactionnalRuntimeEngineTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(TransactionnalRuntimeEngineTask.class.getName());

    @Override
    public void execute(DelegateExecution execution) {

        String bkey = execution.getProcessBusinessKey();
        String aname = execution.getCurrentActivityName();

        try {
            LOGGER.log(Level.INFO, "Starting " + this.getTaskName() + " execution");
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityStartEvent(bkey, getTaskName(), "SERVICE TASK " + aname + " STARTED"));

            try {
                if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                    LOGGER.log(Level.FINE, "Begin transaction for task " + this.getTaskName());
                    getUserTransaction().begin();
                    if (getTransactionTimeout() > 0) {
                        getUserTransaction().setTransactionTimeout(getTransactionTimeout());
                        LOGGER.log(Level.FINE, "Timeout set to: " + getTransactionTimeout());
                    }
                } else if (getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                    LOGGER.log(Level.FINE, "Transaction already active for task " + this.getTaskName());
                    if (getTransactionTimeout() > 0) {
                        getUserTransaction().setTransactionTimeout(getTransactionTimeout());
                        LOGGER.log(Level.FINE, "Timeout set to: " + getTransactionTimeout());
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Unable to begin transaction, bad status: " + getUserTransaction().getStatus());
                    throw new RuntimeEngineTaskException("Unable to begin transaction, bad status: " + getUserTransaction().getStatus());
                }

                try {
                    LOGGER.log(Level.FINE, "Starting task execution");
                    executeTask(execution);
                    LOGGER.log(Level.FINE, "Task executed");
                } catch (RuntimeEngineTaskException e) {
                    LOGGER.log(Level.INFO, "RuntimeEngineException: " + e.getMessage() + " , need to rollback.");
                    LOGGER.log(Level.FINE, "Rollback transaction for task " + this.getTaskName());
                    getUserTransaction().rollback();
                    throw e;
                }

                LOGGER.log(Level.FINE, "Commit transaction for task " + this.getTaskName());
                getUserTransaction().commit();
                LOGGER.log(Level.FINE, "Begin a new transaction to avoid Null Pointer Exception " + this.getTaskName());
                getUserTransaction().begin();
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error occured during task execution, putting task in error and aborting process with pid: " + bkey, e);
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(bkey, "SERVICE TASK " + aname + " IN ERROR: " + e.getMessage(), e));
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityErrorEvent(bkey, getTaskName(), "SERVICE TASK " + aname + " IN ERROR: " + e.getMessage()));
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessAbortEvent(bkey, e.getMessage()));
                throw new BpmnError("RuntimeTaskExecutionError", e.getMessage());
            }

            LOGGER.log(Level.FINE, "Sending events of process evolution");
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessActivityCompleteEvent(bkey, getTaskName(), "SERVICE TASK " + aname + " COMPLETED"));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected runtime task exception", e);
            throw new BpmnError("RuntimeTaskExecutionError", e.getMessage());
        }
    }

    public int getTransactionTimeout() {
        return -1;
    }

}
