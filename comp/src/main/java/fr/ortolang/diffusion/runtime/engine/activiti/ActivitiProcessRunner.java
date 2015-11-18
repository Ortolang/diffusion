package fr.ortolang.diffusion.runtime.engine.activiti;

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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.ActivitiObjectNotFoundException;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineException;

public class ActivitiProcessRunner implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ActivitiProcessRunner.class.getName());

	private ActivitiEngineBean engine;
	private String type;
	private String key;
	private Map<String, Object> variables;
	
	public ActivitiProcessRunner(ActivitiEngineBean engine, String type, String key, Map<String, Object> variables) {
		this.engine = engine;
		this.type = type;
		this.key = key;
		this.variables = variables;
	}
	
	@Override
	public void run() {
		LOGGER.log(Level.INFO, "ActivitiProcessRunner starting new instance of type " + type  + " with pid: " + key);
		try {
			try {
				engine.notify(RuntimeEngineEvent.createProcessStartEvent(key));
				engine.getActivitiRuntimeService().startProcessInstanceByKey(type, key, variables);			
			} catch ( ActivitiObjectNotFoundException e ) {
				LOGGER.log(Level.WARNING, "Error during starting process instance", e);
				engine.notify(RuntimeEngineEvent.createProcessAbortEvent(key, e.getMessage()));
			}
		} catch ( RuntimeEngineException e ) {
			LOGGER.log(Level.WARNING, "unexpected error during process runner execution of pid: " + key, e);
		}
		LOGGER.log(Level.INFO, "ActivitiProcessRunner stopped for pid: " + key);
	}

}
