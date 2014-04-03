package fr.ortolang.diffusion.search;

import java.util.List;

import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.OrtolangService;

public interface SearchService extends OrtolangService {
	
	public static final String SERVICE_NAME = "search";
	public static final String[] OBJECT_TYPE_LIST = new String[] { };
	
	public List<OrtolangSearchResult> search(String query) throws SearchServiceException;
	
}
