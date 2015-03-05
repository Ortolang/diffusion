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

import org.jboss.ejb3.annotation.SecurityDomain;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

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
    private OServer server;
    private ODatabaseDocumentTx db;

    public JsonStoreServiceBean() {
    	logger.log(Level.FINE, "Instanciating json store service");
    	this.base = Paths.get(OrtolangConfig.getInstance().getHome(), DEFAULT_JSON_HOME);
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
    		
    		server = OServerMain.create();
//    	    server.startup(server.getClass().getResourceAsStream("orientdb-config.xml"));
    		server.startup(
    				   "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    				   + "<orient-server>"
    				   + "<network>"
    				   + "<protocols>"
    				   + "<protocol name=\"binary\" implementation=\"com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary\"/>"
    				   + "<protocol name=\"http\" implementation=\"com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb\"/>"
    				   + "</protocols>"
    				   + "<listeners>"
    				   + "<listener ip-address=\"0.0.0.0\" port-range=\"2424-2430\" protocol=\"binary\"/>"
    				   + "<listener ip-address=\"0.0.0.0\" port-range=\"2480-2490\" protocol=\"http\"/>"
    				   + "</listeners>"
    				   + "</network>"
    				   + "<users>"
    				   + "<user name=\"root\" password=\"ThisIsA_TEST\" resources=\"*\"/>"
    				   + "</users>"
    				   + "<properties>"
    				   + "<entry name=\"orientdb.www.path\" value=\"C:/work/dev/orientechnologies/orientdb/releases/1.0rc1-SNAPSHOT/www/\"/>"
    				   + "<entry name=\"orientdb.config.file\" value=\"C:/work/dev/orientechnologies/orientdb/releases/1.0rc1-SNAPSHOT/config/orientdb-server-config.xml\"/>"
    				   + "<entry name=\"server.cache.staticResources\" value=\"false\"/>"
    				   + "<entry name=\"log.console.level\" value=\"info\"/>"
    				   + "<entry name=\"log.file.level\" value=\"fine\"/>"
    				   //The following is required to eliminate an error or warning "Error on resolving property: ORIENTDB_HOME"
    				   + "<entry name=\"plugin.dynamic\" value=\"false\"/>"
    				   + "</properties>" + "</orient-server>");
    	    server.activate();
    	    
    	    db = new ODatabaseDocumentTx("plocal:"+this.base.toFile().getAbsolutePath());

    	    if(db.exists()) {
    	    	db.open("admin","admin"); //TODO set user pass
    	    } else {
    	    	db.create();
    	    }
    	    
		} catch (Exception e) {
			logger.log(Level.SEVERE, "unable to initialize json store", e);
		}
    }
 

    @PreDestroy
    public void shutdown() {
    	logger.log(Level.INFO, "Shuting down json store");
    	try {
    		server.shutdown();
        } catch (Exception e) {
    		logger.log(Level.SEVERE, "unable to shutdown json store", e);
    	}
    }
    
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void index(OrtolangIndexableObject object)
			throws JsonStoreServiceException {
		
		try {
			db.begin();
			
			db.save(JsonStoreDocumentBuilder.buildDocument(object));
		} catch(Exception e) {
			db.rollback();
		} finally {
			db.close();
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void reindex(OrtolangIndexableObject object)
			throws JsonStoreServiceException {
		logger.log(Level.FINE, "Reindexing object: " + object.getKey());

		try {
			db.begin();
			
			db.save(JsonStoreDocumentBuilder.buildDocument(object));
		} catch(Exception e) {
			db.rollback();
		} finally {
			db.close();
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void remove(String key) throws JsonStoreServiceException {
		logger.log(Level.FINE, "Removing key: " + key);
		
		try {
			db.begin();
			
//			db.delete(JsonStoreDocumentBuilder.buildDocument(object));
		} catch(Exception e) {
			db.rollback();
		} finally {
			db.close();
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> search(String query)
			throws JsonStoreServiceException {
		
		//TODO Parse query
		// ex. field1:value1 AND field2:value2

		List<String> jsonResults = new ArrayList<String>();
//		try {
//			for(SimpleEntity entity : bag.getEntities()) {
//				jsonResults.add(SimpleEntity.toJson(entity));
//			}
//		} catch (JasDBStorageException e) {
//			logger.log(Level.WARNING, "unable search in json-store using query : " + query, e);
//			throw new JsonStoreServiceException("Can't search in json-store using query : '" + query + "'\n", e);
//		}
		return jsonResults;
	}

}
