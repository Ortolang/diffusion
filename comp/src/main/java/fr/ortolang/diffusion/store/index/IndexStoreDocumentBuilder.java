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

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import org.apache.lucene.document.*;

import static org.apache.lucene.document.Field.Store.YES;

public class IndexStoreDocumentBuilder {

    private IndexStoreDocumentBuilder() {
    }

    public static final String IDENTIFIER_FIELD = "IDENTIFIER";
    public static final String SERVICE_FIELD = "SERVICE";
    public static final String TYPE_FIELD = "TYPE";
    public static final String KEY_FIELD = "KEY";
    public static final String NAME_FIELD = "NAME";
    public static final String VISIBILITY_FIELD = "VISIBILITY";
    public static final String LOCK_FIELD = "LOCK";
    public static final String STATUS_FIELD = "STATUS";
    public static final String CONTENT_FIELD = "CONTENT";
    public static final String CONTENT_PROPERTY_FIELD_PREFIX = "CONTENT.PROPERTY.";
    public static final String PROPERTY_FIELD_PREFIX = "PROPERTY.";
    public static final String BOOST_FIELD = "BOOST";
    public static final String CONTEXT_ROOT_FIELD = "ROOT";
    public static final String CONTEXT_PATH_FIELD = "PATH";

    static Document buildDocument(OrtolangIndexableObject<IndexablePlainTextContent> object) {
        Document document = new Document();
        document.add(new StringField(IDENTIFIER_FIELD, object.getIdentifier().serialize(), YES));
        document.add(new StringField(SERVICE_FIELD, object.getService(), YES));
        document.add(new StringField(TYPE_FIELD, object.getType(), YES));
        document.add(new StringField(KEY_FIELD, object.getKey(), YES));
        if (object.getContent().getName().length() > 0) {
            document.add(new TextField(NAME_FIELD, object.getContent().getName(), YES));
        } else {
            document.add(new StringField(NAME_FIELD, object.getKey(), YES));
        }
        if (object.isHidden()) {
            document.add(new StringField(VISIBILITY_FIELD, "hidden", YES));
        } else {
            document.add(new StringField(VISIBILITY_FIELD, "visible", YES));
        }
        if (object.isLocked()) {
            document.add(new StringField(LOCK_FIELD, "locked", YES));
        } else {
            document.add(new StringField(LOCK_FIELD, "unlocked", YES));
        }
        for (OrtolangObjectProperty prop : object.getProperties()) {
            document.add(new StringField(PROPERTY_FIELD_PREFIX + prop.getName().toUpperCase(), prop.getValue().toLowerCase(), YES));
        }
        document.add(new StringField(STATUS_FIELD, object.getStatus().toLowerCase(), YES));
        document.add(new TextField(CONTENT_FIELD, object.getContent().toString(), YES));
        document.add(new NumericDocValuesField(BOOST_FIELD, object.getContent().getBoost()));
        for (IndexablePlainTextContentProperty property : object.getContent().getProperties()) {
            document.add(new Field(CONTENT_PROPERTY_FIELD_PREFIX + property.getName().toUpperCase(), property.getValue().toLowerCase(), property.getFieldType()));
        }
        return document;
    }

}
