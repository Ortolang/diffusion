package fr.ortolang.diffusion.store.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import fr.ortolang.diffusion.OrtolangIndexableObject;

public class IndexStoreDocumentBuilder {
	
	public static Document buildDocument(OrtolangIndexableObject object) {
		Document document = new Document();
		document.add(new Field("IDENTIFIER", object.getIdentifier().serialize(), TextField.TYPE_NOT_STORED));
		document.add(new Field("SERVICE", object.getService(), StringField.TYPE_STORED));
		document.add(new Field("TYPE", object.getType(), StringField.TYPE_STORED));
		document.add(new Field("KEY", object.getKey(), StringField.TYPE_STORED));
		document.add(new Field("NAME", object.getName(), TextField.TYPE_STORED));
		document.add(new Field("CONTENT", object.getContent().toString(), TextField.TYPE_STORED));
		return document;
	}

}
