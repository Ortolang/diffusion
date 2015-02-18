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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;

public class ListBagVersionsTask extends RuntimeEngineTask {
	
	public static final String NAME = "List Bag Versions";
	
	public static final String HEAD_PREFIX = "data/head/";
	public static final String SNAPSHOTS_PREFIX = "data/snapshots/";

	private static final Logger logger = Logger.getLogger(ListBagVersionsTask.class.getName());

	public ListBagVersionsTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if ( !execution.hasVariable(BAG_PATH_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + BAG_PATH_PARAM_NAME + " is not set");
		}
		
		logger.log(Level.INFO, "listing bag versions");
		Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
		BagFactory factory = new BagFactory();
		Bag bag = factory.createBag(bagpath.toFile());
		Collection<BagFile> payload = bag.getPayload();
		Map<Integer, String> snapshots = new HashMap<Integer, String> ();
		for (BagFile bagfile : payload) {
			if ( bagfile.getFilepath().startsWith(SNAPSHOTS_PREFIX) ) {
				String[] parts = bagfile.getFilepath().substring(SNAPSHOTS_PREFIX.length()).split("/");
				if ( parts.length <= 1 || parts[0].length() <= 0 || parts[1].length() <= 0 ) {
					logger.log(Level.INFO, "Unparsable snapshot hierarchy found: " + Arrays.deepToString(parts));
				}
				Integer index = -1;
				try {
					index = Integer.decode(parts[0]);
					if ( snapshots.containsKey(index) ) {
						if ( !snapshots.get(index).equals(parts[1]) ) {
							logger.log(Level.WARNING, "Found a version with existing index but different name!! " + snapshots.get(index) + " - " + parts[1]);
						}
					} else {
						logger.log(Level.INFO, "Found new version with index: " + index + " and name: " + parts[1]);
						snapshots.put(index, parts[1]);
					}
				} catch ( Exception e ) {
					logger.log(Level.INFO, "Snapshot index is not a number: " + parts[0]);
				}
			}
		}
	
		List<String> versions = new ArrayList<String> ();
		String snapshot = null;
		Integer cpt = 1;
		while ( (snapshot = snapshots.get(cpt)) != null ) {
			versions.add("snapshots/" + cpt + "/" + snapshot);
			cpt++;
		}
		versions.add(Workspace.HEAD);
			
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), versions.size() + " versions found in this bag: " + Arrays.deepToString(versions.toArray())));
		execution.setVariable(BAG_VERSIONS_PARAM_NAME, versions);
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

}

