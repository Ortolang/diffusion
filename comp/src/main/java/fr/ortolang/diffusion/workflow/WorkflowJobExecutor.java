package fr.ortolang.diffusion.workflow;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.impl.jobexecutor.ExecuteJobsRunnable;
import org.activiti.engine.impl.jobexecutor.JobExecutor;

public class WorkflowJobExecutor extends JobExecutor {

	  private static Logger logger = Logger.getLogger(WorkflowJobExecutor.class.getName());
	  
	  private ExecutorService executor;
	  
	  public WorkflowJobExecutor(ExecutorService executor) {
		  this.executor = executor;
	  }
	    
	  protected void startExecutingJobs() {
	    startJobAcquisitionThread(); 
	  }
	    
	  protected void stopExecutingJobs() {
	    stopJobAcquisitionThread();
	    executor.shutdown();
	    try {
	      if(!executor.awaitTermination(60L, TimeUnit.SECONDS)) {
	    	  logger.log(Level.WARNING, "Timeout during shutdown of job executor. "
	                + "The current running jobs could not end within 60 seconds after shutdown operation.");        
	      }              
	    } catch (InterruptedException e) {
	    	logger.log(Level.WARNING, "Interrupted while shutting down the job executor. ", e);
	    }

	    executor = null;
	  }
	  
	  public void executeJobs(List<String> jobIds) {
	    try {
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