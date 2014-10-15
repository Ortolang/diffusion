package fr.ortolang.diffusion.runtime.activiti;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.impl.jobexecutor.ExecuteJobsRunnable;
import org.activiti.engine.impl.jobexecutor.JobExecutor;

public class ActivitiEngineJobExecutor extends JobExecutor {

	private static Logger logger = Logger.getLogger(ActivitiEngineJobExecutor.class.getName());

	private ExecutorService executor;
	
	public ActivitiEngineJobExecutor(ExecutorService executor) {
		logger.log(Level.INFO, "Instanciating new WorkflowJobExecutor");
		this.executor = executor;
	}

	protected void startExecutingJobs() {
		logger.log(Level.INFO, "Starting JobAcquisitionThread");
		executor.execute(acquireJobsRunnable);
	}

	protected void stopExecutingJobs() {
		logger.log(Level.INFO, "Stopping JobAcquisitionThread...");
		acquireJobsRunnable.stop();
		logger.log(Level.INFO, "JobAcquisitionThread stopped");
	}

	public void executeJobs(List<String> jobIds) {
		try {
			logger.log(Level.INFO, "Using executor to execute jobs");
			executor.execute(new ExecuteJobsRunnable(this, jobIds));
		} catch (RejectedExecutionException e) {
			rejectedJobsHandler.jobsRejected(this, jobIds);
		}
	}

	public ExecutorService getExecutorService() {
		return executor;
	}

	public void setExecutorService(ExecutorService executor) {
		this.executor = executor;
	}

}