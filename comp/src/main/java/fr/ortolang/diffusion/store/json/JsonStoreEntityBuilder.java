package fr.ortolang.diffusion.store.json;

import nl.renarj.jasdb.api.SimpleEntity;

import fr.ortolang.diffusion.OrtolangIndexableObject;

public class JsonStoreEntityBuilder {


	public static SimpleEntity buildEntity(OrtolangIndexableObject object) {
		SimpleEntity entity = new SimpleEntity();
		entity.addProperty("title", "Title of my content");
		entity.addProperty("text", "Some big piece of text content");
		
		entity.setInternalId(object.getKey());
		
		return entity;
	}
}
