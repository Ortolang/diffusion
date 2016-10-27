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
    
    //IN order to allow IoC and dedicate operation of extraction to the ObjectDumper and ObjectRestorer, 
    //maybe use a more common read and create operation for object : 
    
    // OrtolangObject systemFindObject(String key)
    
    // void systemCreateObject(OrtolangObject object)
    
    // Set<String> getObjectDependencies(String key)
    
    // Set<String> getObjectBinaryStreams(String key)
    
    //PROBLEM : This needs to decouple the Dependency Resolving and the BinaryStream resolver to another method 

}
