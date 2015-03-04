package fr.ortolang.diffusion.store.json;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import nl.renarj.jasdb.LocalDBSession;
import nl.renarj.jasdb.api.DBSession;
import nl.renarj.jasdb.api.SimpleEntity;
import nl.renarj.jasdb.api.model.EntityBag;
import nl.renarj.jasdb.core.SimpleKernel;
import nl.renarj.jasdb.core.exceptions.JasDBStorageException;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.store.DeleteFileVisitor;

@Local(JsonStoreService.class)
@Startup
@Singleton(name = JsonStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@Lock(LockType.READ)
@PermitAll
public class JsonStoreServiceBean implements JsonStoreService {

	public static final String DEFAULT_JSON_HOME = "/json-store";
	public static final String DEFAULT_ORTOLANG_INSTANCE = "ortolang";
	public static final String DEFAULT_ORTOLANG_BAG = "ortolang";

    private Logger logger = Logger.getLogger(JsonStoreServiceBean.class.getName());

    private Path base;
    private DBSession session;
    private EntityBag bag;

    public JsonStoreServiceBean() {
    	logger.log(Level.FINE, "Instanciating json store service");
    	this.base = Paths.get(OrtolangConfig.getInstance().getHome(), DEFAULT_JSON_HOME);
    	this.session = null;
    	this.bag = null;
    }
    
    public Path getBase() {
    	return base;
    }

    @PostConstruct
    public void init() {
    	logger.log(Level.INFO, "Initializing service with base folder: " + base);
    	//Forcible initialize JasDB, can also be lazy loaded on first session created
    	try {
    		if ( Files.exists(base) && Boolean.parseBoolean(OrtolangConfig.getInstance().getProperty("store.json.purge")) ) {
				logger.log(Level.FINEST, "base directory exists and config is set to purge, recursive delete of base folder");
				Files.walkFileTree(base, new DeleteFileVisitor());
			} 
    		Files.createDirectories(base);
    		
			SimpleKernel.initializeKernel();
			
			session = new LocalDBSession();
			
			try {
				session.getInstance(DEFAULT_ORTOLANG_INSTANCE);
			} catch(Exception e) {
				logger.log(Level.WARNING, "unable to get instance of json-store named "+DEFAULT_ORTOLANG_INSTANCE);
				session.addInstance(DEFAULT_ORTOLANG_INSTANCE, base.toFile().getAbsolutePath());
			}
			bag = session.createOrGetBag(DEFAULT_ORTOLANG_BAG);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "unable to initialize json store", e);
		}
    }
 

    @PreDestroy
    public void shutdown() {
    	logger.log(Level.INFO, "Shuting down json store");
    	try {
    		session.closeSession();
    		SimpleKernel.shutdown();
        } catch (Exception e) {
    		logger.log(Level.SEVERE, "unable to shutdown json store", e);
    	}
    }
    
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void index(OrtolangIndexableObject object)
			throws JsonStoreServiceException {
		
		try {
			bag.addEntity(JsonStoreEntityBuilder.buildEntity(object));
		} catch (JasDBStorageException e) {
			logger.log(Level.WARNING, "unable to index json of object " + object, e);
			throw new JsonStoreServiceException("Can't index the json of an object", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void reindex(OrtolangIndexableObject object)
			throws JsonStoreServiceException {
		logger.log(Level.FINE, "Reindexing object: " + object.getKey());
		try {
			bag.updateEntity(JsonStoreEntityBuilder.buildEntity(object));
		} catch (JasDBStorageException e) {
			logger.log(Level.WARNING, "unable to reindex json of object " + object, e);
			throw new JsonStoreServiceException("Can't reindex the json of an object", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void remove(String key) throws JsonStoreServiceException {
		logger.log(Level.FINE, "Removing key: " + key);
		try {
			bag.removeEntity(key);
		} catch (JasDBStorageException e) {
			logger.log(Level.WARNING, "unable to remove json of object " + key, e);
			throw new JsonStoreServiceException("Can't remove the json of an object", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> search(String query)
			throws JsonStoreServiceException {
		
		//TODO Parse query
		// ex. field1:value1 AND field2:value2

		List<String> jsonResults = new ArrayList<String>();
		try {
			for(SimpleEntity entity : bag.getEntities()) {
				jsonResults.add(SimpleEntity.toJson(entity));
			}
		} catch (JasDBStorageException e) {
			logger.log(Level.WARNING, "unable search in json-store using query : " + query, e);
			throw new JsonStoreServiceException("Can't search in json-store using query : '" + query + "'\n", e);
		}
		return jsonResults;
	}

}
