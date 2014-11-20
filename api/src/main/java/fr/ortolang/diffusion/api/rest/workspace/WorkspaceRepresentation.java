package fr.ortolang.diffusion.api.rest.workspace;

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
