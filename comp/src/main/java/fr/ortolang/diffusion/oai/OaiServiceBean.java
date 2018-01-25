package fr.ortolang.diffusion.oai;

import java.util.ArrayList;
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

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.entity.Set;
import fr.ortolang.diffusion.oai.exception.OaiServiceException;
import fr.ortolang.diffusion.oai.exception.RecordNotFoundException;
import fr.ortolang.diffusion.oai.exception.SetAlreadyExistsException;
import fr.ortolang.diffusion.oai.exception.SetNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreService;

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
	public List<Record> listRecords() {
		TypedQuery<Record> query = em.createNamedQuery("findRecords", Record.class);
		return query.getResultList();
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

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long countSets() {
		try {
			TypedQuery<Long> query = em.createNamedQuery("countSets", Long.class);
			return query.getSingleResult();
		} catch (Exception e) {
		}
		return 0;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long countRecords() {
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
