package fr.ortolang.diffusion.core.entity;

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
	
	@Id
	private String id;
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
	
	public Set<MetadataElement> getMetadatas() {
		Set<MetadataElement> metadatas = new HashSet<MetadataElement>();
		if ( metadatasContent != null && metadatasContent.length() > 0 ) {
			for ( String metadata : Arrays.asList(metadatasContent.split("\n")) ) {
				metadatas.add(MetadataElement.deserialize(metadata));
			}
		}
		return metadatas;
	}
	
	public void setMetadatas(Set<MetadataElement> metadatas) {
		StringBuffer newmetadatas = new StringBuffer();
		for ( MetadataElement metadata : metadatas ) {
			if ( newmetadatas.length() > 0 ) {
				newmetadatas.append("\n");
			}
			newmetadatas.append(metadata.serialize());
		}
		metadatasContent = newmetadatas.toString();
	}
	
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
	
	public boolean containsMetadata(MetadataElement metadata) {
		if ( metadatasContent.length() > 0 && metadatasContent.indexOf(metadata.serialize()) != -1 ) {
			return true;
		}
		return false;
	}
	
	public boolean containsMetadataName(String name) {
		if ( metadatasContent.indexOf(name + "/") != -1 ) {
			return true;
		}
		return false;
	}
	
	public boolean containsMetadataKey(String key) {
		if ( metadatasContent.indexOf("/" + key) != -1 ) {
			return true;
		}
		return false;
	}
	
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
