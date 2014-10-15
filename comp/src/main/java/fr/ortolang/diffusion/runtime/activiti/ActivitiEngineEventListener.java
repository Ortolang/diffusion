package fr.ortolang.diffusion.runtime.activiti;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;

public class ActivitiEngineEventListener implements ActivitiEventListener {

	private Logger logger = Logger.getLogger(ActivitiEngineEventListener.class.getName());

	@Override
	public void onEvent(ActivitiEvent event) {
		switch (event.getType()) {

		case JOB_EXECUTION_SUCCESS:
			logger.log(Level.INFO, "A job well done!");
			break;

		case JOB_EXECUTION_FAILURE:
			logger.log(Level.INFO, "A job has failed!");
			break;

		default:
			logger.log(Level.INFO, "Event received: " + event.getType());
		}
	}

	@Override
	public boolean isFailOnException() {
		return false;
	}
}