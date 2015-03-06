package fr.ortolang.diffusion.store.json;

import java.io.IOException;

import com.orientechnologies.orient.core.record.impl.ODocument;

import fr.ortolang.diffusion.OrtolangIndexableObject;

public class JsonStoreDocumentBuilder {

	public static final String KEY_PROPERTY = "key";
	public static final String TYPE_PROPERTY = "type";
	public static final String STATUS_PROPERTY = "status";
	public static final String NAME_PROPERTY = "name";

	public static ODocument buildDocument(OrtolangIndexableObject object) {
		
		ODocument doc = new ODocument("OrtolangObject");

		doc.field("ortolang_key", object.getKey());
		doc.field("ortolang_status", object.getStatus());
		
		if(object.getJsonContent()!=null && object.getJsonContent().getStream()!=null) {
			try {
				doc.field("ortolang_meta", new ODocument("MetadataObject").fromJSON(object.getJsonContent().getStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return doc;
	}
}
