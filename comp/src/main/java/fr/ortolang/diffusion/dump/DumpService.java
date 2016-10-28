package fr.ortolang.diffusion.dump;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import fr.ortolang.diffusion.OrtolangService;

public interface DumpService extends OrtolangService {
    
    String SERVICE_NAME = "dump";
    
    Set<String> dump(String key, OutputStream output, boolean single) throws DumpServiceException;
    
    void restore(InputStream input) throws DumpServiceException;

}
