package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangEvent.ArgumentsBuilder;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.entity.Set;
import fr.ortolang.diffusion.oai.exception.RecordNotFoundException;
import fr.ortolang.diffusion.oai.exception.SetNotFoundException;
import fr.ortolang.diffusion.publication.PublicationService;
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

		if (execution.hasVariable(WORKSPACE_ALIAS_PARAM_NAME)) {	
			aliases = Arrays.asList(execution.getVariable(WORKSPACE_ALIAS_PARAM_NAME, String.class));
		} else {
			try {
				aliases = getCoreService().listAllWorkspaceAlias();
			} catch (AccessDeniedException | CoreServiceException e) {
				report.append("unable to list all workspace alias").append("\r\n").append(e.getMessage()).append("\r\n");
				throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Report: \r\n" + report.toString(), e));
				throw new RuntimeEngineTaskException("unable to rebuild oai", e);
			}
		}
        
		// Cleans sets and records
		List<Set> sets = this.getOaiService().listSets();
		List<Record>  records = this.getOaiService().listRecords();
		removeOAIRecords(records);
		removeOAISets(sets);

        for (String alias : aliases) {
        	try {
                String wskey = getCoreService().resolveWorkspaceAlias(alias);
                String snapshot = getCoreService().findWorkspaceLatestPublishedSnapshot(wskey);
                
                if (snapshot != null) {
	                String caller = getMembershipService().getProfileKeyForConnectedIdentifier();
	                report.append("[INFO] publishing snapshot ").append(snapshot).append(" to workspace ").append(wskey).append("\r\n");
	                
	                ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("snapshot", snapshot);
	                getNotificationService().throwEvent(wskey, caller, Workspace.OBJECT_TYPE, 
	                        OrtolangEvent.buildEventType(PublicationService.SERVICE_NAME, Workspace.OBJECT_TYPE, "publish-snapshot"), argumentsBuilder.build());
                }
                
                if (getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
//                    report.append("[COMMIT-TRAN]\r\n");
                    getUserTransaction().commit();
                    getUserTransaction().begin();
//                    report.append("[BEGIN-TRAN]\r\n");
                }
        	} catch (Exception e) {
        		report.append("[ERROR] workspace ").append(alias).append("\r\n").append(e.getMessage()).append("\r\n");
        		try {
        			report.append("[ROLLBACK-TRAN]\r\n");
					getUserTransaction().rollback();
                    getUserTransaction().begin();
                    report.append("[BEGIN-TRAN]\r\n");
				} catch (Exception e1) {
					LOGGER.log(Level.SEVERE, "Unable to rollback transaction", e1);
				}
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

	private void removeOAIRecords(List<Record>  records) {
		records.forEach(rec -> {
			try {
				this.getOaiService().deleteRecord(rec.getId());
			} catch (RecordNotFoundException | RuntimeEngineTaskException e) {
				LOGGER.log(Level.SEVERE, "Unable to delete OAI record {0}", rec.getId());
			}
		});
	}

	private void removeOAISets(List<Set> sets) {
		sets.forEach(set -> {
			try {
				this.getOaiService().deleteSet(set.getSpec());
			} catch (SetNotFoundException | RuntimeEngineTaskException e) {
				LOGGER.log(Level.SEVERE, "Unable to delete OAI record {0}", set.getSpec());
			}
		});
	}

}
