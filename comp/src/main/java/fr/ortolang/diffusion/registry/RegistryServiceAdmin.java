package fr.ortolang.diffusion.registry;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import fr.ortolang.diffusion.registry.entity.RegistryEntry;

@Local
public interface RegistryServiceAdmin {
    
    public List<RegistryEntry> systemListEntries(String keyFilter) throws RegistryServiceException;
    
    public long systemCountEntries(String identifierFilter) throws RegistryServiceException;
    
    public Map<String, String> getServiceInfos() throws RegistryServiceException;

}
