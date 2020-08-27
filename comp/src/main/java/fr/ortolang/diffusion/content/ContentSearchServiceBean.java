package fr.ortolang.diffusion.content;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.content.entity.ContentSearchResource;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.MetadataSource;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.core.indexing.DataObjectContentIndexableContent;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.elastic.ElasticSearchService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;

@Local(ContentSearchService.class)
@Stateless(name = ContentSearchService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ContentSearchServiceBean implements ContentSearchService {

    private static final Logger LOGGER = Logger.getLogger(ContentSearchServiceBean.class.getName());

    @EJB
    private CoreService core;
    @EJB
    private BinaryStoreService binary;
    @EJB
    private AuthorisationService authorisation;
    @EJB
    private RegistryService registry;
    @EJB
    private ElasticSearchService elastic;
    @EJB
    private HandleStoreService handle;
	
	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;

    /**
     * A Dataobject policy needed to be indexed.
     */
    private List<String> policiesAllowed;

	public ContentSearchServiceBean() {
        policiesAllowed = new ArrayList<String>();
        policiesAllowed.add(MembershipService.UNAUTHENTIFIED_IDENTIFIER);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<ContentSearchResource> listResource() {
		TypedQuery<ContentSearchResource> query = em.createNamedQuery("findResources", ContentSearchResource.class);
		return query.getResultList();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ContentSearchResource createResource(String wskey) throws ContentSearchServiceException {
		String id = UUID.randomUUID().toString();
		OrtolangObjectIdentifier identifier;
		try {
			identifier = registry.lookup(wskey);
		} catch (RegistryServiceException | KeyNotFoundException e) {
			throw new ContentSearchServiceException("unable to find workspace with key " + wskey);
		}
		if (!identifier.getService().equals(CoreService.SERVICE_NAME) || !identifier.getType().equals(Workspace.OBJECT_TYPE)) {
			throw new ContentSearchServiceException("key " + wskey + " should be a workspace from the core service");
		}
		Workspace workspace = em.find(Workspace.class, identifier.getId());
		ContentSearchResource resource = new ContentSearchResource(id, wskey, workspace.getAlias(), workspace.getName());
		em.persist(resource);
		return resource;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ContentSearchResource readResource(String id) throws ContentSearchNotFoundException {
		try {
			return em.find(ContentSearchResource.class, id);
		} catch (NoResultException e) {
			throw new ContentSearchNotFoundException("unable to find a content search with id : " + id);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ContentSearchResource findResource(String wskey) throws ContentSearchNotFoundException {
		try {
			return em.createNamedQuery("findResourcesByWorkspace", ContentSearchResource.class)
					.setParameter("workspace", wskey)
					.getSingleResult();
		} catch (NoResultException e) {
			throw new ContentSearchNotFoundException("unable to find a content search with wskey : " + wskey);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ContentSearchResource updateResource(String id, String pid, String title, String description, String landingPageURI) throws ContentSearchNotFoundException {
		try {
			ContentSearchResource res = em.find(ContentSearchResource.class, id);
			res.setPid(pid);
			res.setTitle(title);
			res.setDescription(description);
			res.setLandingPageURI(landingPageURI);
			return em.merge(res);
		} catch (NoResultException e) {
			throw new ContentSearchNotFoundException("unable to find a content search with id : " + id);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ContentSearchResource setResourceDocuments(String id, Set<String> documents) throws ContentSearchNotFoundException {
		try {
			ContentSearchResource res = em.find(ContentSearchResource.class, id);
			res.setDocuments(documents);
			return em.merge(res);
		} catch (NoResultException e) {
			throw new ContentSearchNotFoundException("unable to find a content search with id : " + id);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteResource(String id) throws ContentSearchNotFoundException {
		ContentSearchResource record = readResource(id);
		em.remove(record);
	}

	/**
	 * Removes all documents related to the specific resource. If the parameter force is true,
	 * it removes all documents even if the resource is not found.
	 * @param id a resource id
	 * @throws , ContentSearchServiceException 
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void purgeResource(String id) throws ContentSearchNotFoundException, ContentSearchServiceException {
		ContentSearchResource res = readResource(id);
		
		for(String doc : res.getDocuments()) {
			try {
				deleteContent(doc);
			} catch(ContentSearchServiceException e) {
				LOGGER.log(Level.WARNING, "unable to delete es document : " + doc, e);
			}
		}
	}
	

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long countResources() {
		TypedQuery<Long> query = em.createNamedQuery("countResources", Long.class);
		return query.getSingleResult();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void indexContent(String key) throws ContentSearchServiceException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
	        if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
	            throw new ContentSearchServiceException("object identifier " + identifier + " does not refer to service " + CoreService.SERVICE_NAME);
	        }
			DataObject dataObject = em.find(DataObject.class, identifier.getId());
			dataObject.setKey(key);
			elastic.indexDocument(new DataObjectContentIndexableContent(dataObject));
			
		} catch (RegistryServiceException | KeyNotFoundException | IndexingServiceException | OrtolangException e) {
			throw new ContentSearchServiceException(e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void deleteContent(String key) throws ContentSearchServiceException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
	        if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
	            throw new ContentSearchServiceException("object identifier " + identifier + " does not refer to service " + CoreService.SERVICE_NAME);
	        }
			DataObject dataObject = em.find(DataObject.class, identifier.getId());
			dataObject.setKey(key);
			elastic.removeDocument(new DataObjectContentIndexableContent(dataObject, false));
		} catch (RegistryServiceException | KeyNotFoundException | IndexingServiceException | OrtolangException e) {
			throw new ContentSearchServiceException(e);
		}
	}
	

    /**
     * Indexes the content of all documents which are :
     * - availables for anonymous (forall)
     * By default, if the snapshot is not specified, it indexes the latest publishd snapshot.
     * @param wskey workspace key
     * @param snapshot snapshot key
     * @throws ContentSearchNotFoundException 
     * @throws ContentSearchServiceException 
     */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void indexResourceFromWorkspace(String wskey, String snapshot) throws ContentSearchNotFoundException, ContentSearchServiceException {

    	// Check if job.getTarget() is a workspace
    	OrtolangObjectIdentifier identifier;
		try {
			identifier = registry.lookup(wskey);
		} catch (RegistryServiceException | KeyNotFoundException e) {
			throw new ContentSearchNotFoundException("unable to find workspace with key " + wskey);
		}
		if (!identifier.getService().equals(CoreService.SERVICE_NAME) || !identifier.getType().equals(Workspace.OBJECT_TYPE)) {
			throw new ContentSearchServiceException("key " + wskey + " should be a workspace from the core service");
		}
		try { 
			if (snapshot == null) {
				snapshot = core.findWorkspaceLatestPublishedSnapshot(wskey);
			}
			if (snapshot == null) {
				LOGGER.log(Level.WARNING, "When indexing workspace for ContentSearch, finds no published snapshot for workspace " + wskey);
				return;
			}
			Workspace workspace = core.systemReadWorkspace(wskey);
			workspace.setKey(wskey);
			String root = workspace.findSnapshotByName(snapshot).getKey();
			String pid = null;
			List<String> hdls = handle.listHandlesForKey(root);
			if (hdls.isEmpty()) {
				throw new ContentSearchServiceException(
						"Unable to index content of workspace [" + wskey + "] because there is no handle on collection " + root);
			} else {
				pid = hdls.get(0);
			}
			// Create or update if resource already exists
			ContentSearchResource res = findResource(wskey);
			if (res != null) {
				// Sets documents in updateResource method
				purgeResource(res.getId());
				updateResource(res, workspace, root, HandleStoreService.HDL_PROXY_URL + pid);
			}
		} catch(CoreServiceException | KeyNotFoundException | HandleStoreServiceException | AccessDeniedException | RegistryServiceException e) {
			throw new ContentSearchServiceException("unable to index resource content", e);
		}
    }
    
    /**
     * Updates the resource informations (Title, ...) and launches the indexation of all dataobject publicaly available.
     * During the process, the documents parameter is filled by the keys of all document indexed.
     * 
     * @param res the resource
     * @param ws the workspace 
     * @param root the root collection
     * @param documents a list of documents indexed
     * @throws ContentSearchNotFoundException
     * @throws AccessDeniedException
     * @throws CoreServiceException
     * @throws KeyNotFoundException
     * @throws RegistryServiceException
     */
    private void updateResource(ContentSearchResource res, Workspace ws, String root, String pid) throws ContentSearchNotFoundException, AccessDeniedException, CoreServiceException, KeyNotFoundException, RegistryServiceException {
		HashSet<String> documents = new HashSet<String>();
    	updateResource(res.getId(), pid, ws.getAlias(), ws.getName(), pid);
    	// Getting ignore rules
    	Collection rootCollection = core.readCollection(root);
    	List<String> rules = buildIgnoreObject(rootCollection);
    	// Index content text of all public documents
    	java.util.Set<CollectionElement> elements = core.systemReadCollection(root).getElements();
		for (CollectionElement element : elements) {
			indexDataObject(res, element.getKey(), documents, rules);
		}
		// Updates the resource with the list of documents
		setResourceDocuments(res.getId(), documents);
    }

    /**
     * Extracts ignore rules from metadata object attached to a collection.
     * @param collection
     * @return
     * @throws AccessDeniedException
     * @throws CoreServiceException
     * @throws KeyNotFoundException
     */
    private List<String> buildIgnoreObject(Collection collection) throws AccessDeniedException, CoreServiceException, KeyNotFoundException {
    	List<String> rules = new ArrayList<String>();
        MetadataElement mdIgnore = ((MetadataSource) collection).findMetadataByName(MetadataFormat.IGNORE);
        if (mdIgnore != null) {
            MetadataObject md = (MetadataObject) core.readMetadataObject(mdIgnore.getKey());
            try {
                JsonReader reader = Json.createReader(binary.get(md.getStream()));
                JsonObject json = reader.readObject();
                JsonArray rulesArray = json.getJsonArray("rules");
                if ( rulesArray != null) {
                	for ( JsonString jsonStr : rulesArray.getValuesAs(JsonString.class)) {
                		rules.add(jsonStr.getString());
                	}
                	return rules;
                }
            } catch (BinaryStoreServiceException | DataNotFoundException e) {
                LOGGER.log(Level.SEVERE, "unable to read ignore metadata", e);
            }
        }
        return rules;
    }
    
    /**
     * Indexes the content of a dataobject or call this method on a collection element.
     * This method check whether the dataobject is downloadable by anyone or not. If it's
     * publicaly available, the content of the dataobject is indexed by the contentSearch service. 
     * @param res the resource
     * @param key the workspace
     * @param documents a list of documents indexed
     * @throws RegistryServiceException
     * @throws KeyNotFoundException
     * @throws AccessDeniedException
     * @throws CoreServiceException
     */
    private void indexDataObject(ContentSearchResource res, String key, Set<String> documents, List<String> rules) throws RegistryServiceException, KeyNotFoundException, AccessDeniedException, CoreServiceException {
    	OrtolangObjectIdentifier identifier = registry.lookup(key);
		String type = identifier.getType();

		switch (type) {
		case Collection.OBJECT_TYPE:
			// Checks if the collection is block by a rule (from ignore file)
			boolean blockCollectionByRule = false;
			Collection collection = core.readCollection(key);
			for(String rule : rules) {
				if( collection.getName().equals(rule) ) {
					blockCollectionByRule = true;
					break;
				}
			}
			// Index content text of all public documents
			if ( !blockCollectionByRule) {
				java.util.Set<CollectionElement> elements = core.systemReadCollection(key).getElements();
				for (CollectionElement element : elements) {
					indexDataObject(res, element.getKey(), documents, rules);
				}
			}
			break;
		case DataObject.OBJECT_TYPE:
			// Checks if the data object is block by a rule (from ignore file)
			boolean blockByRule = false;
			DataObject dataObject = core.readDataObject(key);
			for(String rule : rules) {
				if( dataObject.getName().equals(rule) ) {
					blockByRule = true;
					break;
				}
			}
			// Indexes only document with download permission for anonymous
			if ( !blockByRule) {
				try {
					authorisation.checkPermission(key, policiesAllowed, "download");
					indexContent(key);
					documents.add(key);
				} catch (AccessDeniedException | AuthorisationServiceException | ContentSearchServiceException e) {
					LOGGER.log(Level.WARNING, "Content of dataobject with key [" + key + "] can not be indexed", e);
				}
			}
			break;
		}
    	
    }
    

	public EntityManager getEm() {
		return em;
	}

	public void setEm(EntityManager em) {
		this.em = em;
	}

	@Override
	public String getServiceName() {
		return SERVICE_NAME;
	}

	@Override
	public Map<String, String> getServiceInfos() {
		Map<String, String> infos = new HashMap<String, String>();
		infos.put(ContentSearchService.INFO_TOTAL_SIZE, Long.toString(countResources()));
		return infos;
	}
}
