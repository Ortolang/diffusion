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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.utilities.SimpleResult;

public class CheckBagTask extends RuntimeEngineTask {
	
	public static final String NAME = "Check Bag Integrity";
	
	private static final Logger logger = Logger.getLogger(CheckBagTask.class.getName());

	public CheckBagTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(BAG_PATH_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + BAG_PATH_PARAM_NAME + " is not set");
		}
		
		Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
		if ( !Files.exists(bagpath) ) {
			throw new RuntimeEngineTaskException("bag file " + bagpath + " does not exists");
		}
		
		logger.log(Level.FINE, "loading bag from file: " + bagpath);
		BagFactory factory = new BagFactory();
		Bag bag = factory.createBag(bagpath.toFile());
		logger.log(Level.FINE, "bag loaded: " + bag.getBagItTxt());
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Bag loaded from local file: " + bagpath));
		
		logger.log(Level.FINE, "verifying bag content integrity...");
		long start = System.currentTimeMillis();
		SimpleResult result = bag.verifyPayloadManifests();
		if (!result.isSuccess()) {
			logger.log(Level.WARNING, "bag verification failed: " + result.messagesToString());
			throw new RuntimeEngineTaskException("bag verification failed: " + result.messagesToString());
		}
		long stop = System.currentTimeMillis();
		try {
			bag.close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to close bag", e);
		}
		logger.log(Level.FINE, "bag verification success done in " + (stop - start) + " ms");
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Bag integrity checked successfully from local file: " + bagpath + " in " + (stop - start) + " ms"));
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}

