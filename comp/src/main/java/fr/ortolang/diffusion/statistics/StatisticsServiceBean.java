package fr.ortolang.diffusion.statistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Schedule;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.statistics.entity.Value;

@Startup
@Local(StatisticsService.class)
@Singleton(name = StatisticsService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class StatisticsServiceBean implements StatisticsService {
    
    private static final Logger LOGGER = Logger.getLogger(StatisticsServiceBean.class.getName());
    
    private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
    private static final String SEPARATOR = ".";
    private static final Map<String, List<String>> STATS_NAMES = new HashMap<String, List<String>> ();
    
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @Resource
    private SessionContext ctx;
    
    public StatisticsServiceBean() {
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
        String[] registryInfos = new String[] { RegistryService.INFO_SIZE };
        STATS_NAMES.put(RegistryService.SERVICE_NAME, Arrays.asList(registryInfos));
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> list() throws StatisticsServiceException {
        LOGGER.log(Level.FINEST, "listing all stats names");
        List<String> names = new ArrayList<String> ();
        for ( Entry<String, List<String>> stat : STATS_NAMES.entrySet() ) {
            for ( String info : stat.getValue() ) {
                names.add(stat.getKey() + SEPARATOR + info);
            }
        }
        return names;
    }

    @Override
    @Schedule(minute="*/5", hour="*")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void probe() throws StatisticsServiceException {
        LOGGER.log(Level.FINEST, "probing stat for fresh values");
        for ( Entry<String, List<String>> stat : STATS_NAMES.entrySet() ) {
            LOGGER.log(Level.FINEST, "looking up service: " + stat.getKey());
            try {
                OrtolangService service = OrtolangServiceLocator.findService(stat.getKey());
                Map<String, String> infos = service.getServiceInfos();
                for ( String info : stat.getValue() ) {
                    if ( infos.containsKey(info) ) {
                        try {
                            Value value = new Value(stat.getKey() + SEPARATOR + info, System.currentTimeMillis(), infos.get(info));
                            em.persist(value);
                            LOGGER.log(Level.FINEST, "value persisted for stat name: " + value.getName());
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "unable to persist value", e);
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "unable to probe service: " + stat.getKey() + " for info: " + info);
                    }
                }
            } catch ( OrtolangException e ) {
                LOGGER.log(Level.WARNING, "unable to probe some stats", e);
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String[] read(String name) throws StatisticsServiceException, StatisticNameNotFoundException {
        LOGGER.log(Level.FINEST, "reading stat value for name : " + name);
        TypedQuery<Value> query = em.createNamedQuery("findValuesForName", Value.class).setParameter("name", name).setMaxResults(1);
        Value v = query.getSingleResult();
        if ( v == null ) {
            throw new StatisticNameNotFoundException("unable to find a value for stat with name: " + name);
        } else {
            String[] value = new String[2];
            value[0] = Long.toString(v.getTimestamp());
            value[1] = v.getValue();
            return value;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> history(String name, long from, long to) throws StatisticsServiceException, StatisticNameNotFoundException {
        LOGGER.log(Level.FINEST, "reading stat history for name : " + name);
        TypedQuery<Value> query = em.createNamedQuery("findValuesForNameFromTo", Value.class).setParameter("name", name).setParameter("from", from).setParameter("to", to);
        List<Value> values = query.getResultList();
        if ( values == null ) {
            throw new StatisticNameNotFoundException("unable to find values for stat with name: " + name);
        } else {
            Map<String, String> result = new HashMap<String, String> ();
            for ( Value value : values ) {
                result.put(Long.toBinaryString(value.getTimestamp()), value.getValue());
            }
            return result;
        }
    }
    
    //Service methods
    
    @Override
    public String getServiceName() {
        return RegistryService.SERVICE_NAME;
    }
    
    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String>infos = new HashMap<String, String> ();
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
 