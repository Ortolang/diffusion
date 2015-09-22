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

				String content = getContent(referentielEntityFile);
				if(content==null) {
					continue;
				}
				ReferentielType type = getReferentielType(referentieltype);
				if(type==null) {
					continue;
				}

				boolean exist = false;
				String name = referentielEntityFile.getName().substring(0, referentielEntityFile.getName().length()-5);
				String key = type.toString() + ":" + name;
				try {
					getReferentielService().readReferentielEntity(key);
					LOGGER.log(Level.FINE, "  referentiel entity already exists for key: " + name);
					exist = true;
				} catch (ReferentielServiceException | KeyNotFoundException e) {
					//
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
						getReferentielService().updateReferentielEntity(key, content);
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
			case "Term":
				return ReferentielType.TERM;
			case "Language":
				return ReferentielType.LANGUAGE;
			case "AnnotationLevel":
				return ReferentielType.ANNOTATION_LEVEL;
			case "CorporaDataType":
				return ReferentielType.CORPORA_DATA_TYPE;
			case "CorporaFileEncoding":
				return ReferentielType.CORPORA_FILE_ENCODING;
			case "CorporaFormat":
				return ReferentielType.CORPORA_FORMAT;
			case "CorporaLanguageType":
				return ReferentielType.CORPORA_LANGUAGE_TYPE;
			case "CorporaStyle":
				return ReferentielType.CORPORA_STYLE;
			case "CorporaType":
				return ReferentielType.CORPORA_TYPE;
			case "LexiconDescriptionType":
				return ReferentielType.LEXICON_DESCRIPTION_TYPE;
			case "LexiconFormat":
				return ReferentielType.LEXICON_FORMAT;
			case "LexiconInputType":
				return ReferentielType.LEXICON_INPUT_TYPE;
			case "LexiconLanguageType":
				return ReferentielType.LEXICON_LANGUAGE_TYPE;
			case "Role":
				return ReferentielType.ROLE;
			case "StatusOfUse":
				return ReferentielType.STATUS_OF_USE;
			case "ToolFileEncoding":
				return ReferentielType.TOOL_FILE_ENCODING;
			case "ToolFunctionality":
				return ReferentielType.TOOL_FUNCTIONALITY;
			case "ToolInputData":
				return ReferentielType.TOOL_INPUT_DATA;
			case "ToolOutputData":
				return ReferentielType.TOOL_OUTPUT_DATA;
            case "OperatingSystem":
                return ReferentielType.OPERATING_SYSTEM;
            case "ProgrammingLanguage":
                return ReferentielType.PROGRAMMING_LANGUAGE;
            case "ToolSupport":
                return ReferentielType.TOOL_SUPPORT;
			case "ResourceType":
				return ReferentielType.RESOURCE_TYPE;
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
