package fr.ortolang.diffusion.runtime.engine.activiti;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.impl.jobexecutor.ExecuteJobsRunnable;
import org.activiti.engine.impl.jobexecutor.JobExecutor;

public class ActivitiEngineJobExecutor extends JobExecutor {

	private static final Logger LOGGER = Logger.getLogger(ActivitiEngineJobExecutor.class.getName());

	private ScheduledExecutorService scheduledExecutor;
	private ExecutorService executor;
	
	public ActivitiEngineJobExecutor(ScheduledExecutorService scheduledExecutor, ExecutorService executor) {
		LOGGER.log(Level.INFO, "Instanciating new WorkflowJobExecutor");
		this.scheduledExecutor = scheduledExecutor;
		this.executor = executor;
	}

	@Override
	protected void startExecutingJobs() {
		LOGGER.log(Level.INFO, "Scheduling start of JobAcquisitionThread (5s)");
		scheduledExecutor.schedule(acquireJobsRunnable, 5, TimeUnit.SECONDS);
	}

	@Override
	protected void stopExecutingJobs() {
		LOGGER.log(Level.INFO, "Stopping JobAcquisitionThread...");
		acquireJobsRunnable.stop();
		LOGGER.log(Level.INFO, "JobAcquisitionThread stopped");
	}

	@Override
	public void executeJobs(List<String> jobIds) {
		try {
			LOGGER.log(Level.INFO, "Using executor to execute jobs");
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