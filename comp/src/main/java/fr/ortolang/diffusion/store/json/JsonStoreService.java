package fr.ortolang.diffusion.store.json;

import java.io.InputStream;
import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangService;

public interface JsonStoreService extends OrtolangService {

	public static final String SERVICE_NAME = "json-store";
	
	public static final String INFO_PATH = "path";
    public static final String INFO_SIZE = "size";
    public static final String INFO_FILES = "files";
    public static final String INFO_POOL_SIZE_MAX = "pool.maxsize";
    public static final String INFO_AVAIL_CONNECTIONS = "connections.avail";
    public static final String INFO_INSTANCES_CREATED = "instances.created";
    public static final String INFO_DB_NAME = "db.name";
    public static final String INFO_DB_SIZE = "db.size";
    public static final String INFO_DB_STATUS = "db.status";
       
	
	public void index(OrtolangIndexableObject<IndexableJsonContent> object) throws JsonStoreServiceException;
	
	public void remove(String key) throws JsonStoreServiceException;
	
	public List<String> search(String query) throws JsonStoreServiceException;
	
    public void systemInsertDocument(String type, InputStream document) throws JsonStoreServiceException;
    
    public void systemInsertDocument(String type, String document) throws JsonStoreServiceException;
    
    public String systemGetDocument(String key) throws JsonStoreServiceException;
    
}
