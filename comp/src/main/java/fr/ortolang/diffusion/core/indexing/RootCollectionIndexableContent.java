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
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public class RootCollectionIndexableContent extends MetadataSourceIndexableContent {

    private static final String[] ROOT_COLLECTION_MAPPING;

    private static final String TYPE = "root";

    static {
        ROOT_COLLECTION_MAPPING = Stream.concat(Arrays.stream(METADATA_SOURCE_MAPPING),
                Arrays.stream(new String[] {
                        "registry",
                        "type=object"
                }))
                .toArray(String[]::new);
    }

    public RootCollectionIndexableContent(Collection collection, Map<String, String> collectionContent) throws OrtolangException, NotIndexableContentException, RegistryServiceException, KeyNotFoundException {
        super(collection, CoreService.SERVICE_NAME, TYPE);
        content.put("registry", collectionContent);
        setContent(content);
    }

    @Override
    public Object[] getMapping() {
        return ROOT_COLLECTION_MAPPING;
    }
}
