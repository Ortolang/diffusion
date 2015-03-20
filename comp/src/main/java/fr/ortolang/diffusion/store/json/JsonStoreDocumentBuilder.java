package fr.ortolang.diffusion.store.json;

import java.io.IOException;

import com.orientechnologies.orient.core.record.impl.ODocument;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.core.entity.MetadataObject;

public class JsonStoreDocumentBuilder {

	public static final String KEY_PROPERTY = "key";
	public static final String STATUS_PROPERTY = "status";
	public static final String META_PROPERTY = "meta";

	public static ODocument buildDocument(OrtolangIndexableObject object) {
		return buildDocument(object, null);
	}
	
	public static ODocument buildDocument(OrtolangIndexableObject object, ODocument oldDoc) {
		
		ODocument doc = null;
		if(oldDoc!=null) {
			doc = oldDoc;
		} else {
			doc = new ODocument(object.getType());
		}
		
		doc.field(KEY_PROPERTY, object.getKey());
		doc.field(STATUS_PROPERTY, object.getStatus());
		
		if(object.getJsonContent()!=null && object.getJsonContent().getStream()!=null) {
			try {
				doc.field(META_PROPERTY, new ODocument(MetadataObject.OBJECT_TYPE).fromJSON(object.getJsonContent().getStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return doc;
	}
}