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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonException;
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

    private StringBuilder report = new StringBuilder();
    private boolean partial = false;

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        checkParameters(execution);
        String referentialPathParam = execution.getVariable(REFERENTIAL_PATH_PARAM_NAME, String.class);
        report = new StringBuilder();

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
                                //									LOGGER.log(Level.SEVERE, "Referential entity content is empty for file " + jsonFile);
                                report.append(" - referential entity content is empty for file ").append(jsonFile).append("\r\n");
                                partial = true;
                                return FileVisitResult.CONTINUE;
                            }

                            String type = extractField(content, "type");
                            if(type==null) {
                                //									LOGGER.log(Level.SEVERE, "Referential entity type unknown for file " + jsonFile);
                                report.append(" - referential entity type unknown for file ").append(jsonFile).append("\r\n");
                                partial = true;
                                return FileVisitResult.CONTINUE;
                            }

                            String name = jsonFile.getName().substring(0, jsonFile.getName().length()-5);
                            try {
                                boolean exist = exists(name);

                                if(!exist) {
                                    createReferentialEntity(name, type, content);
                                    report.append(" + referential entity created : ").append(name).append("\r\n");
                                } else {
                                    updateReferentialEntity(name, type, content);
                                    report.append(" + referential entity updated : ").append(name).append("\r\n");
                                }
                            } catch(RuntimeEngineTaskException e) {
                                //									LOGGER.log(Level.SEVERE, "  unable to import referential entity ("+type+") named "+name, e);
                                report.append(" - unable to import referential entity '").append(name).append("' : ").append(e.getMessage()).append("\r\n");
                                partial = true;
                                return FileVisitResult.CONTINUE;
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
                //				LOGGER.log(Level.SEVERE, "  unable to import referential : " + referentialPathFile, e);
                report.append("Enable to import referential ").append(referentialPathFile).append(" caused by : ").append(e.getMessage()).append("\r\n");
                partial = true;
            }

        } else {
            //			LOGGER.log(Level.SEVERE, "Referential folder doesn't exists : " + referentialPathFile);
            report.append("Referential folder doesn't exists at ").append(referentialPathFile).append("\r\n");
            partial = true;
        }

        if ( partial ) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Some entities has not been imported (see trace for detail)"));
        } else {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "All entities imported succesfully"));
        }
        throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Report: \r\n" + report.toString(), null));
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

    private boolean exists(String name) throws RuntimeEngineTaskException {
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
            ReferentialEntityType entityType = getEntityType(type.toUpperCase());
            if(entityType!=null) {
                getReferentialService().createEntity(name, entityType, content);
            } else {
                throw new RuntimeEngineTaskException("type '"+type+"' unknown");
            }
        } catch (ReferentialServiceException | KeyAlreadyExistsException | AccessDeniedException e) {
            //			LOGGER.log(Level.SEVERE, "  unable to create referential entity named "+name, e);
            throw new RuntimeEngineTaskException(e.getMessage());
        }
    }

    private void updateReferentialEntity(String name, String type, String content) throws RuntimeEngineTaskException {
        try {
            ReferentialEntityType entityType = getEntityType(type.toUpperCase());
            if(entityType!=null) {
                getReferentialService().updateEntity(name, entityType, content);
            } else {
                throw new RuntimeEngineTaskException("type '"+type+"' unknown");
            }
        } catch (ReferentialServiceException | AccessDeniedException | KeyNotFoundException e) {
            //			LOGGER.log(Level.SEVERE, "  unable to update referential entity named "+name, e);
            throw new RuntimeEngineTaskException(e.getMessage());
        }
    }

    private String getContent(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return IOUtils.toString(is, "UTF-8");
        }
    }

    private String extractField(String jsonContent, String fieldName) {
        String fieldValue = null;
        StringReader reader = new StringReader(jsonContent);
        JsonReader jsonReader = Json.createReader(reader);
        try {
            JsonObject jsonObj = jsonReader.readObject();
            fieldValue = jsonObj.getString(fieldName);
        } catch(IllegalStateException | NullPointerException | ClassCastException | JsonException e) {
            LOGGER.log(Level.WARNING, "No property '"+fieldName+"' in json object", e);
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
