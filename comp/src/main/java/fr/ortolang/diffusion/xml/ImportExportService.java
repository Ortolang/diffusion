package fr.ortolang.diffusion.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import fr.ortolang.diffusion.OrtolangImportExportLogger;
import fr.ortolang.diffusion.OrtolangService;

public interface ImportExportService extends OrtolangService {
    
    String SERVICE_NAME = "import-export";
    
    void dump(Set<String> key, OutputStream output, OrtolangImportExportLogger logger, boolean withdeps, boolean withbinary) throws ImportExportServiceException;
    
    void restore(InputStream input, OrtolangImportExportLogger logger) throws ImportExportServiceException;

}
