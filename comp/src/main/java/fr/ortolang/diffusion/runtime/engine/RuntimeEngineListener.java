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

import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.Process.State;

public class RuntimeEngineListener {

	private static final Logger logger = Logger.getLogger(RuntimeEngineListener.class.getName());

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
		logger.log(Level.FINE, "RuntimeEngineEvent type: " + event.getType());
		try {
			if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_START)) {
				getRuntimeService().updateProcessState(event.getPid(), State.RUNNING);
			}
			if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ABORT)) {
				getRuntimeService().updateProcessState(event.getPid(), State.ABORTED);
			}
			if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_COMPLETE)) {
				getRuntimeService().updateProcessState(event.getPid(), State.COMPLETED);
			}
			if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_LOG)) {
				getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
			}
			if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ACTIVITY_STARTED)) {
				getRuntimeService().updateProcessActivity(event.getPid(), event.getActivityName());
				getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
			}
			if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ACTIVITY_ERROR)) {
				getRuntimeService().updateProcessActivity(event.getPid(), "");
				getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
			}
			if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ACTIVITY_PROGRESS)) {
				// TODO Increase RuntimeService Process model to include activity progression field
				getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
			}
			if (event.getType().equals(RuntimeEngineEvent.Type.PROCESS_ACTIVITY_COMPLETED)) {
				getRuntimeService().updateProcessActivity(event.getPid(), "");
				getRuntimeService().appendProcessLog(event.getPid(), event.getMessage());
			}
		} catch (RuntimeServiceException | RuntimeEngineException e) {
			logger.log(Level.SEVERE, "unexpected error while trying to treat runtime  event", e);
		}
	}

}