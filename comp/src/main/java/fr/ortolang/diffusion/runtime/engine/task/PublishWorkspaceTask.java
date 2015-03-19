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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.jboss.logmanager.Level;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataSource;
import fr.ortolang.diffusion.publication.type.FreeForAll;
import fr.ortolang.diffusion.publication.type.PublicationType;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class PublishWorkspaceTask extends RuntimeEngineTask {

	public static final String NAME = "Publish Workspace";
	
	private static final Logger logger = Logger.getLogger(PublishWorkspaceTask.class.getName());

	public PublishWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(ROOT_COLLECTION_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + ROOT_COLLECTION_PARAM_NAME + " is not set");
		}
		String root = execution.getVariable(ROOT_COLLECTION_PARAM_NAME, String.class);

		Map<String, PublicationType> map = new HashMap<String, PublicationType>();
		logger.log(Level.INFO, "starting building publication map...");
		builtPublicationMap(root, map);
		logger.log(Level.INFO, "publication map built containing " + map.size() + " keys");
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "PublicationMap built, containing " + map.size() + " elements"));

		StringBuffer report = new StringBuffer();
		logger.log(Level.INFO, "starting publication");
		for (Entry<String, PublicationType> entry : map.entrySet()) {
			try {
				getPublicationService().publish(entry.getKey(), entry.getValue());
				report.append("key [").append(entry.getKey()).append("] published successfully with publication type [").append(entry.getValue().getName()).append("]\r\n");
			} catch (Exception e) {
				logger.log(Level.INFO, "key [" + entry.getKey() + "] failed to publish: " + e.getMessage());
				report.append("key [").append(entry.getKey()).append("] failed to publish: ").append(e.getMessage()).append("\r\n");
			}
		}

		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "publication done report : \r\n" + report.toString()));
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

	private void builtPublicationMap(String key, Map<String, PublicationType> map) throws RuntimeEngineTaskException {
		try {
			OrtolangObject object = getCoreService().findObject(key);
			map.put(key, PublicationType.getType(FreeForAll.NAME));
			
			Set<MetadataElement> mde = ((MetadataSource)object).getMetadatas();
			for ( MetadataElement element : mde) {
				map.put(element.getKey(), PublicationType.getType(FreeForAll.NAME));
			}
			
			if (object instanceof Collection) {
				
				for (CollectionElement element : ((Collection)object).getElements()) {
					builtPublicationMap(element.getKey(), map);
				}
			}
		} catch (KeyNotFoundException | AccessDeniedException | OrtolangException e) {
			throw new RuntimeEngineTaskException("unexpected error while trying to load core object for key : " + key, e);
		}
	}
}
