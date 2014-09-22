package fr.ortolang.diffusion.store.index;

import java.util.List;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangSearchResult;

public interface IndexStoreService {
	
	public static final String SERVICE_NAME = "index-store";
	
	public void index(OrtolangIndexableObject object) throws IndexStoreServiceException;
	
	public void reindex(OrtolangIndexableObject object) throws IndexStoreServiceException;
	
	public void remove(String key) throws IndexStoreServiceException;
	
	public List<OrtolangSearchResult> search(String query) throws IndexStoreServiceException;

}
