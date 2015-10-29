package fr.ortolang.diffusion.runtime.engine.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

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
		String referentielPathParam = execution.getVariable(REFERENTIEL_PATH_PARAM_NAME, String.class);
		
		File referentielPathFile = new File(referentielPathParam);
		if(referentielPathFile.exists()) {
			
			final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.{json}");
			final Path referentialPath = Paths.get(referentielPathParam);
			try {
				Files.walkFileTree(referentialPath, new FileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path filepath, BasicFileAttributes attrs) throws IOException {
							Path filename = filepath.getFileName();
							if (filename!= null && matcher.matches(filename)) {
								
								File jsonFile = filepath.toFile();
								String content = getContent(jsonFile);
								if(content==null) {
									LOGGER.log(Level.INFO, "Referential entity content is empty for file " + jsonFile);
									return FileVisitResult.CONTINUE;
								}
								
								ReferentielType type = extractReferentielType(content);
								if(type==null) {
									LOGGER.log(Level.INFO, "Referential entity type unknown for file " + jsonFile);
									return FileVisitResult.CONTINUE;
								}

								String name = jsonFile.getName().substring(0, jsonFile.getName().length()-5);
								try {
									addReferentialEntity(name, type, content);
								} catch (RuntimeEngineTaskException e) {
									LOGGER.log(Level.WARNING, "  unable to import referentiel : " + referentielPathFile);
								}
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}

					});
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "  unable to import referentiel : " + referentielPathFile);
			}
			
		} else {
			LOGGER.log(Level.SEVERE, "Referential folder doesn't exists : " + referentielPathFile);
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
	}
	
	private void addReferentialEntity(String name, ReferentielType type, String content) throws RuntimeEngineTaskException {
		boolean exist = false;
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
				LOGGER.log(Level.FINE, "  add referentiel entity "+name); 
				getReferentielService().createReferentielEntity(name, type, content);
				LOGGER.log(Level.FINE, "  referentiel entity created with name "+name);
			} catch (ReferentielServiceException | KeyAlreadyExistsException | AccessDeniedException e) {
				LOGGER.log(Level.SEVERE, "  unable to create referentiel entity named "+name, e);
			}
		} else {
			LOGGER.log(Level.FINE, "  update referentiel entity "+name); 
			try {
				getReferentielService().updateReferentielEntity(key, content);
				LOGGER.log(Level.FINE, "  referentiel entity updated with name "+name);
			} catch (ReferentielServiceException | AccessDeniedException | KeyNotFoundException e) {
				LOGGER.log(Level.SEVERE, "  unable to create referentiel entity named "+name, e);
			}
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
			case "Country":
				return ReferentielType.COUNTRY;
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
	
	private String getContent(File file) throws IOException {
		String content = null;
		InputStream is = new FileInputStream(file);
		try {
			content = IOUtils.toString(is);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "  unable to get content of file : "+file, e);
		} finally {
			is.close();
		}
		return content;
	}
	
	private ReferentielType extractReferentielType(String jsonContent) {
		String type = null;
		StringReader reader = new StringReader(jsonContent);
		JsonReader jsonReader = Json.createReader(reader);
		try {
			JsonObject jsonObj = jsonReader.readObject();
			type = jsonObj.getString("type");
		} catch(NullPointerException | ClassCastException e) {
			LOGGER.log(Level.WARNING, "No property 'type' in json object");
		} finally {
			jsonReader.close();
			reader.close();
		}

		if(type!=null) {
			return getReferentielType(type);
		}
		return null;
	}

}
