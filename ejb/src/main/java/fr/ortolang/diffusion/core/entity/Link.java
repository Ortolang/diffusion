package fr.ortolang.diffusion.core.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@Entity
@Table(indexes = {@Index(columnList="target")})
@NamedQueries({
	@NamedQuery(name="findLinksForTarget", query="select r from Link r where r.target = :target")
})
@SuppressWarnings("serial")
public class Link extends OrtolangObject {
	
	public static final String OBJECT_TYPE = "link";
	
	@Id
	private String id;
	@Transient
	private String key;
	private String name;
	private String target;
	private boolean dynamic;
	@ElementCollection(fetch=FetchType.EAGER)
	private Set<String> metadatas;
	
	public Link() {
		metadatas = new HashSet<String>();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public boolean isDynamic() {
		return dynamic;
	}
	
	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
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
	
	public void setMetadatas(Set<String> metadatas) {
		this.metadatas = metadatas;
	}
	
	public Set<String> getMetadatas() {
		return metadatas;
	}
	
	public void addMetadata(String metadata) {
		this.metadatas.add(metadata);
	}
	
	public void removeMetadata(String metadata) {
		this.metadatas.remove(metadata);
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
