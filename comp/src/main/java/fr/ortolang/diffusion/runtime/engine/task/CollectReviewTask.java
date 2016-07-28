package fr.ortolang.diffusion.runtime.engine.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

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
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(getReviewer().getValue(execution)).append(") ");
        builder.append("-> ").append(getGrade().getValue(execution));
        builder.append(" - ").append(getReason().getValue(execution));
        builder.append("\r\n");
        String review = execution.getVariable(REVIEW_RESULTS, String.class);
        review += builder.toString();
        execution.setVariable(REVIEW_RESULTS, review);
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
