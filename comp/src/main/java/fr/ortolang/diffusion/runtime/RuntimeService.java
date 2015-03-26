package fr.ortolang.diffusion.runtime;

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
import java.util.Map;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.runtime.entity.Process.State;
import fr.ortolang.diffusion.runtime.entity.ProcessType;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface RuntimeService extends OrtolangService {

	public static final String SERVICE_NAME = "runtime";
	
	public void importProcessTypes() throws RuntimeServiceException;
	
	public List<ProcessType> listProcessTypes() throws RuntimeServiceException;
	
	/* Process */
	
	public void createProcess(String key, String type, String name) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException;

	public void startProcess(String key, Map<String, Object> variables) throws RuntimeServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public void updateProcessState(String pid, State state) throws RuntimeServiceException;
	
	public void appendProcessLog(String pid, String log) throws RuntimeServiceException;
	
	public void updateProcessActivity(String pid, String name) throws RuntimeServiceException;
	
	public List<Process> listProcesses(State sate) throws RuntimeServiceException, AccessDeniedException;

	public Process readProcess(String key) throws RuntimeServiceException, KeyNotFoundException, AccessDeniedException;
	
	/* Task */

	public List<HumanTask> listCandidateTasks() throws RuntimeServiceException;

	public List<HumanTask> listAssignedTasks() throws RuntimeServiceException;

	public void claimTask(String id) throws RuntimeServiceException;

	public void completeTask(String id, Map<String, Object> variables) throws RuntimeServiceException;
	
}
