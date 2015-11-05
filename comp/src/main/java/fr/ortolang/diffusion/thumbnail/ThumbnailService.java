package fr.ortolang.diffusion.thumbnail;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;

public interface ThumbnailService extends OrtolangService {
	
	public static final String SERVICE_NAME = "thumbnail";
	public static final String THUMBS_MIMETYPE = "image/jpeg";
	
	public static final String INFO_PATH = "path";
    public static final String INFO_SIZE = "size";
    public static final String INFO_FILES = "files";
    
	public Thumbnail getThumbnail(String key, int size) throws ThumbnailServiceException, AccessDeniedException, KeyNotFoundException, CoreServiceException, BinaryStoreServiceException;
	
}
