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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;

public class ImportHandlesTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(ImportHandlesTask.class.getName());

    public static final String NAME = "Import Handles";

    public ImportHandlesTask() {
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        checkParameters(execution);
        String handlespath = execution.getVariable(HANDLES_PATH_PARAM_NAME, String.class);
        if (execution.getVariable(INITIER_PARAM_NAME, String.class).equals(MembershipService.SUPERUSER_IDENTIFIER)) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            try {
                if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                    LOGGER.log(Level.FINE, "starting new user transaction.");
                    getUserTransaction().begin();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
            }
            try {
                boolean needcommit;
                long tscommit = System.currentTimeMillis();
                List<JsonHandle> handles = Arrays.asList(mapper.readValue(new File(handlespath), JsonHandle[].class));
                LOGGER.log(Level.FINE, "- starting import handles");
                boolean partial = false;
                StringBuilder report = new StringBuilder();
                for (JsonHandle handle : handles) {
                    needcommit = false;
                    try {
                        getHandleStore().recordHandle(handle.handle, handle.key, handle.url);
                    } catch (HandleStoreServiceException e) {
                        partial = true;
                        report.append("unable to import handle [" + handle.handle + "] : " + e.getMessage() + "\r\n");
                    }
                    if ( System.currentTimeMillis() - tscommit > 30000 ) {
                        LOGGER.log(Level.FINE, "current transaction exceed 30sec, need commit.");
                        needcommit = true;
                    }
                    try {
                        if (needcommit && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                            LOGGER.log(Level.FINE, "committing active user transaction.");
                            getUserTransaction().commit();
                            tscommit = System.currentTimeMillis();
                            getUserTransaction().begin();
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
                    }
                }
                if (partial) {
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Some handles has not been imported (see trace for detail)"));
                    throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), report.toString(), null));
                }
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Import Handles done"));
            } catch (IOException e) {
                throw new RuntimeEngineTaskException("error parsing json file: " + e.getMessage());
            }
        } else {
            throw new RuntimeEngineTaskException("only " + MembershipService.SUPERUSER_IDENTIFIER + " can perform this task !!");
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

    private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
        if (!execution.hasVariable(HANDLES_PATH_PARAM_NAME)) {
            throw new RuntimeEngineTaskException("execution variable " + HANDLES_PATH_PARAM_NAME + " is not set");
        }
        if (!execution.hasVariable(INITIER_PARAM_NAME)) {
            throw new RuntimeEngineTaskException("execution variable " + INITIER_PARAM_NAME + " is not set");
        }
    }

    static class JsonHandle {
        public String handle = "";
        public String key = "";
        public String url = "";

        public JsonHandle() {
        }
    }

}