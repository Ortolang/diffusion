package fr.ortolang.diffusion.archive;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.archive.exception.ArchiveServiceException;

public interface ArchiveService extends OrtolangService {

    String SERVICE_NAME = "archive";
    String CHECK_ACTION = "check";
    String CREATE_SIP_ACTION = "createsip";


    void checkArchivable(String key) throws ArchiveServiceException;
    void createSIP(String key, String schema) throws ArchiveServiceException;
    void createSIPTar(String key, String schema) throws ArchiveServiceException;
    void validateDataobject(String key) throws ArchiveServiceException;
}