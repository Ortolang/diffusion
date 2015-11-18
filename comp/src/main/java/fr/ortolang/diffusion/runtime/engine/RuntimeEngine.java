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

import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.runtime.entity.ProcessType;

public interface RuntimeEngine {
	
	public static final String SERVICE_NAME = "engine";
	
	public void deployDefinitions(String[] resources) throws RuntimeEngineException;
	
	public List<ProcessType> listProcessTypes(boolean onlyLatest) throws RuntimeEngineException;
	
	public ProcessType getProcessTypeById(String id) throws RuntimeEngineException;
	
	public ProcessType getProcessTypeByKey(String key) throws RuntimeEngineException;
	
	public void startProcess(String type, String key, Map<String, Object> variables) throws RuntimeEngineException;
	
	public void deleteProcess(String key) throws RuntimeEngineException;
	
	public Process getProcess(String id) throws RuntimeEngineException;
	
	public List<Process> findProcess(Map<String, Object> variables) throws RuntimeEngineException;
	
	public HumanTask getTask(String id) throws RuntimeEngineException;
	
	public List<HumanTask> listAllTasks() throws RuntimeEngineException;
	
	public boolean isCandidate(String id, String user, List<String> groups) throws RuntimeEngineException;
	
	public List<HumanTask> listCandidateTasks(String user, List<String> groups) throws RuntimeEngineException;
	
	public boolean isAssigned(String id, String user) throws RuntimeEngineException;

	public List<HumanTask> listAssignedTasks(String user) throws RuntimeEngineException;
	
	public void claimTask(String id, String user) throws RuntimeEngineException;
	
	public void unclaimTask(String id) throws RuntimeEngineException;
	
	public void completeTask(String id, Map<String, Object> variables) throws RuntimeEngineException;
	
	public void notify(RuntimeEngineEvent event) throws RuntimeEngineException;
	
}
