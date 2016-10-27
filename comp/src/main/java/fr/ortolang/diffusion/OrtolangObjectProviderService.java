package fr.ortolang.diffusion;


public interface OrtolangObjectProviderService extends OrtolangService {
    
    String[] getObjectTypeList();
    
    String[] getObjectPermissionsList(String type) throws OrtolangException;
    
    OrtolangObject findObject(String key) throws OrtolangException;

    OrtolangObjectSize getSize(String key) throws OrtolangException;
    
    OrtolangObjectExportHandler getObjectExportHandler(String key) throws OrtolangException;
    
    OrtolangObjectImportHandler getObjectImportHandler() throws OrtolangException;
    
}
