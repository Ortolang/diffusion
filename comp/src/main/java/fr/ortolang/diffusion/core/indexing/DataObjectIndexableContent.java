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

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DataObjectIndexableContent extends MetadataSourceIndexableContent {

    private static final String[] DATA_OBJECT_MAPPING;

    private static final Logger LOGGER = Logger.getLogger(DataObjectIndexableContent.class.getName());

    static {
        DATA_OBJECT_MAPPING = Stream.concat(Arrays.stream(METADATA_SOURCE_MAPPING),
                Arrays.stream(new String[] {
                        "size",
                        "type=long",
                        "mimeType",
                        "type=string",
                        "content",
                        "type=string"
                }))
                .toArray(String[]::new);
    }

    public DataObjectIndexableContent(DataObject object) throws OrtolangException, NotIndexableContentException, RegistryServiceException, KeyNotFoundException {
        super(object, CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE);
        content.put("size", object.getSize());
        content.put("mimeType", object.getMimeType());
        BinaryStoreService binary = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME, BinaryStoreService.class);
        try {
            String extraction = binary.extract(object.getStream());
            if (!extraction.isEmpty()) {
                content.put("content", extraction);
            }
        } catch (BinaryStoreServiceException | DataNotFoundException e) {
            LOGGER.log(Level.FINE, "Cannot extract content of data object with key [" + object.getKey() + "]", e);
        }
        setContent(content);
    }

    @Override
    public Object[] getMapping() {
        return DATA_OBJECT_MAPPING;
    }
}
