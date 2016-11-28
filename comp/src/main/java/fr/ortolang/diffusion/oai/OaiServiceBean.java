package fr.ortolang.diffusion.oai;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexableObjectFactory;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.entity.Set;
import fr.ortolang.diffusion.oai.format.DCXMLDocument;
import fr.ortolang.diffusion.oai.format.OAI_DCFactory;
import fr.ortolang.diffusion.oai.format.OLACFactory;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;

@Local(OaiService.class)
@Stateless(name = OaiService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class OaiServiceBean implements OaiService {

    private static final Logger LOGGER = Logger.getLogger(OaiServiceBean.class.getName());

    @EJB
    private RegistryService registry;
	@EJB
	private CoreService core;
	@EJB
	private HandleStoreService handleStore;
	@EJB
	private BinaryStoreService binaryStore;
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
	@Resource
	private SessionContext ctx;

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
	}

	public SessionContext getSessionContext() {
		return this.ctx;
	}
	
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Record> listRecordsByIdentifier(String identifier) throws RecordNotFoundException {
        TypedQuery<Record> query = em.createNamedQuery("findRecordsByIdentifier", Record.class).setParameter("identifier", identifier);
        List<Record> records = query.getResultList();
        if (records == null || records.isEmpty()) {
            throw new RecordNotFoundException("unable to list records with identifier: " + identifier);
        }
        return records;
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Record> listRecordsBySet(String set) throws RecordNotFoundException {
    	TypedQuery<Record> query = em.createNamedQuery("listRecordsBySet", Record.class).setParameter("set", set);
    	List<Record> records = query.getResultList();
    	if (records == null || records.isEmpty()) {
    		throw new RecordNotFoundException("unable to list records with set: " + set);
    	}
    	return records;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Record> listRecordsByMetadataPrefix(String metadataPrefix, Long from, Long until) throws RecordNotFoundException, OaiServiceException {
        return listRecordsByMetadataPrefixAndSetspec(metadataPrefix, null, from, until);
    }
    
    public List<Record> listRecordsByMetadataPrefixAndSetspec(String metadataPrefix, String setSpec, Long from, Long until) throws RecordNotFoundException, OaiServiceException {
    	CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Record> criteria = builder.createQuery(Record.class);
        Root<Record> customer = criteria.from(Record.class);
        List<Predicate> predicates = new ArrayList<Predicate>();
        
        if (metadataPrefix == null) {
            throw new OaiServiceException("metadataPrefix argument missing");
        }
        
        predicates.add(builder.equal(customer.get("metadataPrefix"), metadataPrefix));
        if (setSpec != null) {
        	predicates.add(builder.isMember(setSpec, customer.get("sets")));
        }
        if (from != null) {            
            predicates.add(builder.greaterThanOrEqualTo(customer.get("lastModificationDate"), from));
        }
        if (until != null) {            
            predicates.add(builder.lessThanOrEqualTo(customer.get("lastModificationDate"), until));
        }
        criteria.select(customer)
                .where(predicates.toArray(new Predicate[]{}));
        
        List<Record> records = em.createQuery(criteria).getResultList();
        if (records == null || records.isEmpty()) {
            throw new RecordNotFoundException("unable to list records with metadataPrefix: " + metadataPrefix);
        }
        return records;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Record findRecord(String identifier, String metadataPrefix) throws RecordNotFoundException {
        LOGGER.log(Level.FINE, "finding record with identifier " + identifier + " and metadataPrefix " + metadataPrefix);
        try {
            return em.createNamedQuery("findRecordsByIdentifier", Record.class).setParameter("identifier", identifier).getSingleResult();
        } catch (NoResultException e) {
            throw new RecordNotFoundException("unable to find a record with identifier: " + identifier);
        }
    }
    
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Record createRecord(String identifier, String metadataPrefix, long lastModificationDate, String xml) {
	    return createRecord(identifier, metadataPrefix, lastModificationDate, xml, new HashSet<String>());
    }
    
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Record createRecord(String identifier, String metadataPrefix, long lastModificationDate, String xml, java.util.Set<String> sets) {
		String id = UUID.randomUUID().toString();
		Record record = new Record(id, identifier, metadataPrefix, lastModificationDate, xml, sets);
		em.persist(record);
		return record;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Record readRecord(String id) throws RecordNotFoundException {
        try {
	        return em.find(Record.class, id);
        } catch (NoResultException e) {
        	throw new RecordNotFoundException("unable to find a record with id : " + id);
        }
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Record updateRecord(String id, long lastModificationDate, String xml) throws RecordNotFoundException {
		try {
			Record rec = em.find(Record.class, id);
			rec.setLastModificationDate(lastModificationDate);
			rec.setXml(xml);
			return em.merge(rec);
		} catch (NoResultException e) {
        	throw new RecordNotFoundException("unable to find a record with id : " + id);
        }
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteRecord(String id) throws RecordNotFoundException {
        LOGGER.log(Level.FINE, "deleting record with id " + id);
		Record record = readRecord(id);
		em.remove(record);
	}

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Set> listSets() {
        LOGGER.log(Level.FINE, "finding all sets");
        return em.createNamedQuery("listAllSets", Set.class).getResultList();
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Set findSet(String spec) throws SetNotFoundException {
        LOGGER.log(Level.FINE, "finding set with spec " + spec);
        try {
            return em.createNamedQuery("listAllSetsWithSpec", Set.class).setParameter("spec", spec).getSingleResult();
        } catch (NoResultException e) {
            throw new SetNotFoundException("unable to find a set with spec: " + spec);
        }
    }
    
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Set createSet(String spec, String name) throws SetAlreadyExistsException {
	    try {
            em.createNamedQuery("listAllSetsWithSpec", Set.class).setParameter("spec", spec).getSingleResult();
            throw new SetAlreadyExistsException("Set already exists " + spec);
        } catch (NoResultException e) {
        }
    	Set set = new Set(spec, name);
    	em.persist(set);
    	return set;
    }

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Set readSet(String spec) throws SetNotFoundException {
        try {
	        return em.find(Set.class, spec);
        } catch (NoResultException e) {
        	throw new SetNotFoundException("unable to find a set with spec : " + spec);
        }
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public Set updateSet(String spec, String name) throws SetNotFoundException {
		Set set = readSet(spec);
		set.setName(name);
		return em.merge(set);
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteSet(String spec) throws SetNotFoundException {
		Set set = readSet(spec);
		em.remove(set);
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void buildRecordsForWorkspace(String wskey, String snapshot) throws OaiServiceException {
		try {
			HashSet<String> setsWorkspace = new HashSet<String>(Arrays.asList(wskey));
			List<Record> records = null;
			try {
				records = listRecordsByIdentifier(wskey);
			} catch (RecordNotFoundException e) {
			}
			if (records==null) {
				LOGGER.log(Level.FINE, "creating OAI record, set and setRecord for workspace " + wskey + " and snapshot " + snapshot);
				// Creating a Set for the workspace
				try {
					createSet(wskey, "Workspace "+wskey);
				} catch (SetAlreadyExistsException e) {
					LOGGER.log(Level.WARNING, "unable to create a Set " + wskey, e);
				}
				createRecordsForWorkspace(wskey, snapshot, setsWorkspace);
				
			} else {
				LOGGER.log(Level.FINE, "updating OAI record for workspace " + wskey + " and snapshot " + snapshot);
				// Set is already created (see below), so just cleans and creates new records
				// Deleting all Records linking of the workspace
				List<Record> recordsOfWorkspace;
				try {
					recordsOfWorkspace = listRecordsBySet(wskey);
					recordsOfWorkspace.forEach(rec -> {
						try {
							deleteRecord(rec.getId());
						} catch (RecordNotFoundException e) {
						}
					});
				} catch (RecordNotFoundException e1) {
					// No record
				}
				createRecordsForWorkspace(wskey, snapshot, setsWorkspace);
			}
		} catch (RegistryServiceException | KeyNotFoundException | OaiServiceException | CoreServiceException | MetadataPrefixUnknownException e) {
			ctx.setRollbackOnly();
			LOGGER.log(Level.SEVERE, "unable to create OAI record for workspace " + wskey + " and snapshot ", e);
			throw new OaiServiceException("unable to create OAI records for workspace " + wskey + " and snapshot " + snapshot, e);
		}
	}
	
	private void createRecordsForWorkspace(String wskey, String snapshot, HashSet<String> setsWorkspace) throws RegistryServiceException, KeyNotFoundException, OaiServiceException, MetadataPrefixUnknownException, CoreServiceException {
		// Creating a Record for the workspace
		createRecord(wskey, MetadataFormat.OAI_DC, registry.getLastModificationDate(wskey), buildXMLFromWorkspace(wskey, snapshot, MetadataFormat.OAI_DC), setsWorkspace);
		createRecord(wskey, MetadataFormat.OLAC, registry.getLastModificationDate(wskey), buildXMLFromWorkspace(wskey, snapshot, MetadataFormat.OLAC), setsWorkspace);
		
		// Creating a Record for each element with OAI_DC metadata object of the workspace
		Map<String, Map<String, List<String>>> map = core.buildWorkspacePublicationMap(wskey, snapshot);
        for(String key : map.keySet()) {
        	String oai_dc = buildXMLFromOrtolangObject(key, MetadataFormat.OAI_DC);
        	if (oai_dc!=null) {
        		createRecord(key, MetadataFormat.OAI_DC, registry.getLastModificationDate(key), oai_dc, setsWorkspace);
        	}
        	String olac = buildXMLFromOrtolangObject(key, MetadataFormat.OLAC);
        	if (olac!=null) {
        		createRecord(key, MetadataFormat.OLAC, registry.getLastModificationDate(key), olac, setsWorkspace);
        	}
        }
	}

	private String buildXMLFromWorkspace(String wskey, String snapshot, String metadataPrefix) throws OaiServiceException, MetadataPrefixUnknownException {
		LOGGER.log(Level.FINE, "building XML for workspace " + wskey + " and snapshot " + snapshot + " and metadataPrefix " + metadataPrefix);
		try {
			Workspace workspace = core.readWorkspace(wskey);
			String root = workspace.findSnapshotByName(snapshot).getKey();
			OrtolangIndexableObject<IndexableJsonContent> indexableObject = OrtolangIndexableObjectFactory.buildJsonIndexableObject(root);
			String item = indexableObject.getContent().getStream().get(MetadataFormat.ITEM);

			DCXMLDocument xml = null;
			if (metadataPrefix.equals(MetadataFormat.OAI_DC)) {
				xml = OAI_DCFactory.buildFromItem(item);
			} else if (metadataPrefix.equals(MetadataFormat.OLAC)) {
				xml = OLACFactory.buildFromItem(item);
			}
			
			// Automatically adds handles to 'identifier' XML element
			List<String> handles;
			try {
				handles = handleStore.listHandlesForKey(root);
				for(String handle : handles) {          
					xml.addDcField("identifier", 
							"http://hdl.handle.net/"+OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX)
							+ "/" +handle);
				}
			} catch (NullPointerException | ClassCastException | HandleStoreServiceException e) {
				LOGGER.log(Level.WARNING, "No handle for key " + root, e);
			}
			if (xml!=null) {				
				return xml.toString();
			} else {
				throw new MetadataPrefixUnknownException("unable to build xml for oai record cause metadata prefix unknown " + metadataPrefix);
			}
		} catch (CoreServiceException | KeyNotFoundException | OrtolangException | NotIndexableContentException e) {
			LOGGER.log(Level.SEVERE, "unable to build oai_dc from workspace " + wskey, e);
			throw new OaiServiceException("unable to build xml for oai record");
		}
	}
	
	private String buildXMLFromOrtolangObject(String key, String metadataPrefix) throws OaiServiceException {
		LOGGER.log(Level.FINE, "creating OAI record for ortolang object " + key + " for metadataPrefix " + metadataPrefix);
		try {
			List<String> mdKeys = core.findMetadataObjectsForTargetAndName(key, metadataPrefix);
			DCXMLDocument xml = null;

			if (!mdKeys.isEmpty()) {
				String mdKey = mdKeys.get(0);
				MetadataObject md = core.readMetadataObject(mdKey);
				if (metadataPrefix.equals(MetadataFormat.OAI_DC)) {
					xml = OAI_DCFactory.buildFromJson(getContent(binaryStore.get(md.getStream())));
				} else if (metadataPrefix.equals(MetadataFormat.OLAC)) {
					xml = OLACFactory.buildFromJson(getContent(binaryStore.get(md.getStream())));
				}
			} else {
				return null;
			}
			
			// Automatically adds handles to 'identifier' XML element
			List<String> handles;
			try {
				handles = handleStore.listHandlesForKey(key);
				for(String handle : handles) {          
					xml.addDcField("identifier", 
							"http://hdl.handle.net/"+OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX)
							+ "/" +handle);
				}
			} catch (NullPointerException | ClassCastException | HandleStoreServiceException e) {
				LOGGER.log(Level.WARNING, "No handle for key " + key, e);
			}
			if (xml!=null) {				
				return xml.toString();
			} else {
				throw new OaiServiceException("unable to build xml for oai record cause metadata prefix unknown " + metadataPrefix);
			}
		} catch (OrtolangException | KeyNotFoundException | CoreServiceException | IOException | BinaryStoreServiceException | DataNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unable to build oai_dc from ortolang object  " + key, e);
			throw new OaiServiceException("unable to build xml for oai record");
		}
	}

    private String getContent(InputStream is) throws IOException {
        String content = null;
        try {
            content = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "  unable to get content from stream", e);
        } finally {
            is.close();
        }
        return content;
    }

	@Override
	public String getServiceName() {
		return SERVICE_NAME;
	}

	@Override
	public Map<String, String> getServiceInfos() {
		return Collections.emptyMap();
	}

}
