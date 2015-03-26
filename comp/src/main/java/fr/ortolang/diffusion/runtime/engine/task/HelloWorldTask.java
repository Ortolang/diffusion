package fr.ortolang.diffusion.runtime.engine.task;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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

	private static final Logger LOGGER = Logger.getLogger(HelloWorldTask.class.getName());

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
			LOGGER.log(Level.INFO, "sleeping a while...");
			try {
				Thread.sleep(Integer.parseInt(delay.getExpressionText()));
			} catch (NumberFormatException | InterruptedException e) {
				LOGGER.log(Level.WARNING, "error while waiting...", e);
			}
		}
		LOGGER.log(Level.INFO, "Hello " + name);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Hello " + name));
		execution.setVariable("greettime", new Date());
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}
