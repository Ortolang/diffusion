package fr.ortolang.diffusion.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Startup
@Local(StatisticsService.class)
@Singleton(name = StatisticsService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class StatisticsServiceBean implements StatisticsService {
    
    private static final  Logger LOGGER = Logger.getLogger(StatisticsServiceBean.class.getName());
    
    private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
    
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @Resource
    private SessionContext ctx;
    private Map<String, Runnable> collectors;
    
    public StatisticsServiceBean() {
        collectors = new HashMap<String, Runnable> ();
    }
    
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEntityManager() {
        return this.em;
    }

    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext() {
        return this.ctx;
    }
    
    @PostConstruct
    private void init() {
        LOGGER.log(Level.FINE, "Initializing statistics service, building collectors");
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Set<String> list() throws StatisticsServiceException {
        return collectors.keySet();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String read(String name) throws StatisticsServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String probe(String name) throws StatisticsServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> history(String name, long from, long to) throws StatisticsServiceException {
        // TODO Auto-generated method stub
        return null;
    }
    
    //Service methods
    
    @Override
    public String getServiceName() {
        return RegistryService.SERVICE_NAME;
    }
    
    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String>infos = new HashMap<String, String> ();
        //TODO
        return infos;
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        return OBJECT_PERMISSIONS_LIST;
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException, AccessDeniedException, KeyNotFoundException {
        throw new OrtolangException("this service does not managed any object");
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
        throw new OrtolangException("this service does not managed any object");
    }

}
