package fr.ortolang.diffusion.runtime.engine.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.referentiel.ReferentielServiceException;
import fr.ortolang.diffusion.referentiel.entity.ReferentielType;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class ImportReferentielTask extends RuntimeEngineTask {
	
	private static final Logger LOGGER = Logger.getLogger(ImportReferentielTask.class.getName());

	public static final String NAME = "Import Referentiel";

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		checkParameters(execution);
		String referentielpath = execution.getVariable(REFERENTIEL_PATH_PARAM_NAME, String.class);
		String referentieltype = execution.getVariable(REFERENTIEL_TYPE_PARAM_NAME, String.class);
		
		File referentielPathFile = new File(referentielpath);
		if(referentielPathFile.exists()) {
			for(File referentielEntityFile : referentielPathFile.listFiles()) {

				boolean exist = false;
				String name = referentielEntityFile.getName().substring(0, referentielEntityFile.getName().length()-5);
				try {
					getReferentielService().readReferentielEntity(name);
					LOGGER.log(Level.FINE, "  referentiel entity already exists for key: " + name);
					exist = true;
				} catch (ReferentielServiceException | KeyNotFoundException e) {
					//
				}
				
				String content = getContent(referentielEntityFile);
				if(content==null) {
					continue;
				}
				ReferentielType type = getReferentielType(referentieltype);
				if(type==null) {
					continue;
				}
				if(!exist) {
					try {
						getReferentielService().createReferentielEntity(name, type, content);
						LOGGER.log(Level.FINE, "  referentiel entity created with name "+name);
					} catch (ReferentielServiceException | KeyAlreadyExistsException | AccessDeniedException e) {
						LOGGER.log(Level.SEVERE, "  unable to create referentiel entity named "+name, e);
					}
				} else {
					LOGGER.log(Level.INFO, "  update referentiel entity "+name);
					try {
						getReferentielService().updateReferentielEntity(name, content);
						LOGGER.log(Level.FINE, "  referentiel entity updated with name "+name);
					} catch (ReferentielServiceException | AccessDeniedException | KeyNotFoundException e) {
						LOGGER.log(Level.SEVERE, "  unable to create referentiel entity named "+name, e);
					}
				}
			}
		} else {
			LOGGER.log(Level.SEVERE, "  unable to import referentiel : " + referentielPathFile);
		}
		
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Import Referentiel entities done"));
		execution.setVariable("greettime", new Date());
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

	private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(REFERENTIEL_PATH_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + REFERENTIEL_PATH_PARAM_NAME + " is not set");
		}
		if (!execution.hasVariable(REFERENTIEL_TYPE_PARAM_NAME) ) {
			throw new RuntimeEngineTaskException("execution variable " + REFERENTIEL_TYPE_PARAM_NAME + " is not set");
		}
	}
	
	private ReferentielType getReferentielType(String type) {
		switch(type) {
			case "Organization":
				return ReferentielType.ORGANIZATION;
			case "Person":
				return ReferentielType.PERSON;
			default:
				LOGGER.log(Level.SEVERE, "  referentiel type unknown : "+type);
				return null;
		}
	}
	
	private String getContent(File file) {
		String content = null;
		try {
			content = IOUtils.toString(new FileInputStream(file));
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "  unable to get content of file : "+file, e);
		}
		return content;
	}

}
