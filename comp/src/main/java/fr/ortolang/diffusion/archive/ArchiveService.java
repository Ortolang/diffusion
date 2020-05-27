package fr.ortolang.diffusion.archive;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.archive.exception.ArchiveServiceException;
import fr.ortolang.diffusion.archive.exception.CheckArchivableException;

public interface ArchiveService extends OrtolangService {

    String SERVICE_NAME = "archive";

    void checkArchivable(String key) throws ArchiveServiceException;
}