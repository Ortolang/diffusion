package fr.ortolang.diffusion.core.entity;

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
import javax.persistence.Version;

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
	public static final String HEAD = "head";
	
	@Id
	private String id;
	@Version
	private long version;
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
