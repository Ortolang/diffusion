package fr.ortolang.diffusion.referential;

import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface ReferentialService extends OrtolangService, OrtolangIndexableService {

	public static final String SERVICE_NAME = "referential";
	
	public List<ReferentialEntity> listEntities(ReferentialEntityType type) throws ReferentialServiceException;
	
	public void createEntity(String name, ReferentialEntityType type, String content) throws ReferentialServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public ReferentialEntity readEntity(String name) throws ReferentialServiceException, KeyNotFoundException;
	
	public void updateEntity(String name, ReferentialEntityType type, String content) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException;

	public void updateEntity(String name, ReferentialEntityType type, String content, Long boost) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteEntity(String name) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException;

	public List<ReferentialEntity> findEntitiesByTerm(ReferentialEntityType type, String term, String lang) throws ReferentialServiceException;
}
