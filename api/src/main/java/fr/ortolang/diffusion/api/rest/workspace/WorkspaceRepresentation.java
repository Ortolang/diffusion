package fr.ortolang.diffusion.api.rest.workspace;

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

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.Workspace;

@XmlRootElement(name = "workspace")
public class WorkspaceRepresentation {

	@XmlAttribute
	private String key;
	private String name = "No Name Provided";
	private String type = "default";
	private int clock;
	private String members;
	private String head;
	private boolean changed;
	private Set<SnapshotElement> snapshots;

	public WorkspaceRepresentation() {
		snapshots = new HashSet<SnapshotElement>();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getClock() {
		return clock;
	}

	public void setClock(int clock) {
		this.clock = clock;
	}

	public String getMembers() {
		return members;
	}

	public void setMembers(String members) {
		this.members = members;
	}

	public String getHead() {
		return head;
	}

	public void setHead(String head) {
		this.head = head;
	}

	public Set<SnapshotElement> getSnapshots() {
		return snapshots;
	}

	public void setSnapshots(Set<SnapshotElement> snapshots) {
		this.snapshots = snapshots;
	}
	
	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	public static WorkspaceRepresentation fromWorkspace(Workspace workspace) {
		WorkspaceRepresentation representation = new WorkspaceRepresentation();
		representation.setKey(workspace.getKey());
		representation.setName(workspace.getName());
		representation.setType(workspace.getType());
		representation.setClock(workspace.getClock());
		representation.setHead(workspace.getHead());
		representation.setMembers(workspace.getMembers());
		representation.setChanged(workspace.hasChanged());
		representation.setSnapshots(workspace.getSnapshots());
		return representation;
	}
}
