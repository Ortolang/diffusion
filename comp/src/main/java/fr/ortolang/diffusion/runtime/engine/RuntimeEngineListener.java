package fr.ortolang.diffusion.runtime.engine;

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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.entity.Process.State;

public class RuntimeEngineListener {

	private static final Logger LOGGER = Logger.getLogger(RuntimeEngineListener.class.getName());

	private RuntimeService runtime;

	public RuntimeService getRuntimeService() throws RuntimeEngineException {
		try {
			if (runtime == null) {
				runtime = (RuntimeService) OrtolangServiceLocator.findService(RuntimeService.SERVICE_NAME);
			}
			return runtime;
		} catch (Exception e) {
			throw new RuntimeEngineException(e);
		}
	}

	public void onEvent(RuntimeEngineEvent event) {
		LOGGER.log(Level.FINE, "RuntimeEngineEvent type: " + event.getType() + " pid: " + event.getPid() + " activityName: " + event.getActivityName());
		try {
		    switch (event.getType()) {
                case PROCESS_START:
                    getRuntimeService().updateProcessState(event.getPid(), State.RUNNING);
                    break;
                case PROCESS_ABORT:
                    getRuntimeService().updateProcessState(event.getPid(), State.ABORTED);
                    break;
                case PROCESS_COMPLETE:
                    getRuntimeService().updateProcessState(event.getPid(), State.COMPLETED);
                    break;
                case PROCESS_LOG:
                    getRuntimeService().appendProcessLog(event.getPid(), new Date(event.getTimestamp())  + "  " + event.getMessage());
                    break;
                case PROCESS_TRACE:
                    getRuntimeService().appendProcessTrace(event.getPid(), new Date(event.getTimestamp())  + "  " + event.getMessage() + "\r\n" + event.getTrace());
                    break;
                case PROCESS_UPDATE_STATUS:
                    getRuntimeService().updateProcessStatus(event.getPid(), event.getStatus(), event.getExplanation());
                    break;
                case PROCESS_ACTIVITY_STARTED:
                    getRuntimeService().updateProcessActivity(event.getPid(), "[STARTED] " + event.getActivityName(), 0);
                    getRuntimeService().appendProcessLog(event.getPid(), new Date(event.getTimestamp())  + "  " + event.getMessage());
                    break;
                case PROCESS_ACTIVITY_ERROR:
                    getRuntimeService().updateProcessActivity(event.getPid(), "[ERROR] " + event.getActivityName(), 0);
                    getRuntimeService().appendProcessLog(event.getPid(), new Date(event.getTimestamp())  + "  " + event.getMessage());
                    break;
                case PROCESS_ACTIVITY_PROGRESS:
                    getRuntimeService().updateProcessActivity(event.getPid(), "[IN PROGRESS]" + event.getActivityName(), event.getActivityProgress());
                    getRuntimeService().appendProcessLog(event.getPid(), new Date(event.getTimestamp())  + "  " + event.getMessage());
                    break;
                case PROCESS_ACTIVITY_COMPLETED:
                    getRuntimeService().updateProcessActivity(event.getPid(), "[COMPLETED] " + event.getActivityName(), 100);
                    getRuntimeService().appendProcessLog(event.getPid(), new Date(event.getTimestamp())  + "  " + event.getMessage());
                    break;
                case TASK_CREATED:
                    getRuntimeService().updateProcessActivity(event.getPid(), "[CREATED] Human Task '" + event.getActivityName() + "' (#" + event.getTid() + ")", 0);
                    getRuntimeService().appendProcessLog(event.getPid(), new Date(event.getTimestamp())  + "  HUMAN TASK (#" + event.getTid() + ") " + event.getActivityName() + " CREATED");
                    getRuntimeService().pushTaskEvent(event.getPid(), event.getTid(), event.getCandidates(), event.getAssignee(), event.getType());
                    break;
                case TASK_ASSIGNED:
                    getRuntimeService().updateProcessActivity(event.getPid(), "[ASSIGNED] Human Task '" + event.getActivityName() + "' (#" + event.getTid() + ")", 10);
                    getRuntimeService().appendProcessLog(event.getPid(), new Date(event.getTimestamp())  + "  HUMAN TASK (#" + event.getTid() + ") " + event.getActivityName() + " ASSIGNED TO: " + event.getAssignee());
                    getRuntimeService().pushTaskEvent(event.getPid(), event.getTid(), event.getCandidates(), event.getAssignee(), event.getType());
                    break;
                case TASK_COMPLETED:
                    getRuntimeService().updateProcessActivity(event.getPid(), "[COMPLETED] Human Task '" + event.getActivityName() + "' (#" + event.getTid() + ")", 100);
                    getRuntimeService().appendProcessLog(event.getPid(), new Date(event.getTimestamp())  + "  HUMAN TASK (#" + event.getTid() + ") " + event.getActivityName() + " COMPLETED");
                    getRuntimeService().pushTaskEvent(event.getPid(), event.getTid(), event.getCandidates(), event.getAssignee(), event.getType());
                    break;
            }
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unexpected error while trying to treat runtime  event", e);
		}
	}

}