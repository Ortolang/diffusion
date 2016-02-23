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

import fr.ortolang.diffusion.referential.ReferentialServiceException;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class ImportReferentialEntityTask extends RuntimeEngineTask {
	
	private static final Logger LOGGER = Logger.getLogger(ImportReferentialEntityTask.class.getName());

	public static final String NAME = "Import Referential entity";

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		checkParameters(execution);
		String referentialPathParam = execution.getVariable(REFERENTIAL_PATH_PARAM_NAME, String.class);
		
		File referentialPathFile = new File(referentialPathParam);
		if(referentialPathFile.exists()) {
			
			final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.{json}");
			final Path referentialPath = Paths.get(referentialPathParam);
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
									LOGGER.log(Level.FINE, "Referential entity content is empty for file " + jsonFile);
									return FileVisitResult.CONTINUE;
								}
								
								String type = extractField(content, "type");
								if(type==null) {
									LOGGER.log(Level.INFO, "Referential entity type unknown for file " + jsonFile);
									return FileVisitResult.CONTINUE;
								}

								String name = jsonFile.getName().substring(0, jsonFile.getName().length()-5);
								try {
									boolean exist = exists(name, type);
									
									if(!exist) {
										createReferentialEntity(name, type, content);
									} else {
										updateReferentialEntity(name, type, content);
									}
								} catch(RuntimeEngineTaskException e) {
									LOGGER.log(Level.SEVERE, "  unable to import referential entity ("+type+") named "+name, e);
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
				LOGGER.log(Level.SEVERE, "  unable to import referential : " + referentialPathFile, e);
			}
			
		} else {
			LOGGER.log(Level.SEVERE, "Referential folder doesn't exists : " + referentialPathFile);
		}
		
		throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Import Referential entities done"));
		execution.setVariable("greettime", new Date());
	}

	@Override
	public String getTaskName() {
		return NAME;
	}

	private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
		if (!execution.hasVariable(REFERENTIAL_PATH_PARAM_NAME)) {
			throw new RuntimeEngineTaskException("execution variable " + REFERENTIAL_PATH_PARAM_NAME + " is not set");
		}
	}
	
	private boolean exists(String name, String type) throws RuntimeEngineTaskException {
		boolean exist = false; 
		try {
			getReferentialService().readEntity(name);
			LOGGER.log(Level.FINE, "  referential entity already exists for key: " + name);
			exist = true;
		} catch (ReferentialServiceException | KeyNotFoundException e) {
			//
		}
		return exist;
	}
	
	private void createReferentialEntity(String name, String type, String content) throws RuntimeEngineTaskException {
		try {
			LOGGER.log(Level.FINE, "  add referential entity "+name+" with type "+type);

			ReferentialEntityType entityType = getEntityType(type.toUpperCase());
			if(entityType!=null) {
				getReferentialService().createEntity(name, entityType, content);
			} else {
				LOGGER.log(Level.SEVERE, "  unable to find type of referential entity named "+name);
				throw new RuntimeEngineTaskException("unable to create referential entity named " + name + " and type " + type);
			}
			
			LOGGER.log(Level.FINE, "  referential entity created with name "+name);
		} catch (ReferentialServiceException | KeyAlreadyExistsException | AccessDeniedException e) {
			LOGGER.log(Level.SEVERE, "  unable to create referential entity named "+name, e);
		}
	}

	private void updateReferentialEntity(String name, String type, String content) throws RuntimeEngineTaskException {
		try {
			LOGGER.log(Level.FINE, "  update referential entity "+name+" with type "+type);

			ReferentialEntityType entityType = getEntityType(type.toUpperCase());
			if(entityType!=null) {
				getReferentialService().updateEntity(name, entityType, content);
			} else {
				LOGGER.log(Level.SEVERE, "  unable to find type of referential entity named "+name);
				throw new RuntimeEngineTaskException("unable to create referential entity named " + name + " and type " + type);
			}
			
			LOGGER.log(Level.FINE, "  referential entity updated with name "+name);
		} catch (ReferentialServiceException | AccessDeniedException | KeyNotFoundException e) {
			LOGGER.log(Level.SEVERE, "  unable to update referential entity named "+name, e);
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
	
	private String extractField(String jsonContent, String fieldName) {
		String fieldValue = null;
		StringReader reader = new StringReader(jsonContent);
		JsonReader jsonReader = Json.createReader(reader);
		try {
			JsonObject jsonObj = jsonReader.readObject();
			fieldValue = jsonObj.getString(fieldName);
		} catch(NullPointerException | ClassCastException e) {
			LOGGER.log(Level.WARNING, "No property '"+fieldName+"' in json object");
		} finally {
			jsonReader.close();
			reader.close();
		}

		return fieldValue;
	}

    private ReferentialEntityType getEntityType(String type) {
    	try {
    		return ReferentialEntityType.valueOf(type);
    	} catch(IllegalArgumentException e) {
    		LOGGER.log(Level.WARNING, "Asking entity type unknown : " + type);
    		return null;
    	}
    }

}
