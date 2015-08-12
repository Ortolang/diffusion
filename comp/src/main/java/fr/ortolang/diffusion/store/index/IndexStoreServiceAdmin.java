package fr.ortolang.diffusion.store.index;

import java.util.Map;

import javax.ejb.Local;

import fr.ortolang.diffusion.store.json.JsonStoreServiceException;

@Local
public interface IndexStoreServiceAdmin {
    
    public Map<String, String> getServiceInfos() throws JsonStoreServiceException;

}
