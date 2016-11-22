package fr.ortolang.diffusion.oai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
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

import fr.ortolang.diffusion.oai.entity.Record;

@Local(OaiService.class)
@Stateless(name = OaiService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class OaiServiceBean implements OaiService {

    private static final Logger LOGGER = Logger.getLogger(OaiServiceBean.class.getName());
    
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
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
    public List<Record> listRecordsByMetadataPrefix(String metadataPrefix, Long from, Long until) throws RecordNotFoundException, OaiServiceException {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Record> criteria = builder.createQuery(Record.class);
        Root<Record> customer = criteria.from(Record.class);
        List<Predicate> predicates = new ArrayList<Predicate>();
        
        if (metadataPrefix == null) {
            throw new OaiServiceException("metadataPrefix argument missing");
        }
        
        predicates.add(builder.equal(customer.get("metadataPrefix"), metadataPrefix));
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
	    String id = UUID.randomUUID().toString();
	    //TODO check if already exist
    	Record record = new Record(id, identifier, metadataPrefix, lastModificationDate, xml);
    	em.persist(record);
    	return record;
    }
    
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Record readRecord(String id) throws RecordNotFoundException {
        LOGGER.log(Level.FINE, "reading record with id " + id);
        try {
	        return em.find(Record.class, id);
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
	public String getServiceName() {
		return SERVICE_NAME;
	}

	@Override
	public Map<String, String> getServiceInfos() {
		return Collections.emptyMap();
	}

}
