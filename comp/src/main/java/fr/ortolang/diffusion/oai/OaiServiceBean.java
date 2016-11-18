package fr.ortolang.diffusion.oai;

import java.util.Collections;
import java.util.Map;
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
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Record createRecord(String key, String metadataPrefix, long lastModificationDate, String xml) {
    	Record record = new Record(key, metadataPrefix, lastModificationDate, xml);
    	em.persist(record);
    	return record;
    }
    
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Record readRecord(String key) throws RecordNotFoundException {
        LOGGER.log(Level.FINE, "reading record : " + key);
        try {
	        return em.createNamedQuery("findRecordBykey", Record.class).setParameter("key", key.getBytes()).getSingleResult();
        } catch (NoResultException e) {
        	throw new RecordNotFoundException("unable to find a record with key: " + key);
        }
	}
	
	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteRecord(String key) throws RecordNotFoundException {
		Record record = readRecord(key);
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
