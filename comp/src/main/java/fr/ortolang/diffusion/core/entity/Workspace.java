package fr.ortolang.diffusion.core.entity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@Entity
@NamedQueries(value= {
		@NamedQuery(name="findWorkspaceByMember", query="select w from Workspace w where w.members IN :groups")
})
@SuppressWarnings("serial")
public class Workspace extends OrtolangObject {

	public static final String OBJECT_TYPE = "workspace";
	
	@Id
	private String id;
	@Transient
	private String key;
	private String type;
	private String name;
	private String head;
	private int clock;
	private boolean changed;
	private String members;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String snapshotsContent = "";
	
	public Workspace() {
		clock = 1;
		changed = false;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public boolean hasChanged() {
		return changed;
	}
	
	public void setChanged(boolean changed) {
		this.changed = changed;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getHead() {
		return head;
	}

	public void setHead(String head) {
		this.head = head;
	}
	
	public int getClock() {
		return clock;
	}
	
	public void incrementClock() {
		this.clock++;
	}
	
	public void setSnapshotsContent(String snapshotsContent) {
		this.snapshotsContent = snapshotsContent;
	}
	
	public String getSnapshotsContent() {
		return snapshotsContent;
	}
	
	public Set<SnapshotElement> getSnapshots() {
		Set<SnapshotElement> snapshots = new HashSet<SnapshotElement>();
		if ( snapshotsContent != null && snapshotsContent.length() > 0 ) {
			for ( String snapshot : Arrays.asList(snapshotsContent.split("\n")) ) {
				snapshots.add(SnapshotElement.deserialize(snapshot));
			}
		}
		return snapshots;
	}
	
	public void setSnapshots(Set<SnapshotElement> snapshots) {
		StringBuffer newsnapshots = new StringBuffer();
		for ( SnapshotElement snapshot : snapshots ) {
			if ( newsnapshots.length() > 0 ) {
				newsnapshots.append("\n");
			}
			newsnapshots.append(snapshot.serialize());
		}
		snapshotsContent = newsnapshots.toString();
	}
	
	public boolean addSnapshot(SnapshotElement snapshot) {
		if ( !containsSnapshot(snapshot) ) {
			if ( snapshotsContent.length() > 0 ) {
				snapshotsContent += "\n" + snapshot.serialize();
			} else {
				snapshotsContent = snapshot.serialize();
			}
			return true;
		}
		return false;
	}
	
	public boolean removeSnapshot(SnapshotElement snapshot) {
		if ( containsSnapshot(snapshot) ) {
			snapshotsContent = snapshotsContent.replaceAll("(?m)^(" + snapshot.serialize() + ")\n?", "");
			if ( snapshotsContent.endsWith("\n") ) {
				snapshotsContent = snapshotsContent.substring(0, snapshotsContent.length()-1);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean containsSnapshot(SnapshotElement snapshot) {
		if ( snapshotsContent.length() > 0 && snapshotsContent.indexOf(snapshot.serialize()) != -1 ) {
			return true;
		}
		return false;
	}
	
	public boolean containsSnapshotName(String name) {
		if ( snapshotsContent.indexOf(name + "/") != -1 ) {
			return true;
		}
		return false;
	}
	
	public boolean containsSnapshotKey(String key) {
		if ( snapshotsContent.indexOf("/" + key) != -1 ) {
			return true;
		}
		return false;
	}
	
	public SnapshotElement findSnapshotByName(String name) {
		Pattern pattern = Pattern.compile("(?s).*(" + name + "/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})).*$");
		Matcher matcher = pattern.matcher(snapshotsContent);
		if ( matcher.matches() ) {
			return SnapshotElement.deserialize(matcher.group(1));
		}
		return null;
	}

	public String getMembers() {
		return members;
	}

	public void setMembers(String members) {
		this.members = members;
	}
	
	@Override
	public String getObjectName() {
		return name;
	}

	@Override
	public String getObjectKey() {
		return key;
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, id);
	}

	

}
