package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class LoadTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(LoadTask.class.getName());

    public static final String NAME = "Load Workspace";

    public LoadTask() {
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        LOGGER.log(Level.INFO, "Loading informations required for process execution");
        List<String> reviewers = Arrays.asList(new String [] {"reviewer1", "reviewer2"});
        execution.setVariable("reviewers", reviewers);
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
