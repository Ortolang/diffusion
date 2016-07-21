package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class LogTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(LogTask.class.getName());

    public static final String NAME = "Log";

    private Expression message;

    public LogTask() {
    }

    public Expression getMessage() {
        return message;
    }

    public void setMessage(Expression message) {
        this.message = message;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        String message = (String) getMessage().getValue(execution);
        LOGGER.log(Level.INFO, "Logging task, message: " + message);
        LOGGER.log(Level.INFO, "parent id: " + execution.getParentId());
        LOGGER.log(Level.INFO, "id: " + execution.getId());
        LOGGER.log(Level.INFO, "process id: " + execution.getProcessInstanceId());
        LOGGER.log(Level.INFO, "procees bk: " + execution.getProcessBusinessKey());
        LOGGER.log(Level.INFO, "activity id: " + execution.getCurrentActivityId());
        LOGGER.log(Level.INFO, "activity name: " + execution.getCurrentActivityName());
        LOGGER.log(Level.INFO, "event name: " + execution.getEventName());
        LOGGER.log(Level.INFO, "super exec id: " + execution.getSuperExecutionId());
        LOGGER.log(Level.INFO, "tenant id: " + execution.getTenantId());
        LOGGER.log(Level.INFO, "variables: ");
        for ( Entry<String, Object> entry : execution.getVariables().entrySet() ) {
            LOGGER.log(Level.INFO, entry.getKey() + ": " + entry.getValue());
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
