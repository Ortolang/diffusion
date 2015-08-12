package fr.ortolang.diffusion.store.json;

import java.io.InputStream;
import java.util.Map;

import javax.ejb.Local;

@Local
public interface JsonStoreServiceAdmin {
    
    public void insertDocument(String type, InputStream document) throws JsonStoreServiceException;
    
    public void insertDocument(String type, String document) throws JsonStoreServiceException;
    
    public String getDocument(String key) throws JsonStoreServiceException;
    
    public Map<String, String> getServiceInfos() throws JsonStoreServiceException;

}
