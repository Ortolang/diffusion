package fr.ortolang.diffusion.preview;

import java.io.InputStream;
import java.util.List;

import fr.ortolang.diffusion.OrtolangJob;
import fr.ortolang.diffusion.preview.entity.Preview;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;



public interface PreviewService {
	
	public static final String SERVICE_NAME = "preview";
	
	public long getLastModification() throws PreviewServiceException;
	
	public void submit(OrtolangJob job) throws PreviewServiceException;
	
	public boolean exists(String key) throws PreviewServiceException;
	
	public List<Preview> list(List<String> keys) throws PreviewServiceException;
	
	public Preview getPreview(String key) throws PreviewServiceException;
	
	public long getPreviewSize(String key, String size) throws PreviewServiceException;
	
	public InputStream getPreviewContent(String key, String size) throws PreviewServiceException, AccessDeniedException;

}
