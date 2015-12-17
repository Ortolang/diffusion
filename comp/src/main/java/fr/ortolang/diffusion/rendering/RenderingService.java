package fr.ortolang.diffusion.rendering;

import java.io.File;
import java.util.Collection;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;

public interface RenderingService extends OrtolangService {

    public static final String SERVICE_NAME = "rendering";
    
    public static final String INFO_PATH = "path";
    public static final String INFO_SIZE = "size";
    public static final String INFO_FILES = "files";
    
    public Collection<RenderEngine> listEngines();
    
    public Collection<RenderEngine> listEnginesForType(String mimetype);
    
    public File getView(String key, String engineid) throws RenderingServiceException, AccessDeniedException, KeyNotFoundException, CoreServiceException, BinaryStoreServiceException;
    
}
