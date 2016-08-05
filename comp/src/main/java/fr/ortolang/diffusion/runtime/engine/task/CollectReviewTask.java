package fr.ortolang.diffusion.runtime.engine.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class CollectReviewTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(LockWorkspaceTask.class.getName());
    public static final String NAME = "Collect Review";

    private Expression reviewer;
    private Expression grade;
    private Expression reason;

    public CollectReviewTask() {
    }

    public Expression getReviewer() {
        return reviewer;
    }

    public void setReviewer(Expression reviewer) {
        this.reviewer = reviewer;
    }

    public Expression getGrade() {
        return grade;
    }

    public void setGrade(Expression grade) {
        this.grade = grade;
    }

    public Expression getReason() {
        return reason;
    }

    public void setReason(Expression reason) {
        this.reason = reason;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        LOGGER.log(Level.FINE, "collecting reviewer result in parent process");
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("reviewer", getReviewer().getValue(execution));
            jsonObject.put("grade", getGrade().getValue(execution));
            jsonObject.put("reason", getReason().getValue(execution));
            String review = execution.getVariable(REVIEW_RESULTS, String.class);
            JSONArray array;
            if ( review.length() <= 0 ) {
                array = new JSONArray();
            } else {
                array = new JSONArray(review);
            }
            array.put(jsonObject);
            execution.setVariable(REVIEW_RESULTS, array.toString());
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
