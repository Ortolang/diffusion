package fr.ortolang.diffusion.archive.facile;

import java.io.File;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.archive.exception.CheckArchivableException;

public interface FacileService extends OrtolangService {
    String SERVICE_NAME = "facile";

    String checkArchivableFile(File content, String filename) throws CheckArchivableException;
}