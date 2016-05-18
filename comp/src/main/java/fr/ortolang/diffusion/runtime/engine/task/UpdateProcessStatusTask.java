package fr.ortolang.diffusion.runtime.engine.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class UpdateProcessStatusTask extends RuntimeEngineTask {
    
    private static final Logger LOGGER = Logger.getLogger(UpdateProcessStatusTask.class.getName());
    public static final String NAME = "Update Process Status";
    
    private Expression status;
    private Expression explanation;
    
    public UpdateProcessStatusTask() {
    }
    
    public Expression getStatus() {
        return status;
    }

    public void setStatus(Expression status) {
        this.status = status;
    }
    
    public Expression getExplanation() {
        return explanation;
    }

    public void setExplanation(Expression explanation) {
        this.explanation = explanation;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        LOGGER.log(Level.FINE, "sending update process status event");
        throwRuntimeEngineEvent(RuntimeEngineEvent.createUpdateProcessStatusEvent(execution.getProcessBusinessKey(), (String) status.getValue(execution), (String) explanation.getValue(execution)));
        LOGGER.log(Level.FINE, "update envent sent");
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}