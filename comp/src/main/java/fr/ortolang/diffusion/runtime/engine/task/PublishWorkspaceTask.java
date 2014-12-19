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
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataSource;
import fr.ortolang.diffusion.publication.PublicationContext;
import fr.ortolang.diffusion.publication.type.ForAllPublicationType;
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

		Map<String, PublicationContext> map = new HashMap<String, PublicationContext>();
		logger.log(Level.INFO, "starting building publication map...");
		try {
			builtPublicationMap(root, map, root, PathBuilder.newInstance().path(root));
		} catch (InvalidPathException e1) {
		}
		logger.log(Level.INFO, "publication map built containing " + map.size() + " keys");
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "PublicationMap built, containing " + map.size() + " elements"));

		StringBuffer report = new StringBuffer();
		logger.log(Level.INFO, "starting publication");
		for (Entry<String, PublicationContext> entry : map.entrySet()) {
			try {
				getPublicationService().publish(entry.getKey(), entry.getValue());
				report.append("key [").append(entry.getKey()).append("] published successfully with publication type [").append(entry.getValue().getType().getName()).append("]\r\n");
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

	private void builtPublicationMap(String key, Map<String, PublicationContext> map, String root, PathBuilder path) throws RuntimeEngineTaskException {
		try {
			OrtolangObject object = getCoreService().findObject(key);
			map.put(key, new PublicationContext(PublicationType.getType(ForAllPublicationType.NAME), root, path.build(), object.getObjectName()));
			
			Set<MetadataElement> mde = ((MetadataSource)object).getMetadatas();
			for ( MetadataElement element : mde) {
				map.put(element.getKey(), new PublicationContext(PublicationType.getType(ForAllPublicationType.NAME), root, path.build(), object.getObjectName()));
			}
			
			if (object instanceof Collection) {
				
				for (CollectionElement element : ((Collection)object).getElements()) {
					builtPublicationMap(element.getKey(), map, root, path.path(element.getKey()));
				}
			}
		} catch (InvalidPathException | KeyNotFoundException | AccessDeniedException | OrtolangException e) {
			throw new RuntimeEngineTaskException("unexpected error while trying to load core object for key : " + key, e);
		}
	}
}
