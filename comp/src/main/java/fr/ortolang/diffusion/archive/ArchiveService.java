package fr.ortolang.diffusion.archive;

import java.nio.file.Path;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveOutputStream;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.archive.exception.ArchiveServiceException;

public interface ArchiveService extends OrtolangService {

    String SERVICE_NAME = "archive";
    String CHECK_ACTION = "check";
    String CREATE_SIP_ACTION = "createsip";


    void checkArchivable(String key) throws ArchiveServiceException;
    void validateDataobject(String key) throws ArchiveServiceException;

    ArchiveOutputStream createArchive(String wskey) throws ArchiveServiceException;
    Path getArchivePath(String wskey);
    void finishArchive(ArchiveOutputStream tarOutput) throws ArchiveServiceException;

    List<ArchiveEntry> buildWorkspaceArchiveList(String wskey, String snapshot) throws ArchiveServiceException;
    void addEntryToArchive(ArchiveEntry entry, ArchiveOutputStream archiveOutput) throws ArchiveServiceException;
    java.nio.file.Path addXmlSIPFileToArchive(String wskey, String snapshot, String schema, List<ArchiveEntry> archiveList, ArchiveOutputStream archive) throws ArchiveServiceException;
}