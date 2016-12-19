package fr.ortolang.diffusion.runtime.engine.task;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.AliasNotFoundException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.oai.exception.OaiServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class RebuildOAITask extends RuntimeEngineTask {

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
            aliases = getCoreService().listAllWorkspaceAlias();
        } catch (AccessDeniedException | CoreServiceException e) {
        	report.append("unable to list all workspace alias").append("\r\n").append(e.getMessage());
        	throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Report: \r\n" + report.toString(), e));
        	throw new RuntimeEngineTaskException("unable to rebuild oai", e);
        }
        
    	report.append("LIST OF WORKSPACES REBUILDED :").append("\r\n");
        for (String alias : aliases) {
        	try {
                String wskey = getCoreService().resolveWorkspaceAlias(alias);
                getOaiService().buildFromWorkspace(wskey);
                report.append("+ ").append(alias).append("\r\n");
        	} catch (CoreServiceException | AliasNotFoundException | OaiServiceException e) {
        		report.append("- ").append(alias).append("\r\n");
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
