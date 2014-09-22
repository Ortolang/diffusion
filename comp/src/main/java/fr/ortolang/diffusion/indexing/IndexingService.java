package fr.ortolang.diffusion.indexing;


public interface IndexingService {
	
	public static final String SERVICE_NAME = "indexing";
    
    public void index(String key) throws IndexingServiceException;
    
    public void reindex(String key) throws IndexingServiceException;
    
    public void remove(String key) throws IndexingServiceException;

}
