package fr.ortolang.diffusion.referentiel;

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
