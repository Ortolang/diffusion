package fr.ortolang.diffusion.runtime.engine.task;

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
                        getHandleStore().recordHandle(handle.handle, "", handle.url);
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
        public String url = "";

        public JsonHandle() {
        }
    }

}