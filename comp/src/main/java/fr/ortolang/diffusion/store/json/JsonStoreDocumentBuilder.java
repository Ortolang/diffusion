package fr.ortolang.diffusion.store.json;

import com.orientechnologies.orient.core.record.impl.ODocument;

import fr.ortolang.diffusion.OrtolangIndexableObject;

public class JsonStoreDocumentBuilder {

	public static final String KEY_PROPERTY = "key";
	public static final String TYPE_PROPERTY = "type";
	public static final String STATUS_PROPERTY = "status";
	public static final String NAME_PROPERTY = "name";

	public static ODocument buildDocument(OrtolangIndexableObject object) {
		
		ODocument doc = new ODocument("OrtolangObject");
		doc.field("name", object.getName());
		
		return doc;
	}
}
