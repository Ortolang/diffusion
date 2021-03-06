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
	
	String SERVICE_NAME = "engine";
	
	void deployDefinitions(String[] resources) throws RuntimeEngineException;
	
	List<ProcessType> listProcessTypes(boolean onlyLatest) throws RuntimeEngineException;
	
	ProcessType getProcessTypeById(String id) throws RuntimeEngineException;
	
	ProcessType getProcessTypeByKey(String key) throws RuntimeEngineException;
	
	void startProcess(String type, String key, Map<String, Object> variables) throws RuntimeEngineException;
	
	void deleteProcess(String key) throws RuntimeEngineException;
	
	Process getProcess(String id) throws RuntimeEngineException;
	
	Map<String, Object> listProcessVariables(String id) throws RuntimeEngineException;
	
	List<Process> findProcess(Map<String, Object> variables) throws RuntimeEngineException;
	
	HumanTask getTask(String id) throws RuntimeEngineException;
	
	List<HumanTask> listAllTasks() throws RuntimeEngineException;
	
	boolean isCandidate(String id, String user, List<String> groups) throws RuntimeEngineException;
	
	List<HumanTask> listCandidateTasks(String user, List<String> groups) throws RuntimeEngineException;
	
	boolean isAssigned(String id, String user) throws RuntimeEngineException;
	
	List<HumanTask> listAllUnassignedTasks() throws RuntimeEngineException;

	List<HumanTask> listAssignedTasks(String user) throws RuntimeEngineException;
	
	void claimTask(String id, String user) throws RuntimeEngineException;
	
	void unclaimTask(String id) throws RuntimeEngineException;
	
	void completeTask(String id, Map<String, Object> variables) throws RuntimeEngineException;
	
	void notify(RuntimeEngineEvent event) throws RuntimeEngineException;
	
}
