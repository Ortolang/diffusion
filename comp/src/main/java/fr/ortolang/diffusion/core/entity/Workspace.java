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

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@NamedQueries(value= {
		@NamedQuery(name="findWorkspaceByMember", query="select w from Workspace w where w.members in :groups"),
		@NamedQuery(name="findWorkspaceByAlias", query="select w from Workspace w where w.alias = :alias"),
		@NamedQuery(name="listAllWorkspaceAlias", query="select w.alias from Workspace w where w.alias not like ''")
})
@SuppressWarnings("serial")
public class Workspace extends OrtolangObject {

	public static final String OBJECT_TYPE = "workspace";
	public static final String HEAD = "head";
	public static final String LATEST = "latest";
	
	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	@Column(unique=true)
	private String alias;
	private String type;
	private String name;
	private String head;
	private int clock;
	private boolean changed;
	private boolean readOnly;
	private String members;
	private String privileged;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String snapshotsContent = "";
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String tagsContent = "";
	
	public Workspace() {
		clock = 1;
		changed = false;
		readOnly = false;
		alias = "";
		privileged = "";
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

	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
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
	
	public boolean isReadOnly() {
	    return readOnly;
	}
	
	public void setReadOnly(boolean readOnly) {
	    this.readOnly = readOnly;
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
	
	public boolean hasSnapshot() {
		return snapshotsContent != null && snapshotsContent.length() > 0;
	}
	
	public void setSnapshots(Set<SnapshotElement> snapshots) {
		StringBuilder newsnapshots = new StringBuilder();
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
		return snapshotsContent.length() > 0 && snapshotsContent.contains(snapshot.serialize());
	}
	
	public boolean containsSnapshotName(String name) {
		return snapshotsContent.contains(name + "/");
	}
	
	public boolean containsSnapshotKey(String key) {
		return snapshotsContent.contains("/" + key);
	}
	
	public SnapshotElement findSnapshotByName(String name) {
		Pattern pattern = Pattern.compile("(?s).*(" + name + "/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})).*$");
		Matcher matcher = pattern.matcher(snapshotsContent);
		if ( matcher.matches() ) {
			return SnapshotElement.deserialize(matcher.group(1));
		}
		return null;
	}
	
	public SnapshotElement findSnapshotByKey(String key) {
		StringTokenizer tok = new StringTokenizer(snapshotsContent, "\r\n");
		while ( tok.hasMoreTokens() ) {
			String line = tok.nextToken();
			if ( line.endsWith("/" + key) ) {
				return SnapshotElement.deserialize(line);
			}
		}
		return null;
	}
	
	public void setTagsContent(String tagsContent) {
		this.tagsContent = tagsContent;
	}
	
	public String getTagsContent() {
		return tagsContent;
	}
	
	public Set<TagElement> getTags() {
		Set<TagElement> tags = new HashSet<TagElement>();
		if ( tagsContent != null && tagsContent.length() > 0 ) {
			for ( String tag : Arrays.asList(tagsContent.split("\n")) ) {
				tags.add(TagElement.deserialize(tag));
			}
		}
		return tags;
	}
	
	public void setTags(Set<TagElement> tags) {
		StringBuilder newtags = new StringBuilder();
		for ( TagElement tag : tags ) {
			if ( newtags.length() > 0 ) {
				newtags.append("\n");
			}
			newtags.append(tag.serialize());
		}
		tagsContent = newtags.toString();
	}
	
	public boolean addTag(TagElement tag) {
		if ( !containsTag(tag) ) {
			if ( tagsContent.length() > 0 ) {
				tagsContent += "\n" + tag.serialize();
			} else {
				tagsContent = tag.serialize();
			}
			return true;
		}
		return false;
	}
	
	public boolean removeTag(TagElement tag) {
		if ( containsTag(tag) ) {
			tagsContent = tagsContent.replaceAll("(?m)^(" + tag.serialize() + ")\n?", "");
			if ( tagsContent.endsWith("\n") ) {
				tagsContent = tagsContent.substring(0, tagsContent.length()-1);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean containsTag(TagElement tag) {
		return tagsContent.length() > 0 && tagsContent.contains(tag.serialize());
	}
	
	public boolean containsTagName(String name) {
		return findTagByName(name) != null;
	}
	
	public boolean containsTagSnapshot(String snapshot) {
		return tagsContent.contains("/" + snapshot);
	}
	
	public TagElement findTagByName(String name) {
		StringTokenizer tok = new StringTokenizer(tagsContent, "\r\n");
		String start = name + "/";
		while ( tok.hasMoreTokens() ) {
			String line = tok.nextToken();
			if ( line.startsWith(start) ) {
				return TagElement.deserialize(line);
			}
		}
		return null;
	}
	
	public TagElement findTagBySnapshot(String snapshot) {
		StringTokenizer tok = new StringTokenizer(tagsContent, "\r\n");
		String end = "/" + snapshot;
		while ( tok.hasMoreTokens() ) {
			String line = tok.nextToken();
			if ( line.endsWith(end) ) {
				return TagElement.deserialize(line);
			}
		}
		return null;
	}

	public String getMembers() {
		return members;
	}

	public void setMembers(String members) {
		this.members = members;
	}
	
	public String getPrivileged() {
        return privileged;
    }

    public void setPrivileged(String privileged) {
        this.privileged = privileged;
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
