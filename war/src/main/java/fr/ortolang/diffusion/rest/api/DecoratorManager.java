package fr.ortolang.diffusion.rest.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DecoratorManager {
	
	private static final Logger logger = Logger.getLogger(DecoratorManager.class.getName());
	private static DecoratorManager instance;
	private HashMap<String, String> decorators;
	
	private DecoratorManager() {
		decorators = new HashMap<String, String> ();
	}
	
	public static DecoratorManager getInstance() {
		if ( instance == null ) {
			instance = new DecoratorManager();
		}
		return instance;
	}
	
	public String decorate(String name, String body) {
		if ( !decorators.containsKey(name) ) {
			loadDecorator(name);
		}
		return decorators.get(name).replace("${body}", body);
	}
	
	private void loadDecorator(String name) {
		try {
			logger.log(Level.INFO, "Loading decorator with name: " + name);
			Path decorator = Paths.get(DecoratorManager.class.getClassLoader().getResource(name).getPath());
			decorators.put(name, new String(Files.readAllBytes(decorator)));
		} catch ( IOException e ) {
			logger.log(Level.WARNING, "unable to load decorator for name: " + name);
		}
	}
}
