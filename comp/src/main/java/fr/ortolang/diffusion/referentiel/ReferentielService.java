package fr.ortolang.diffusion.referentiel;

import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.referentiel.entity.Organization;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface ReferentielService extends OrtolangService, OrtolangIndexableService {

	public static final String SERVICE_NAME = "referentiel";
	
	public List<Organization> listOrganizations() throws ReferentielServiceException;
	
	public void createOrganization(String identifier, String name, String fullname, String acronym, String city, String country, String homepage, String img) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public Organization readOrganization(String key) throws ReferentielServiceException, KeyNotFoundException;
	
	public void updateOrganzation(String key, String name, String fullname, String acronym, String city, String country, String homepage, String img) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteOrganization(String key) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException;

	public String getOrganizationKeyForIdentifier(String identifier);
}
