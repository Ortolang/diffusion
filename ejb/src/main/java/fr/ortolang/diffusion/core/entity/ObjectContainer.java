package fr.ortolang.diffusion.core.entity;

import java.util.HashMap;
import java.util.Map;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@SuppressWarnings("serial")
public class ObjectContainer extends OrtolangObject {

	public static final String OBJECT_TYPE = "container";

	private String id;
	private String key;
	private String name;
	private Map<String, String> streams;
	
	public ObjectContainer() {
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
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, ObjectContainer.OBJECT_TYPE, id);
	}

}
