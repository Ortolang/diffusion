package fr.ortolang.diffusion.core.entity;

import java.util.HashMap;
import java.util.Map;

import fr.ortolang.diffusion.DiffusionObject;
import fr.ortolang.diffusion.DiffusionObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

public class ObjectContainer extends DiffusionObject {

	public static final String OBJECT_TYPE = "container";

	private String id;
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
	public String getDiffusionObjectName() {
		return getName();
	}

	@Override
	public DiffusionObjectIdentifier getDiffusionObjectIdentifier() {
		return new DiffusionObjectIdentifier(CoreService.SERVICE_NAME, ObjectContainer.OBJECT_TYPE, id);
	}

}
