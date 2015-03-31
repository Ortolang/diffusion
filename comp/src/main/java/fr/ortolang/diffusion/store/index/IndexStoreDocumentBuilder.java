package fr.ortolang.diffusion.store.index;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
	public static final String CONTEXT_ROOT_FIELD = "ROOT";
	public static final String CONTEXT_PATH_FIELD = "PATH";
	
	public static Document buildDocument(OrtolangIndexableObject<IndexablePlainTextContent> object) {
		Document document = new Document();
		document.add(new Field(IDENTIFIER_FIELD, object.getIdentifier().serialize(), TextField.TYPE_STORED));
		document.add(new Field(SERVICE_FIELD, object.getService(), StringField.TYPE_STORED));
		document.add(new Field(TYPE_FIELD, object.getType(), StringField.TYPE_STORED));
		document.add(new Field(KEY_FIELD, object.getKey(), StringField.TYPE_STORED));
		if ( object.isHidden() ) {
			document.add(new Field(VISIBLITY_FIELD, "hidden", StringField.TYPE_STORED));
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
