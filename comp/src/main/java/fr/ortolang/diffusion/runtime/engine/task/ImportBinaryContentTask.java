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
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;

public class ImportBinaryContentTask extends RuntimeEngineTask {
	
	public static final String NAME = "Import Binary Content";
	
	public static final String DATA_PREFIX = "data/";
	public static final String OBJECTS_PREFIX = "objects/";
	public static final String METADATA_PREFIX = "metadata/";
	
	private static final Logger logger = Logger.getLogger(ImportBinaryContentTask.class.getName());

	public ImportBinaryContentTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(BAG_PATH_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + BAG_PATH_PARAM_NAME + " is not set");
		}
		if ( !execution.hasVariable(BAG_VERSION_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + BAG_VERSION_PARAM_NAME + " is not set");
		}
		
		String version = DATA_PREFIX + execution.getVariable(BAG_VERSION_PARAM_NAME, String.class);
		if ( !version.endsWith("/") ) {
			version += "/";
		}
		logger.log(Level.INFO, "Starting binary content import for bag version " + version);
		
		Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
		BagFactory factory = new BagFactory();
		Bag bag = factory.createBag(bagpath.toFile());
		Collection<BagFile> payload = bag.getPayload();
		
		long start = System.currentTimeMillis();
		long size = 0;
		int cpt = 0;
		Map<String, String> oentries = new HashMap<String, String>();
		Map<String, String> mdentries = new HashMap<String, String>();
		for (BagFile file : payload) {
			if (file.getFilepath().startsWith(version + OBJECTS_PREFIX)) {
				String oentry = file.getFilepath().substring((version + OBJECTS_PREFIX).length());
				try {
					InputStream is = file.newInputStream();
					String ohash = getCoreService().put(is);
					oentries.put(oentry, ohash);
					try {
						is.close();
					} catch (IOException e) {
						logger.log(Level.WARNING, "unable to close input stream", e);
					}
					cpt++;
					size += file.getSize();
				} catch (CoreServiceException | DataCollisionException e) {
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Error importing binary content of bag entry [" + file.getFilepath() + "]"));
					logger.log(Level.SEVERE, "error importing binary content of bag entry [" + file.getFilepath() + "]");
				}
			} else if (file.getFilepath().startsWith(version + METADATA_PREFIX)) {
				String mdentry = file.getFilepath().substring((version + METADATA_PREFIX).length());
				try {
					InputStream is = file.newInputStream();
					String mdhash = getCoreService().put(is);
					mdentries.put(mdentry, mdhash);
					try {
						is.close();
					} catch (IOException e) {
						logger.log(Level.WARNING, "unable to close input stream", e);
					}
					cpt++;
					size += file.getSize();
				} catch (CoreServiceException | DataCollisionException e) {
					throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Error importing binary content of bag entry [" + file.getFilepath() + "]"));
					logger.log(Level.SEVERE, "error importing binary content of bag entry [" + file.getFilepath() + "]");
				}
			} 
		}
		long stop = System.currentTimeMillis();
		try {
			bag.close();
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to close bag", e);
		}
		execution.setVariable(OBJECT_ENTRIES_PARAM_NAME, oentries);
		execution.setVariable(METADATA_ENTRIES_PARAM_NAME, mdentries);
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), cpt + " binary elements imported successfully in " + (stop - start) + " ms for a total size of " + size + " octets"));
		logger.log(Level.FINE, cpt + " binary elements imported successfully in " + (stop - start) + " ms for a total size of " + size + " octets");
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}

