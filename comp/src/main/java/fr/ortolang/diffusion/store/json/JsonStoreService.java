package fr.ortolang.diffusion.store.json;

import java.io.InputStream;
import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangService;

public interface JsonStoreService extends OrtolangService {

	public static final String SERVICE_NAME = "json-store";
	
	public void index(OrtolangIndexableObject<IndexableJsonContent> object) throws JsonStoreServiceException;
	
	public void remove(String key) throws JsonStoreServiceException;
	
	public List<String> search(String query) throws JsonStoreServiceException;
	
    public void systemInsertDocument(String type, InputStream document) throws JsonStoreServiceException;
    
    public void systemInsertDocument(String type, String document) throws JsonStoreServiceException;
    
    public String systemGetDocument(String key) throws JsonStoreServiceException;
    
}
