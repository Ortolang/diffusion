package fr.ortolang.diffusion.referential.indexing;

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

import fr.ortolang.diffusion.indexing.IndexableContentParsingException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ReferentialEntityIndexableContent extends OrtolangIndexableContent {

    private static final Logger LOGGER = Logger.getLogger(ReferentialEntityIndexableContent.class.getName());

    protected Map<String, Object> content;

    public ReferentialEntityIndexableContent(ReferentialEntity entity) throws IndexableContentParsingException {
        super(ReferentialService.SERVICE_NAME, entity.getType().name().toLowerCase(), entity.getObjectKey());
//        entityType = entity.getType();
        content = new HashMap<>();
        
        // Copies referential to content
        JSONObject jsonObject = new JSONObject(entity.getContent());
        Iterator iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof String && value.toString().isEmpty()) {
                LOGGER.log(Level.WARNING, "Found empty property [" + key + "] in referential entity [" + entity.getKey() + "]");
                iterator.remove();
            } else {
            	content.put(key, value);
            }
        }
        
        // Encodes referential to content
        String referentialEntityContent = entity.getContent();
    	try {
    		content.put("content", URLEncoder.encode(referentialEntityContent, "UTF-8"));
		} catch (JSONException | UnsupportedEncodingException e) {
			LOGGER.log(Level.WARNING, "Unable to encode content", e);
		}

        content.put("key", entity.getKey());
        content.put("boost", entity.getBoost());
        setContent(content);
    }
}
