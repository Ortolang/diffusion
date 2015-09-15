package fr.ortolang.diffusion.system;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInfo;
import org.hyperic.sigar.Sigar;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;

@Local(SystemService.class)
@Startup
@Singleton(name = SystemService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class SystemServiceBean implements SystemService {
    
    private static final Logger LOGGER = Logger.getLogger(SystemServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
    
    private Sigar sigar;
    
    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "initializing System Service");
        sigar = new Sigar();
    }

    @Override
    public String getServiceName() {
        return SystemService.SERVICE_NAME;
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String> ();
        try {
            NetInfo netinfo = sigar.getNetInfo();
            infos.put(INFO_HOSTNAME, netinfo.getHostName());
            infos.put(INFO_DOMAINNAME, netinfo.getDomainName());
            infos.put(INFO_DNS, netinfo.getPrimaryDns());
            infos.put(INFO_GATEWAY, netinfo.getDefaultGateway());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to collect network infos", e);
        }
        try {
            double[] load = sigar.getLoadAverage();
            infos.put(INFO_LOAD, Arrays.toString(load));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to collect load average", e);
        }
        try {
            Mem mem = sigar.getMem();
            infos.put(INFO_MEMTOTAL, Long.toString(mem.getTotal()));
            infos.put(INFO_MEMRAM, Long.toString(mem.getRam()));
            infos.put(INFO_MEMUSED, Long.toString(mem.getUsed()));
            infos.put(INFO_MEMFREE, Long.toString(mem.getFree()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to collect memory infos", e);
        }
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
    public OrtolangObject findObject(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }

}
