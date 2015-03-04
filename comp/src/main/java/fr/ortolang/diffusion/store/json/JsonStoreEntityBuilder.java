package fr.ortolang.diffusion.store.json;

import nl.renarj.jasdb.api.SimpleEntity;
import fr.ortolang.diffusion.OrtolangIndexableObject;

public class JsonStoreEntityBuilder {

	public static final String KEY_PROPERTY = "key";
	public static final String TYPE_PROPERTY = "type";
	public static final String STATUS_PROPERTY = "status";
	public static final String NAME_PROPERTY = "name";

	public static SimpleEntity buildEntity(OrtolangIndexableObject object) {
		SimpleEntity entity = new SimpleEntity();
		entity.addProperty(KEY_PROPERTY, object.getName());
		entity.addProperty(TYPE_PROPERTY, object.getType());
		entity.addProperty(STATUS_PROPERTY, object.getStatus());
		entity.addProperty(NAME_PROPERTY, object.getContext().getName());
		
		entity.setInternalId(object.getKey());
		
		return entity;
	}
}
