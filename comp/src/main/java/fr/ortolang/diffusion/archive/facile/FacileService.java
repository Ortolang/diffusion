package fr.ortolang.diffusion.archive.facile;

import java.io.File;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.archive.exception.CheckArchivableException;
import fr.ortolang.diffusion.archive.facile.entity.Validator;

public interface FacileService extends OrtolangService {
    String SERVICE_NAME = "facile";

    Validator checkArchivableFile(File content, String filename) throws CheckArchivableException;
}