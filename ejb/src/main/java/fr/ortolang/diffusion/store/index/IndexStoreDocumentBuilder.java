package fr.ortolang.diffusion.store.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangObjectProperty;

public class IndexStoreDocumentBuilder {
	
	public static final String IDENTIFIER_FIELD = "IDENTIFIER";
	public static final String SERVICE_FIELD = "SERVICE";
	public static final String TYPE_FIELD = "TYPE";
	public static final String KEY_FIELD = "KEY";
	public static final String NAME_FIELD = "NAME";
	public static final String VISIBLITY_FIELD = "VISIBILITY";
	public static final String LOCK_FIELD = "LOCK";
	public static final String STATUS_FIELD = "STATUS";
	public static final String CONTENT_FIELD = "CONTENT";
	public static final String PROPERTY_FIELD_PREFIX = "PROPERTY.";
	
	public static Document buildDocument(OrtolangIndexableObject object) {
		Document document = new Document();
		document.add(new Field(IDENTIFIER_FIELD, object.getIdentifier().serialize(), TextField.TYPE_NOT_STORED));
		document.add(new Field(SERVICE_FIELD, object.getService(), StringField.TYPE_STORED));
		document.add(new Field(TYPE_FIELD, object.getType(), StringField.TYPE_STORED));
		document.add(new Field(KEY_FIELD, object.getKey(), StringField.TYPE_STORED));
		document.add(new Field(NAME_FIELD, object.getName(), TextField.TYPE_STORED));
		if ( object.isDeleted() || object.isHidden() ) {
			document.add(new Field(VISIBLITY_FIELD, "invisible", StringField.TYPE_STORED));
		} else {
			document.add(new Field(VISIBLITY_FIELD, "visible", StringField.TYPE_STORED));
		} 
		if ( object.isLocked() ) {
			document.add(new Field(LOCK_FIELD, "locked", StringField.TYPE_STORED));
		} else {
			document.add(new Field(LOCK_FIELD, "unlocked", StringField.TYPE_STORED));
		}
		for ( OrtolangObjectProperty prop : object.getProperties() ) {
			document.add(new Field(PROPERTY_FIELD_PREFIX + prop.getName().toUpperCase(), prop.getValue().toLowerCase(), StringField.TYPE_STORED));
		}
		document.add(new Field(STATUS_FIELD, object.getStatus().toLowerCase(), StringField.TYPE_STORED));
		document.add(new Field(CONTENT_FIELD, object.getContent().toString(), TextField.TYPE_STORED));
		return document;
	}

}
