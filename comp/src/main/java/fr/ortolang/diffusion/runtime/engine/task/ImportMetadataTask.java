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

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class ImportMetadataTask extends RuntimeEngineTask {
	
	public static final String NAME = "Import Metadata";

	private static final Logger logger = Logger.getLogger(ImportMetadataTask.class.getName());

	public ImportMetadataTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(WORKSPACE_KEY_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
		}
		String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
		if ( !execution.hasVariable(METADATA_ENTRIES_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + METADATA_ENTRIES_PARAM_NAME + " is not set");
		}
		@SuppressWarnings("unchecked")
		Map<String, String> entries = execution.getVariable(METADATA_ENTRIES_PARAM_NAME, Map.class);
		
		logger.log(Level.INFO, "Starting metadata objects import for provided entries");
		long start = System.currentTimeMillis();
		int cpt = 0;
		for (Entry<String, String> entry : entries.entrySet()) {
			logger.log(Level.FINE, "treating metadata entry: " + entry.getKey());
			int lastPathIndex = entry.getKey().lastIndexOf("/");
			String mdfullname = entry.getKey();
			String mdname = mdfullname;
			String mdformat = "unknown";
			String mdpath = "/";
			if (lastPathIndex > -1) {
				mdfullname = entry.getKey().substring(lastPathIndex + 1);
				mdpath = entry.getKey().substring(0, lastPathIndex);
			}
			if (mdfullname.indexOf("[") == 0 && mdfullname.indexOf("]") >= 0) {
				mdformat = mdfullname.substring(1, mdfullname.indexOf("]")).trim();
				mdname = mdfullname.substring(mdfullname.indexOf("]") + 1).trim();
			}
			try {
				try {
					getCoreService().resolveWorkspaceMetadata(wskey, Workspace.HEAD, mdpath, mdname);
					logger.log(Level.FINE, "updating metadata object for path: " + mdpath + " and name: " + mdname);
					getCoreService().updateMetadataObject(wskey, mdpath, mdname, mdformat, entry.getValue());
				} catch ( InvalidPathException e ) {
					logger.log(Level.FINE, "creating metadata object for path: " + mdpath + " and name: " + mdname);
					getCoreService().createMetadataObject(wskey, mdpath, mdname, mdformat, entry.getValue());
					cpt++;
				}
			} catch (Exception e) {
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Error creating metadata object for path [" + mdpath + "] and name [" + mdname + "]"));
				logger.log(Level.SEVERE, "- error creating metadata for path: " + mdpath + " and name: " + mdname, e);
			}
		}
		long stop = System.currentTimeMillis();
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), cpt + " metadata objects created successfully in " + (stop - start) + " ms"));
		

		//TODO crawl the workspace head in order to delete objects that are not in the entry map....
		//TODO crawl the workspace head in order to delete empty collections
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}

