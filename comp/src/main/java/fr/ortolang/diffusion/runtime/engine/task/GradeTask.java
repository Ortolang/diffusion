package fr.ortolang.diffusion.runtime.engine.task;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.impl.util.json.JSONObject;

import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class GradeTask extends RuntimeEngineTask {

    public static final String NAME = "Grade";

    private static final Logger LOGGER = Logger.getLogger(GradeTask.class.getName());

    private Expression wskey;
    private Expression snapshot;
    private Expression grade;

    public GradeTask() {
    }

    public Expression getWskey() {
        return wskey;
    }

    public void setWskey(Expression wskey) {
        this.wskey = wskey;
    }

    public Expression getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(Expression snapshot) {
        this.snapshot = snapshot;
    }

    public Expression getGrade() {
        return grade;
    }

    public void setGrade(Expression grade) {
        this.grade = grade;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {

        String wskey = (String) getWskey().getValue(execution);
        String snapshot = (String) getSnapshot().getValue(execution);

        try {
            LOGGER.log(Level.FINE, "creating grade metadata...");
            Workspace workspace = getCoreService().readWorkspace(wskey);
            SnapshotElement se = workspace.findSnapshotByName(snapshot);
            if (se == null) {
                throw new RuntimeEngineTaskException("unable to find a snapshot with name " + snapshot + " in workspace " + wskey);
            }
            String root = se.getKey();
            String grade = (String) getGrade().getValue(execution);
            String json = builtGradeJson(grade);
            String hash = getBinaryStore().put(new ByteArrayInputStream(json.getBytes()));
            getCoreService().systemCreateMetadata(root, MetadataFormat.RATING, hash, MetadataFormat.RATING + ".json");
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "rate fixed to: " + grade));
            LOGGER.log(Level.FINE, "grade metadata created");

        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to create rate metadata: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unable to create rate metadata", e);
        }
    }

    private String builtGradeJson(String grade) throws Exception {
        int score = 0;
        switch (grade) {
        case "A":
            score = 1;
            break;
        case "B":
            score = 2;
            break;
        case "C":
            score = 3;
            break;
        case "D":
            score = 4;
            break;
        case "E":
            score = 5;
            break;
        default:
            score = 6;
            break;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("grade", grade);
        jsonObject.put("score", score);
        return jsonObject.toString();
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
