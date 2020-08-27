package fr.ortolang.diffusion.content;

import fr.ortolang.diffusion.OrtolangWorker;

public interface ContentSearchServiceWorker extends OrtolangWorker {

	public static final String WORKER_NAME = "cs-worker";
	
    public static final String INDEX_ACTION = "index";
    public static final String REMOVE_ACTION = "remove";
}
