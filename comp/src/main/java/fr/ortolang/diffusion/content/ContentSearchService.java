package fr.ortolang.diffusion.content;

import java.util.List;
import java.util.Set;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.content.entity.ContentSearchResource;

public interface ContentSearchService extends OrtolangService {

    String SERVICE_NAME = "content-search";

	String INFO_TOTAL_SIZE = "size.total";
	
    // Resource
    List<ContentSearchResource> listResource();
    ContentSearchResource createResource(String wskey) throws ContentSearchServiceException;
    ContentSearchResource readResource(String id) throws ContentSearchNotFoundException;
    ContentSearchResource findResource(String wskey) throws ContentSearchNotFoundException;
    ContentSearchResource updateResource(String id, String pid, String title, String description, String landingPageURI) throws ContentSearchNotFoundException;
    void deleteResource(String id) throws ContentSearchNotFoundException;
    long countResources();
    void indexResourceFromWorkspace(String wskey, String snapshot) throws ContentSearchNotFoundException, ContentSearchServiceException;
    
    // Content document
    void indexContent(String key) throws ContentSearchServiceException;
    void deleteContent(String key) throws ContentSearchServiceException;
    void purgeResource(String id) throws ContentSearchNotFoundException, ContentSearchServiceException;
}
