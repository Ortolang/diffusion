package fr.ortolang.diffusion.runtime.engine.task;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;
import javax.transaction.SystemException;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class RebuildOAITask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(RebuildOAITask.class.getName());

    public static final String NAME = "Rebuild OAI";
    
    private StringBuilder report = new StringBuilder();
    private boolean partial = false;
    
	@Override
	public String getTaskName() {
		return NAME;
	}

	@Override
	public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
		report = new StringBuilder();
		List<String> aliases = null;
		try {
            if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                getUserTransaction().begin();
                LOGGER.log(Level.FINE, "starting new user transaction.");
                report.append("[BEGIN-TRAN]\r\n");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
        }
		
        try {
            aliases = getCoreService().listAllWorkspaceAlias();
        } catch (AccessDeniedException | CoreServiceException e) {
        	report.append("unable to list all workspace alias").append("\r\n").append(e.getMessage()).append("\r\n");
        	throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Report: \r\n" + report.toString(), e));
        	throw new RuntimeEngineTaskException("unable to rebuild oai", e);
        }
        
    	report.append("LIST OF WORKSPACES REBUILDED :").append("\r\n");
        for (String alias : aliases) {
        	try {
                String wskey = getCoreService().resolveWorkspaceAlias(alias);
                getOaiService().buildFromWorkspace(wskey);
                report.append("+ ").append(alias).append("\r\n");

                if (getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                    report.append("[COMMIT-TRAN]\r\n");
                    getUserTransaction().commit();
                    getUserTransaction().begin();
                    report.append("[BEGIN-TRAN]\r\n");
                }
        	} catch (Exception e) {
        		try {
        			report.append("[ROLLBACK-TRAN]\r\n");
					getUserTransaction().rollback();
                    getUserTransaction().begin();
                    report.append("[BEGIN-TRAN]\r\n");
				} catch (Exception e1) {
					LOGGER.log(Level.SEVERE, "Unable to rollback transaction", e1);
				}
        		report.append("- ").append(alias).append("\r\n").append(e.getMessage()).append("\r\n");
        		partial = true;
        	}
        }
        
        if ( partial ) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Some elements has not been rebuild (see trace for detail)"));
        } else {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "All elements rebuiled succesfully"));
        }
        throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Report: \r\n" + report.toString(), null));
	}

}
