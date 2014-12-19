package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.el.FixedValue;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class HelloWorldTask extends RuntimeEngineTask {
	
	public static final String NAME = "HelloWorld";

	private static final Logger logger = Logger.getLogger(HelloWorldTask.class.getName());

	private FixedValue delay = null;

	public HelloWorldTask() {
	}

	public FixedValue getDelay() {
		return delay;
	}

	public void setDelay(FixedValue delay) {
		this.delay = delay;
	}
	
	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		String name = (String) execution.getVariable("name");
		if (delay != null) {
			logger.log(Level.INFO, "sleeping a while...");
			try {
				Thread.sleep(Integer.parseInt(delay.getExpressionText()));
			} catch (NumberFormatException | InterruptedException e) {
				logger.log(Level.WARNING, "error while waiting...", e);
			}
		}
		logger.log(Level.INFO, "Hello " + name);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Hello " + name));
		execution.setVariable("greettime", new Date());
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}
