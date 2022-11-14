package fr.ortolang.diffusion.runtime.engine.task;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.apache.commons.compress.archivers.ArchiveOutputStream;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangEvent.ArgumentsBuilder;
import fr.ortolang.diffusion.archive.ArchiveEntry;
import fr.ortolang.diffusion.archive.ArchiveService;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class CreateSipArchiveTask extends RuntimeEngineTask {
    
    public static final String NAME = "CreateSipTar";

    private static final Logger LOGGER = Logger.getLogger(CreateSipArchiveTask.class.getName());

    private Expression wskeyExpression;
    private Expression snapshotExpression;
    private Expression schemaExpression;

    public CreateSipArchiveTask() {
        // No need to initialize
    }

    public Expression getWskey() {
        return wskeyExpression;
    }

    public void setWskey(Expression wskeyExpression) {
        this.wskeyExpression = wskeyExpression;
    }

    public Expression getSnapshot() {
        return snapshotExpression;
    }

    public void setSnapshot(Expression snapshotExpression) {
        this.snapshotExpression = snapshotExpression;
    }

    public Expression getSchema() {
        return schemaExpression;
    }

    public void setSchema(Expression schemaExpression) {
        this.schemaExpression = schemaExpression;
    }


    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        
        String wskey = (String) getWskey().getValue(execution);
        String snapshot = null;
        String schema = null;
        if (execution.hasVariable("snapshot")) {
            snapshot = (String) getSchema().getValue(execution);
        }
        if (execution.hasVariable("schema")) {
            schema = (String) getSchema().getValue(execution);
        }

        LOGGER.log(Level.FINE, "starting creation of SIP archive for workspace {0} ...", wskey);
        String caller = getMembershipService().getProfileKeyForConnectedIdentifier();
        StringBuilder reports = new StringBuilder();

        // Building the list of entries to the sip archive
        List<ArchiveEntry> archiveList = null;
        try {
            archiveList = getArchiveService().buildWorkspaceArchiveList(wskey, snapshot);
        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to build archive sip map: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unable to build archive sip map", e);
        }
        throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Archive entry list builded"));
        
        try {
            if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                LOGGER.log(Level.FINE, "starting new user transaction.");
                getUserTransaction().begin();
                reports.append("[BEGIN-TRAN]\r\n");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
            reports.append("Unable to begin a transaction\r\n");
        }
        try {
            long tscommit = System.currentTimeMillis();
            
            // Adding each element into sip archive
            ArchiveOutputStream archiveOutputStream = getArchiveService().createArchive(wskey);
            reports.append("Creating archive of workspace ").append(wskey).append(" of snapshot ").append(snapshot)
                .append("\r\n");
            for( ArchiveEntry archiveEntry : archiveList) {
                getArchiveService().addEntryToArchive(archiveEntry, archiveOutputStream);
                reports.append(" + imported archive entry key ").append(archiveEntry.getKey())
                    .append(" to path ").append(archiveEntry.getPath()).append("\r\n");
            	try {
                    if (System.currentTimeMillis() - tscommit > 30000 && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                        reports.append("[COMMIT-TRAN]\r\n");
                        getUserTransaction().commit();
                        tscommit = System.currentTimeMillis();
                        getUserTransaction().begin();
                        reports.append("[BEGIN-TRAN]\r\n");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
                    reports.append("Unable to commit or begin a transaction\r\n");
                }
            }
            
            try {
                LOGGER.log(Level.FINE, "committing active user transaction and starting new one.");
                getUserTransaction().commit();
                getUserTransaction().begin();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
                reports.append("Unable to commit or begin a transaction\r\n");
            }
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "All archivable file imported"));
            // Generating sip xml file and add it to the sip archive
            getArchiveService().addXmlSIPFileToArchive(wskey, snapshot, schema, archiveList, archiveOutputStream);
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "XML Sip file imported"));
            // Closes the sip archive
            getArchiveService().finishArchive(archiveOutputStream);
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Archive generation done"));

            ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("wskey", wskey);
            if (snapshot != null) {
                argumentsBuilder.addArgument("snapshot", snapshot);
            }
            getNotificationService().throwEvent(wskey, caller, Workspace.OBJECT_TYPE, 
                OrtolangEvent.buildEventType(ArchiveService.SERVICE_NAME, Workspace.OBJECT_TYPE, "archive-workspace"), argumentsBuilder.build());

            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "All elements archived succesfully"));
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Report: \r\n" + reports.toString(), null));
        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to build archive sip map: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unable to build archive sip map", e);
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }
    
    @Override
    public int getTransactionTimeout() {
        return 5000;
    }

}
