package fr.ortolang.diffusion.referentiel;

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

import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.referentiel.entity.LicenseEntity;
import fr.ortolang.diffusion.referentiel.entity.OrganizationEntity;
import fr.ortolang.diffusion.referentiel.entity.PersonEntity;
import fr.ortolang.diffusion.referentiel.entity.StatusOfUseEntity;
import fr.ortolang.diffusion.referentiel.entity.TermEntity;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface ReferentielService extends OrtolangService, OrtolangIndexableService {

	public static final String SERVICE_NAME = "referential";
	
	public List<OrganizationEntity> listOrganizationEntities() throws ReferentielServiceException;
	
	public void createOrganizationEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public OrganizationEntity readOrganizationEntity(String name) throws ReferentielServiceException, KeyNotFoundException;
	
	public void updateOrganizationEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException;

	public List<PersonEntity> listPersonEntities() throws ReferentielServiceException;

	public void createPersonEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public PersonEntity readPersonEntity(String name) throws ReferentielServiceException, KeyNotFoundException;

	public void updatePersonEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException;

	public List<StatusOfUseEntity> listStatusOfUseEntities() throws ReferentielServiceException;
	
	public void createStatusOfUseEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public StatusOfUseEntity readStatusOfUseEntity(String name) throws ReferentielServiceException, KeyNotFoundException;
	
	public void updateStatusOfUseEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException;

	public List<LicenseEntity> listLicenseEntities() throws ReferentielServiceException;

	public void createLicenseEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public LicenseEntity readLicenseEntity(String name) throws ReferentielServiceException, KeyNotFoundException;

	public void updateLicenseEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException;

	public List<TermEntity> listTermEntities() throws ReferentielServiceException;

	public void createTermEntity(String name, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public TermEntity readTermEntity(String name) throws ReferentielServiceException, KeyNotFoundException;

	public void updateTermEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException;

}
