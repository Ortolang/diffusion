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
import fr.ortolang.diffusion.core.entity.MetadataSource;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

abstract class MetadataSourceIndexableContent extends OrtolangObjectIndexableContent {

    static final String[] METADATA_SOURCE_MAPPING;

    static {
        METADATA_SOURCE_MAPPING = Stream.concat(Arrays.stream(ORTOLANG_OBJECT_MAPPING),
                Arrays.stream(new String[] {
                        "version",
                        "type=long,index=no",
                        "clock",
                        "type=integer,index=no",
                        "metadata",
                        "type=object",
                        "workspace",
                        "type=object"
                }))
                .toArray(String[]::new);
    }

    MetadataSourceIndexableContent(MetadataSource object, String index, String type) throws IndexingServiceException {
        super(object, index, type);
        try {
            RegistryService registry = (RegistryService) OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME, RegistryService.class);
            if (registry.hasProperty(object.getKey(), CoreService.WORKSPACE_REGISTRY_PROPERTY_KEY)) {
                Map<String, String> map = new HashMap<>(1);
                map.put("key", registry.getProperty(object.getKey(), CoreService.WORKSPACE_REGISTRY_PROPERTY_KEY));
                content.put("workspace", map);
            }
            content.put("version", object.getVersion());
            content.put("clock", object.getClock());
        } catch (RegistryServiceException | PropertyNotFoundException | OrtolangException | KeyNotFoundException e) {
            throw new IndexingServiceException(e);
        }
    }
}
