package fr.ortolang.diffusion.store.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.orientechnologies.orient.core.record.impl.ODocument;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.core.entity.MetadataObject;

public class JsonStoreDocumentBuilder {

	private static final Logger LOGGER = Logger.getLogger(JsonStoreDocumentBuilder.class.getName());

	public static final String KEY_PROPERTY = "key";
	public static final String STATUS_PROPERTY = "status";
	public static final String META_PROPERTY = "meta";

	public static ODocument buildDocument(OrtolangIndexableObject<IndexableJsonContent> object) {
		return buildDocument(object, null);
	}

	public static ODocument buildDocument(OrtolangIndexableObject<IndexableJsonContent> object, ODocument oldDoc) {

		ODocument doc = null;
		if (oldDoc != null) {
			doc = oldDoc;
		} else {
			doc = new ODocument(object.getType());
		}

		doc.field(KEY_PROPERTY, object.getKey());
		doc.field(STATUS_PROPERTY, object.getStatus());

		if (object.getContent() != null && object.getContent().getStream() != null) {
			for(Map.Entry<String, InputStream> entry : object.getContent().getStream().entrySet()) {
				try {
					doc.field(META_PROPERTY+"_"+entry.getKey(), new ODocument(MetadataObject.OBJECT_TYPE).fromJSON(entry.getValue()));
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "unable to get object json content", e);
				}
			}
		}

		return doc;
	}
}
