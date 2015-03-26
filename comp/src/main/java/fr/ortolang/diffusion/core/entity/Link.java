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
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Type;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@Entity
@Table(indexes = {@Index(columnList="target")})
@NamedQueries({
	@NamedQuery(name="findLinksForTarget", query="select r from Link r where r.target = :target")
})
@SuppressWarnings("serial")
public class Link extends OrtolangObject implements MetadataSource {
	
	public static final String OBJECT_TYPE = "link";
	public static final String MIME_TYPE = "ortolang/link";
	
	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private String name;
	private String target;
	private int clock;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String metadatasContent = "";
	
	public Link() {
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public int getClock() {
		return clock;
	}
	
	public void setClock(int clock) {
		this.clock = clock;
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
	
	public String getTarget() {
		return target;
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	
	public void setMetadatasContent(String metadatasContent) {
		this.metadatasContent = metadatasContent;
	}
	
	public String getMetadatasContent() {
		return metadatasContent;
	}
	
	@Override
	public Set<MetadataElement> getMetadatas() {
		Set<MetadataElement> metadatas = new HashSet<MetadataElement>();
		if ( metadatasContent != null && metadatasContent.length() > 0 ) {
			for ( String metadata : Arrays.asList(metadatasContent.split("\n")) ) {
				metadatas.add(MetadataElement.deserialize(metadata));
			}
		}
		return metadatas;
	}
	
	@Override
	public void setMetadatas(Set<MetadataElement> metadatas) {
		StringBuilder newmetadatas = new StringBuilder();
		for ( MetadataElement metadata : metadatas ) {
			if ( newmetadatas.length() > 0 ) {
				newmetadatas.append("\n");
			}
			newmetadatas.append(metadata.serialize());
		}
		metadatasContent = newmetadatas.toString();
	}
	
	@Override
	public boolean addMetadata(MetadataElement metadata) {
		if ( !containsMetadata(metadata) ) {
			if ( metadatasContent.length() > 0 ) {
				metadatasContent += "\n" + metadata.serialize();
			} else {
				metadatasContent = metadata.serialize();
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean removeMetadata(MetadataElement metadata) {
		if ( containsMetadata(metadata) ) {
			metadatasContent = metadatasContent.replaceAll("(?m)^(" + metadata.serialize() + ")\n?", "");
			if ( metadatasContent.endsWith("\n") ) {
				metadatasContent = metadatasContent.substring(0, metadatasContent.length()-1);
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean containsMetadata(MetadataElement metadata) {
		if ( metadatasContent.length() > 0 && metadatasContent.indexOf(metadata.serialize()) != -1 ) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean containsMetadataName(String name) {
		if ( metadatasContent.indexOf(name + "/") != -1 ) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean containsMetadataKey(String key) {
		if ( metadatasContent.indexOf("/" + key) != -1 ) {
			return true;
		}
		return false;
	}
	
	@Override
	public MetadataElement findMetadataByName(String name) {
		Pattern pattern = Pattern.compile("(?s).*(" + name + "/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})).*$");
		Matcher matcher = pattern.matcher(metadatasContent);
		if ( matcher.matches() ) {
			return MetadataElement.deserialize(matcher.group(1));
		}
		return null;
	}
	
	@Override
	public String getObjectKey() {
		return getKey();
	}
	
	@Override
	public String getObjectName() {
		return getName();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, id);
	}

}
