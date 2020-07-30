package fr.ortolang.diffusion.archive;

import fr.ortolang.diffusion.OrtolangWorker;

public interface ArchiveServiceWorker extends OrtolangWorker {
    String WORKER_NAME = "archive-worker";
}