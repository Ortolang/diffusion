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
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class OrtolangObjectIndexableContent extends OrtolangIndexableContent {

    final static String[] ORTOLANG_OBJECT_MAPPING;

    static {
        ORTOLANG_OBJECT_MAPPING = new String[]{
                "key",
                "type=keyword",
                "name",
                "type=string",
                "status",
                "type=keyword",
                "creation",
                "type=date",
                "modification",
                "type=date",
                "modification",
                "type=date"
        };
    }

    protected Map<String, Object> content;

    OrtolangObjectIndexableContent(OrtolangObject object, String index, String type) throws IndexingServiceException {
        super(index, type, object.getObjectKey());
        content = new HashMap<>();
        content.put("key", object.getObjectKey());
        content.put("name", object.getObjectName());
        try {
            RegistryService registry = (RegistryService) OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME, RegistryService.class);
            content.put("status", registry.getPublicationStatus(object.getObjectKey()));
            content.put("creation", registry.getCreationDate(object.getObjectKey()));
            content.put("modification", registry.getLastModificationDate(object.getObjectKey()));
            AuthorisationService authorization = (AuthorisationService) OrtolangServiceLocator.lookup(AuthorisationService.SERVICE_NAME, AuthorisationService.class);
            Map<String, List<String>> policyRules = authorization.getPolicyRules(object.getObjectKey());
            // TODO
        } catch (OrtolangException | RegistryServiceException | KeyNotFoundException | AuthorisationServiceException e) {
            throw new IndexingServiceException(e);
        }
    }
}
