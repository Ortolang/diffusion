package fr.ortolang.diffusion.viewer;

import java.io.File;
import java.util.Collection;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;

public interface ViewerService extends OrtolangService {

    public static final String SERVICE_NAME = "viewer";
    
    public static final String INFO_PATH = "path";
    public static final String INFO_SIZE = "size";
    public static final String INFO_FILES = "files";
    
    public Collection<ViewerEngine> listViewers();
    
    public Collection<ViewerEngine> listViewersForType(String mimetype);
    
    public File getView(String key) throws ViewerServiceException, AccessDeniedException, KeyNotFoundException, CoreServiceException, BinaryStoreServiceException;
    
    public File getView(String key, String viewer) throws ViewerServiceException, AccessDeniedException, KeyNotFoundException, CoreServiceException, BinaryStoreServiceException;

}
