package fr.ortolang.diffusion.referentiel;

import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.referentiel.entity.ReferentielEntity;
import fr.ortolang.diffusion.referentiel.entity.ReferentielType;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface ReferentielService extends OrtolangService, OrtolangIndexableService {

	public static final String SERVICE_NAME = "referentiel";
	
	public List<ReferentielEntity> listReferentielEntities() throws ReferentielServiceException;
	
	public void createReferentielEntity(String name, ReferentielType type, String content) throws ReferentielServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public ReferentielEntity readReferentielEntity(String name) throws ReferentielServiceException, KeyNotFoundException;
	
	public void updateReferentielEntity(String name, String content) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteReferentielEntity(String name) throws ReferentielServiceException, KeyNotFoundException, AccessDeniedException;
}
