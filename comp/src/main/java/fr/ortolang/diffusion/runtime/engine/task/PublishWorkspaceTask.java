package fr.ortolang.diffusion.runtime.engine.task;

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
import fr.ortolang.diffusion.publication.type.ForAllPublicationType;
import fr.ortolang.diffusion.publication.type.PublicationType;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class PublishWorkspaceTask extends RuntimeEngineTask {

	public static final String NAME = "Publish Workspace";
	public static final String ROOT = "root";

	private static final Logger logger = Logger.getLogger(PublishWorkspaceTask.class.getName());

	public PublishWorkspaceTask() {
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(ROOT)) {
			throw new RuntimeEngineTaskException("execution variable " + ROOT + " is not set");
		}
		String root = execution.getVariable(ROOT, String.class);

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

	@Override
	public boolean needEngineAuth() {
		return false;
	}

	private void builtPublicationMap(String key, Map<String, PublicationType> map) throws RuntimeEngineTaskException {
		try {
			OrtolangObject object = getCoreService().findObject(key);
			map.put(key, PublicationType.getType(ForAllPublicationType.NAME));
			
			Set<MetadataElement> mde = ((MetadataSource)object).getMetadatas();
			for ( MetadataElement element : mde) {
				map.put(element.getKey(), PublicationType.getType(ForAllPublicationType.NAME));
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
