package fr.ortolang.diffusion.core.indexing;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MetadataObjectIndexableContent extends OrtolangObjectIndexableContent {

    private static final String[] METADATA_OBJECT_MAPPING;

    private static final Logger LOGGER = Logger.getLogger(MetadataObjectIndexableContent.class.getName());

    private static final String[] UNUSED_KEYS;

    static {
        METADATA_OBJECT_MAPPING = Stream.concat(Arrays.stream(ORTOLANG_OBJECT_MAPPING),
                Arrays.stream(new String[] {
                        "target",
                        "type=keyword",
                        "targetType",
                        "type=keyword",
                        "size",
                        "type=long",
                        "mimeType",
                        "type=string",
                        "content",
                        "type=string",
                        "extraction",
                        "type=object",
                        "format",
                        "type=keyword"
                }))
                .toArray(String[]::new);

        UNUSED_KEYS = new String[] {
                "PLTE PLTEEntry",
                "Chroma Palette PaletteEntry",
                "tRNS tRNS_Palette tRNS_PaletteEntry",
                "Text TextEntry",
                "tEXt tEXtEntry",
                "iTXt iTXtEntry",
                "xmpMM:DocumentID",
                "xmpMM:DerivedFrom:DocumentID",
                "xmpMM:DerivedFrom:InstanceID",
                "xmpMM:History:Action",
                "xmpMM:History:When",
                "xmpMM:History:SoftwareAgent",
                "Component 1",
                "Component 2",
                "Component 3"
        };
    }

    public MetadataObjectIndexableContent(MetadataObject metadata) throws IndexingServiceException, OrtolangException, KeyNotFoundException, RegistryServiceException {
        super(metadata, CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE);
        RegistryService registry = (RegistryService) OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME, RegistryService.class);
        OrtolangObjectIdentifier objectIdentifier = registry.lookup(metadata.getTarget());
        content.put("target", metadata.getTarget());
        content.put("targetType", objectIdentifier.getType());
        content.put("size", metadata.getSize());
        content.put("mimeType", metadata.getContentType());
        if (metadata.getName().startsWith("system-x-")) {
            BinaryStoreService binary = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME, BinaryStoreService.class);
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map extraction = mapper.readValue(binary.getFile(metadata.getStream()), Map.class);
                for (String key : UNUSED_KEYS) {
                    extraction.remove(key);
                }
                content.put("extraction", extraction);
            } catch (IOException | BinaryStoreServiceException | DataNotFoundException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
        }
        setContent(content);
    }

//    @Override
//    public Object[] getMapping() {
//        return METADATA_OBJECT_MAPPING;
//    }
}
