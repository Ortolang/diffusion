package fr.ortolang.diffusion;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;
import fr.ortolang.diffusion.store.json.JsonStoreDocumentBuilder;
import fr.ortolang.diffusion.store.json.OrtolangKeyExtractor;

public class OrtolangIndexableObjectFactory {

    private OrtolangIndexableObjectFactory() {
    }

//    public static OrtolangIndexableObject<IndexablePlainTextContent> buildPlainTextIndexableObject(String key) throws OrtolangException, NotIndexableContentException {
//        try {
//            RegistryService registry = (RegistryService)OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME, RegistryService.class);
//            OrtolangObjectIdentifier identifier = registry.lookup(key);
//            OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
//            IndexablePlainTextContent content = service.getIndexablePlainTextContent(key);
//            OrtolangIndexableObject<IndexablePlainTextContent> object = new OrtolangIndexableObject<IndexablePlainTextContent>();
//            loadCommonIndexableObject(key, identifier, object);
//            object.setContent(content);
//            return object;
//        } catch (RegistryServiceException | KeyNotFoundException e) {
//            throw new OrtolangException("unable to get plain text indexable content for object cause : " + e.getMessage(), e);
//        }
//    }

    public static OrtolangIndexableObject<IndexableJsonContent> buildJsonIndexableObject(String key) throws OrtolangException, NotIndexableContentException {
        return buildJsonIndexableObject(key, new ArrayList<String>());
    }

    private static OrtolangIndexableObject<IndexableJsonContent> buildJsonIndexableObject(String key, List<String> keys) throws OrtolangException, NotIndexableContentException {
        try {
            if(keys.contains(key)) {
                throw new OrtolangException("Key "+key+" is already been injected in json content (Cycle detection)");
            } else {
                keys.add(key);
            }

            RegistryService registry = (RegistryService)OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME, RegistryService.class);
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());

            IndexableJsonContent content = service.getIndexableJsonContent(key);

            for(Map.Entry<String, String> entry : content.getStream().entrySet()) {
                String json = entry.getValue();
                List<String> ortolangKeys = OrtolangKeyExtractor.extractOrtolangKeys(json);
                for(String ortolangKey : ortolangKeys) {
                    List<String> newKeys = new ArrayList<String>(keys);

                    String jsonContent = JsonStoreDocumentBuilder.buildDocument(buildJsonIndexableObject(ortolangKey, newKeys));

                    if(jsonContent!=null) {
                        json = json.replace("\""+OrtolangKeyExtractor.getMarker(ortolangKey)+"\"", jsonContent);
                    } else {
                        throw new OrtolangException("cannot found ortolang key : " + ortolangKey);
                    }
                }
                entry.setValue(json);
            }

            OrtolangIndexableObject<IndexableJsonContent> object = new OrtolangIndexableObject<IndexableJsonContent>();
            loadCommonIndexableObject(key, identifier, object);
            object.setContent(content);
            return object;
        } catch (RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to get json indexable content for object cause :  " + e.getMessage(), e);
        }
    }

    private static void loadCommonIndexableObject(String key, OrtolangObjectIdentifier identifier, OrtolangIndexableObject<?> object) throws OrtolangException {
        try {
            RegistryService registry = (RegistryService)OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME, RegistryService.class);
            object.setKey(key);
            object.setIdentifier(identifier);
            object.setService(identifier.getService());
            object.setType(identifier.getType());
            object.setHidden(registry.isHidden(key));
            object.setStatus(registry.getPublicationStatus(key));
            object.setProperties(registry.getProperties(key));
            object.setAuthor(registry.getAuthor(key));
            object.setCreationDate(registry.getCreationDate(key));
            object.setLastModificationDate(registry.getLastModificationDate(key));
        } catch (Exception e) {
            throw new OrtolangException("unable to get json indexable content for object cause : " + e.getMessage(), e);
        }
    }

}
