package fr.ortolang.diffusion.collection;

import java.util.List;

import fr.ortolang.diffusion.DiffusionService;

public interface CollectionService extends DiffusionService {
	
	public static final String SERVICE_NAME = "Collection";
	
	public void createCollection(String identifier, String name, boolean mutable, List<String> content);

}
