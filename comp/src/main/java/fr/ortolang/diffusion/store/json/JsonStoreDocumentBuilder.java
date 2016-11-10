package fr.ortolang.diffusion.store.json;

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

import java.io.StringReader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import fr.ortolang.diffusion.OrtolangIndexableObject;

public class JsonStoreDocumentBuilder {

    private JsonStoreDocumentBuilder() {
    }

    private static final Logger LOGGER = Logger.getLogger(JsonStoreDocumentBuilder.class.getName());

    private static final String KEY_PROPERTY = "key";
    private static final String STATUS_PROPERTY = "status";
    private static final String META_PROPERTY = "meta";
    private static final String LAST_MODIFICATION_DATE_PROPERTY = "lastModificationDate";

    public static String buildDocument(OrtolangIndexableObject<IndexableJsonContent> object) {

        JsonObjectBuilder builder = Json.createObjectBuilder();

        builder.add(KEY_PROPERTY, object.getKey());
        builder.add(STATUS_PROPERTY, object.getStatus());
        builder.add(LAST_MODIFICATION_DATE_PROPERTY, object.getLastModificationDate());

        if (object.getContent() != null && object.getContent().getStream() != null) {
            for (Map.Entry<String, String> entry : object.getContent().getStream().entrySet()) {

                StringReader reader = new StringReader(entry.getValue());
                JsonReader jsonReader = Json.createReader(reader);
                try {
                    JsonObject jsonObj = jsonReader.readObject();
                    builder.add(META_PROPERTY + "_" + entry.getKey(), jsonObj);
                } catch(NullPointerException | ClassCastException e) {
                    LOGGER.log(Level.WARNING, "Cannot add json object to document", e);
                } finally {
                    jsonReader.close();
                    reader.close();
                }
            }
        }

        return builder.build().toString();
    }
}
