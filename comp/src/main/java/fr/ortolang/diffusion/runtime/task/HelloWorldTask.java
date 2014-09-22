package fr.ortolang.diffusion.runtime.task;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HelloWorldTask extends Task {
	
	private Logger logger = Logger.getLogger(HelloWorldTask.class.getName());
	
	public HelloWorldTask() {
		super();
	}
	
	@Override
	public TaskState execute() {
		try {
			this.log("Starting hello world task");
			logger.log(Level.INFO, "Hello World !!");
			this.log("Hello World !!");
			this.log("Hello World task ended");
			return TaskState.COMPLETED;
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "error during task execution" + e);
			this.log("unexpected error occured during task execution: " + e.getMessage() + ", see server log for further details");
			return TaskState.ERROR;
		}
	}

}
