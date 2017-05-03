package fr.ortolang.diffusion.oai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.entity.Set;
import fr.ortolang.diffusion.oai.exception.MetadataPrefixUnknownException;
import fr.ortolang.diffusion.oai.exception.OaiServiceException;
import fr.ortolang.diffusion.oai.exception.RecordNotFoundException;
import fr.ortolang.diffusion.oai.exception.SetAlreadyExistsException;
import fr.ortolang.diffusion.oai.exception.SetNotFoundException;
import fr.ortolang.diffusion.oai.format.DCXMLDocument;
import fr.ortolang.diffusion.oai.format.OAI_DCFactory;
import fr.ortolang.diffusion.oai.format.OLACFactory;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;
import fr.ortolang.diffusion.util.StreamUtils;

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
	@EJB
	private SearchService search;
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
		TypedQuery<Record> query = em.createNamedQuery("findRecordsByIdentifier", Record.class)
				.setParameter("identifier", identifier);
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
	public List<Record> listRecordsByMetadataPrefix(String metadataPrefix, Long from, Long until)
			throws RecordNotFoundException, OaiServiceException {
		return listRecordsByMetadataPrefixAndSetspec(metadataPrefix, null, from, until);
	}

	public List<Record> listRecordsByMetadataPrefixAndSetspec(String metadataPrefix, String setSpec, Long from,
			Long until) throws RecordNotFoundException, OaiServiceException {
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
		criteria.select(customer).where(predicates.toArray(new Predicate[] {}));

		List<Record> records = em.createQuery(criteria).getResultList();
		if (records == null || records.isEmpty()) {
			throw new RecordNotFoundException("unable to list records with metadataPrefix: " + metadataPrefix);
		}
		return records;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Record findRecord(String identifier, String metadataPrefix) throws RecordNotFoundException {
		LOGGER.log(Level.FINE,
				"finding record with identifier " + identifier + " and metadataPrefix " + metadataPrefix);
		try {
			return em.createNamedQuery("findRecordsByIdentifierAndMetadataPrefix", Record.class)
					.setParameter("identifier", identifier).setParameter("metadataPrefix", metadataPrefix)
					.getSingleResult();
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
	public Record createRecord(String identifier, String metadataPrefix, long lastModificationDate, String xml,
			java.util.Set<String> sets) {
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

	/**
	 * Builds a set and a record for a workspace. For each elements (Collection
	 * and DataObject) with a Metadata DublinCore or OLAC, a record is created
	 * and associated to the set.
	 * 
	 * If any exception is thrown, the transaction rollbacks.
	 * 
	 * @param wskey
	 *            the workspace key
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void buildFromWorkspace(String wskey) throws OaiServiceException {

		buildFromWorkspace(wskey, null);
	}

	/**
	 * Builds a set and a record for a workspace. For each elements (Collection
	 * and DataObject) with a Metadata DublinCore or OLAC, a record is created
	 * and associated to the set.
	 * 
	 * If any exception is thrown, the transaction rollbacks.
	 * 
	 * @param wskey
	 *            the workspace key
	 * @param snapshot
	 *            specify a snapshot name
	 */
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void buildFromWorkspace(String wskey, String snapshot) throws OaiServiceException {
		try {
			HashSet<String> setsWorkspace = new HashSet<String>(Arrays.asList(wskey));

			if (snapshot == null) {
				snapshot = core.findWorkspaceLatestPublishedSnapshot(wskey);
			}
			if (snapshot == null) {
				LOGGER.log(Level.WARNING, "finds no published snapshot for workspace " + wskey);
				return;
			}
			LOGGER.log(Level.FINE, "build from workspace " + wskey + " and snapshot " + snapshot);
			Workspace workspace = core.readWorkspace(wskey);
			String root = workspace.findSnapshotByName(snapshot).getKey();

			List<Record> records = null;
			try {
				records = listRecordsByIdentifier(wskey);
			} catch (RecordNotFoundException e) {
			}
			if (records == null) {
				LOGGER.log(Level.FINE, "creating OAI records and a set for workspace " + wskey);
				// Creating a Set for the workspace
				try {
					createSet(wskey, "Workspace " + wskey);
				} catch (SetAlreadyExistsException e) {
					LOGGER.log(Level.WARNING, "unable to create a Set " + wskey, e);
				}
				createRecordsForItem(wskey, root, setsWorkspace);
			} else {
				LOGGER.log(Level.FINE, "updating OAI records and set for workspace " + wskey);
				// Set is already created (see below), so just cleans and
				// creates new records
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
				createRecordsForItem(wskey, root, setsWorkspace);
			}
		} catch (RegistryServiceException | KeyNotFoundException | OaiServiceException | CoreServiceException
				| MetadataPrefixUnknownException | OrtolangException e) {
			LOGGER.log(Level.SEVERE, "unable to create OAI record for workspace " + wskey, e);
			throw new OaiServiceException("unable to create OAI records for workspace " + wskey, e);
		}
	}

	private void createRecordsForItem(String wskey, String root, HashSet<String> setsWorkspace)
			throws CoreServiceException, KeyNotFoundException, RegistryServiceException, OaiServiceException,
			MetadataPrefixUnknownException, OrtolangException {
		createRecord(wskey, MetadataFormat.OAI_DC, registry.getLastModificationDate(root),
				buildXMLFromItem(root, MetadataFormat.OAI_DC), setsWorkspace);
		createRecord(wskey, MetadataFormat.OLAC, registry.getLastModificationDate(root),
				buildXMLFromItem(root, MetadataFormat.OLAC), setsWorkspace);

		createRecordsFromMetadataObject(root, setsWorkspace);
	}

	/**
	 * Creates records (one for each metadata format) related to an
	 * OrtolangObject.
	 * 
	 * @param key
	 * @param setsWorkspace
	 *            the sets related
	 * @throws OrtolangException
	 * @throws OaiServiceException
	 * @throws RegistryServiceException
	 * @throws KeyNotFoundException
	 * @throws CoreServiceException
	 * @throws MetadataPrefixUnknownException
	 */
	private void createRecordsFromMetadataObject(String key, HashSet<String> setsWorkspace)
			throws OrtolangException, OaiServiceException, RegistryServiceException, KeyNotFoundException,
			CoreServiceException, MetadataPrefixUnknownException {
		OrtolangObject object = core.findObject(key);
		String type = object.getObjectIdentifier().getType();

		switch (type) {
		case Collection.OBJECT_TYPE:
			createRecordsForEarchMetadataObject(key, setsWorkspace);
			java.util.Set<CollectionElement> elements = ((Collection) object).getElements();
			for (CollectionElement element : elements) {
				createRecordsFromMetadataObject(element.getKey(), setsWorkspace);
			}
			break;
		case DataObject.OBJECT_TYPE:
			createRecordsForEarchMetadataObject(key, setsWorkspace);
			break;
		}
	}

	/**
	 * Creates records for each metadata format availabled on the
	 * OrtolangObject.
	 * 
	 * @param key
	 * @param setsWorkspace
	 * @throws OaiServiceException
	 * @throws RegistryServiceException
	 * @throws KeyNotFoundException
	 * @throws MetadataPrefixUnknownException
	 */
	private void createRecordsForEarchMetadataObject(String key, HashSet<String> setsWorkspace)
			throws OaiServiceException, RegistryServiceException, KeyNotFoundException, MetadataPrefixUnknownException {

		String olac = buildXMLFromMetadataObject(key, MetadataFormat.OLAC);
		String oai_dc = buildXMLFromMetadataObject(key, MetadataFormat.OAI_DC);

		if (oai_dc != null) {
			createRecord(key, MetadataFormat.OAI_DC, registry.getLastModificationDate(key), oai_dc, setsWorkspace);
		} else {
			if (olac != null) {
				oai_dc = buildXMLFromMetadataObject(key, MetadataFormat.OLAC, MetadataFormat.OAI_DC);
				createRecord(key, MetadataFormat.OAI_DC, registry.getLastModificationDate(key), oai_dc, setsWorkspace);
			}
		}

		if (olac != null) {
			createRecord(key, MetadataFormat.OLAC, registry.getLastModificationDate(key), olac, setsWorkspace);
		} else {
			if (oai_dc != null) {
				olac = buildXMLFromMetadataObject(key, MetadataFormat.OAI_DC, MetadataFormat.OLAC);
				createRecord(key, MetadataFormat.OLAC, registry.getLastModificationDate(key), olac, setsWorkspace);
			}
		}
	}

	/**
	 * Builds XML from root collection metadata (Item).
	 * 
	 * @param wskey
	 * @param snapshot
	 * @param metadataPrefix
	 * @return
	 * @throws OaiServiceException
	 * @throws MetadataPrefixUnknownException
	 */
	private String buildXMLFromItem(String root, String metadataPrefix)
			throws OaiServiceException, MetadataPrefixUnknownException {
		LOGGER.log(Level.FINE,
				"building XML from ITEM metadata of root collection " + root + " and metadataPrefix " + metadataPrefix);

		String item = null;
		try {
			List<OrtolangIndexableContent> indexableContents = core.getIndexableContent(root);
			if (indexableContents.size() > 0) {
				item = indexableContents.get(0).getContent();
			}
		} catch (KeyNotFoundException | RegistryServiceException | IndexingServiceException | OrtolangException e1) {
			LOGGER.log(Level.SEVERE, "unable to get json content from root collection " + root);
			throw new OaiServiceException("unable to get json content from root collection " + root, e1);
		}

		if (item == null) {
			LOGGER.log(Level.SEVERE, "unable to build xml from root collection cause item metadata is null " + root);
			throw new OaiServiceException("unable to build xml from root collection cause item metadata " + root);
		}

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
			for (String handle : handles) {
				xml.addDcField("identifier", "http://hdl.handle.net/" + handle);
			}
		} catch (NullPointerException | ClassCastException | HandleStoreServiceException e) {
			LOGGER.log(Level.WARNING, "No handle for key " + root, e);
		}
		if (xml != null) {
			return xml.toString();
		} else {
			throw new MetadataPrefixUnknownException(
					"unable to build xml for oai record cause metadata prefix unknown " + metadataPrefix);
		}
	}

	/**
	 * Builds XML from OrtolangObject.
	 * 
	 * @param key
	 * @param metadataPrefix
	 * @return
	 * @throws OaiServiceException
	 */
	private String buildXMLFromMetadataObject(String key, String metadataPrefix)
			throws OaiServiceException, MetadataPrefixUnknownException {
		return buildXMLFromMetadataObject(key, metadataPrefix, metadataPrefix);
	}

	/**
	 * Builds XML from OrtolangObject.
	 * 
	 * @param key
	 * @param metadataPrefix
	 * @return
	 * @throws OaiServiceException
	 */
	private String buildXMLFromMetadataObject(String key, String metadataPrefix, String outputMetadataFormat)
			throws OaiServiceException, MetadataPrefixUnknownException {
		LOGGER.log(Level.FINE,
				"creating OAI record for ortolang object " + key + " for metadataPrefix " + metadataPrefix);
		try {
			List<String> mdKeys = core.findMetadataObjectsForTargetAndName(key, metadataPrefix);
			DCXMLDocument xml = null;

			if (!mdKeys.isEmpty()) {
				String mdKey = mdKeys.get(0);
				MetadataObject md = core.readMetadataObject(mdKey);
				if (outputMetadataFormat.equals(MetadataFormat.OAI_DC) && (metadataPrefix.equals(MetadataFormat.OAI_DC)
						|| metadataPrefix.equals(MetadataFormat.OLAC))) {
					if (metadataPrefix.equals(MetadataFormat.OAI_DC)) {
						xml = OAI_DCFactory.buildFromJson(StreamUtils.getContent(binaryStore.get(md.getStream())));
					} else if (metadataPrefix.equals(MetadataFormat.OLAC)) {
						xml = OAI_DCFactory
								.convertFromJsonOlac(StreamUtils.getContent(binaryStore.get(md.getStream())));
					}
				} else if (outputMetadataFormat.equals(MetadataFormat.OLAC)
						&& (metadataPrefix.equals(MetadataFormat.OLAC)
								|| metadataPrefix.equals(MetadataFormat.OAI_DC))) {
					xml = OLACFactory.buildFromJson(StreamUtils.getContent(binaryStore.get(md.getStream())));
				}
			} else {
				return null;
			}

			// Automatically adds handles to 'identifier' XML element
			List<String> handles;
			try {
				handles = handleStore.listHandlesForKey(key);
				for (String handle : handles) {
					xml.addDcField("identifier", "http://hdl.handle.net/" + handle);
				}
			} catch (NullPointerException | ClassCastException | HandleStoreServiceException e) {
				LOGGER.log(Level.WARNING, "No handle for key " + key, e);
			}
			if (xml != null) {
				return xml.toString();
			} else {
				throw new MetadataPrefixUnknownException(
						"unable to build xml for oai record cause metadata prefix unknown " + metadataPrefix);
			}
		} catch (OrtolangException | KeyNotFoundException | CoreServiceException | IOException
				| BinaryStoreServiceException | DataNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unable to build oai_dc from ortolang object  " + key, e);
			throw new OaiServiceException("unable to build xml for oai record");
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private long countSets() {
		try {
			TypedQuery<Long> query = em.createNamedQuery("countSets", Long.class);
			return query.getSingleResult();
		} catch (Exception e) {
		}
		return 0;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	private long countRecords() {
		try {
			TypedQuery<Long> query = em.createNamedQuery("countRecordsGroupByIdentifier", Long.class);
			return query.getSingleResult();
		} catch (Exception e) {
		}
		return 0;
	}

	@Override
	public String getServiceName() {
		return SERVICE_NAME;
	}

	@Override
	public Map<String, String> getServiceInfos() {
		Map<String, String> infos = new HashMap<String, String>();
		infos.put(INFO_COUNT_SETS, Long.toString(countSets()));
		infos.put(INFO_COUNT_RECORDS, Long.toString(countRecords()));
		return infos;
	}
}
