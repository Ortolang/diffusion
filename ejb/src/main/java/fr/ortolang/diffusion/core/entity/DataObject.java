package fr.ortolang.diffusion.core.entity;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@Entity
@NamedQueries({
	@NamedQuery(name="findObjectByBinaryHash", query="select o from DataObject o where :hash MEMBER OF o.streams")
})
@SuppressWarnings("serial")
public class DataObject extends OrtolangObject {

	public static final String OBJECT_TYPE = "object";

	@Id
	private String id;
	@Transient
	private String key;
	private String name;
	private String description;
	private long size;
	private String contentType;
	private String preview;
	private long nbReads;
	@ElementCollection
	private Map<String, String> streams;
	
	public DataObject() {
		streams = new HashMap<String, String>();
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getPreview() {
		return preview;
	}

	public void setPreview(String preview) {
		this.preview = preview;
	}

	public long getNbReads() {
		return nbReads;
	}

	public void setNbReads(long nbReads) {
		this.nbReads = nbReads;
	}

	public Map<String, String> getStreams() {
		return streams;
	}

	public void setStreams(Map<String, String> streams) {
		this.streams = streams;
	}
	
	public void addStream(String name, String hash) {
		this.streams.put(name, hash);
	}
	
	public void removeStream(String name) {
		this.streams.remove(name);
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
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, id);
	}

}
