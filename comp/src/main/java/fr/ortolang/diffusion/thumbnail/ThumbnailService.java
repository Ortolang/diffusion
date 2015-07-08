package fr.ortolang.diffusion.thumbnail;

import java.io.File;

import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface ThumbnailService {
	
	public static final String SERVICE_NAME = "thumbnail";
	public static final String THUMBS_MIMETYPE = "image/jpeg";
	
	public File getThumbnail(String key, int size) throws ThumbnailServiceException, AccessDeniedException, KeyNotFoundException;
	
}
