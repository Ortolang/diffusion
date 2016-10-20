package fr.ortolang.diffusion;

import java.util.Set;

import javax.xml.stream.XMLStreamWriter;

public interface OrtolangObjectProviderService extends OrtolangService {
    
    String[] getObjectTypeList();
    
    String[] getObjectPermissionsList(String type) throws OrtolangException;
    
    OrtolangObject findObject(String key) throws OrtolangException;

    OrtolangObjectSize getSize(String key) throws OrtolangException;
    
    void dump(String key, XMLStreamWriter writer, Set<String> deps, Set<String> streams) throws OrtolangException;
    
    void restore() throws OrtolangException;

}
