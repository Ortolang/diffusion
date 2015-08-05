package fr.ortolang.diffusion.store.json;

import java.util.List;

import javax.ejb.Local;

import fr.ortolang.diffusion.OrtolangIndexableObject;

@Local
public interface JsonStoreService {

	public static final String SERVICE_NAME = "json-store";
	
	public void index(OrtolangIndexableObject<IndexableJsonContent> object) throws JsonStoreServiceException;
	
	public void remove(String key) throws JsonStoreServiceException;
	
	public List<String> search(String query) throws JsonStoreServiceException;
}
