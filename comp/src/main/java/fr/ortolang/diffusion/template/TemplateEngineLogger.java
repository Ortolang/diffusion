package fr.ortolang.diffusion.template;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;

public class TemplateEngineLogger implements LogChute {
	
	private Logger logger = Logger.getLogger(TemplateEngineLogger.class.getName());

	public TemplateEngineLogger() {
	}

	@Override
	public void init(RuntimeServices rs) throws Exception {
	}

	@Override
	public void log(int level, String message) {
		logger.log(Level.FINE, message);
	}

	@Override
	public void log(int level, String message, Throwable t) {
		logger.log(Level.FINE, message, t);
	}

	@Override
	public boolean isLevelEnabled(int level) {
		return true;
	}

}
