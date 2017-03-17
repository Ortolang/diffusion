package fr.ortolang.diffusion.referential;

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
 * Copyright (C) 2013 - 2016 Ortolang Team
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

import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangObjectProviderService;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface ReferentialService extends OrtolangObjectProviderService, OrtolangIndexableService {

    String SERVICE_NAME = "referential";

    List<ReferentialEntity> listEntities(ReferentialEntityType type) throws ReferentialServiceException;

    ReferentialEntity createEntity(String name, ReferentialEntityType type, String content) throws ReferentialServiceException, KeyAlreadyExistsException, AccessDeniedException;

    ReferentialEntity readEntity(String name) throws ReferentialServiceException, KeyNotFoundException;

    void updateEntity(String name, ReferentialEntityType type, String content) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException;

    void updateEntity(String name, ReferentialEntityType type, String content, Long boost) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException;

    void deleteEntity(String name) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException;

//    List<ReferentialEntity> findEntitiesByTerm(ReferentialEntityType type, String term, String lang) throws ReferentialServiceException;
}
