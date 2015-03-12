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

import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
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

    private Logger logger = Logger.getLogger(JsonStoreServiceBean.class.getName());

    private Path base;
    private OServer server;
    private OPartitionedDatabasePool pool;

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
    	try {
    		if ( Files.exists(base) && Boolean.parseBoolean(OrtolangConfig.getInstance().getProperty("store.json.purge")) ) {
				logger.log(Level.FINEST, "base directory exists and config is set to purge, recursive delete of base folder");
				Files.walkFileTree(base, new DeleteFileVisitor());
			} 
    		Files.createDirectories(base);
    		
    		server = OServerMain.create();
    	    server.startup(this.getClass().getResourceAsStream("/orientdb-config.xml"));
    	    server.activate();
    	    
    	    ODatabaseDocumentTx db = new ODatabaseDocumentTx("plocal:"+this.base.toFile().getAbsolutePath());

    	    try {
	    	    if(!db.exists()) {
	    	    	db.create();
	    	    	
	    	    	db.command(new OCommandSQL("CREATE INDEX ortolangKey unique string")).execute();
	    	    }
	    	} finally {
	    		db.close();
	    	}
    	    
    	    pool = new OPartitionedDatabasePool("plocal:"+this.base.toFile().getAbsolutePath(), "admin", "admin");
    	    
		} catch (Exception e) {
			logger.log(Level.SEVERE, "unable to initialize json store", e);
		}
    }
 

    @PreDestroy
    public void shutdown() {
    	logger.log(Level.INFO, "Shuting down json store");
    	try {
    		pool.close();
    		server.shutdown();
        } catch (Exception e) {
    		logger.log(Level.SEVERE, "unable to shutdown json store", e);
    	}
    }
    
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void index(OrtolangIndexableObject object)
			throws JsonStoreServiceException {
		logger.log(Level.INFO, "Indexing object: " + object.getKey());
		
		ODatabaseDocumentTx db = pool.acquire();
		try {
			ODocument doc = JsonStoreDocumentBuilder.buildDocument(object);
			db.save(doc);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "unable to index json ",e);
		} finally {
			db.close();
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void reindex(OrtolangIndexableObject object)
			throws JsonStoreServiceException {
		logger.log(Level.FINE, "Reindexing object: " + object.getKey());

		ODatabaseDocumentTx db = pool.acquire();
		try {
			ODocument oldDoc = getDocumentByKey(object.getKey());
			oldDoc.delete();
			ODocument doc = JsonStoreDocumentBuilder.buildDocument(object);
			db.save(doc);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "unable to reindex json ",e);
		} finally {
			db.close();
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void remove(String key) throws JsonStoreServiceException {
		logger.log(Level.FINE, "Removing key: " + key);
		
		ODatabaseDocumentTx db = pool.acquire();
		try {
			ODocument oldDoc = getDocumentByKey(key);
			oldDoc.delete();
		} catch(Exception e) {
			logger.log(Level.SEVERE, "unable to remove json ",e);
		} finally {
			db.close();
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> search(String query)
			throws JsonStoreServiceException {

		List<String> jsonResults = new ArrayList<String>();

		ODatabaseDocumentTx db = pool.acquire();
		try {
			List<ODocument> results = db.query(new OSQLSynchQuery<ODocument>(query));
			for(ODocument doc : results) {
				jsonResults.add(doc.toJSON());
		    }
		} finally {
			db.close();
		}
		return jsonResults;
	}
	
	protected ODocument getDocumentByKey(String key) {

		ODatabaseDocumentTx db = pool.acquire();
		try {
			OIndex<?> ortolangKeyIdx = db.getMetadata().getIndexManager().getIndex("ortolangKey");
			OIdentifiable doc = (OIdentifiable) ortolangKeyIdx.get(key);
			if( doc != null )
				return (ODocument) doc.getRecord();
			
		} finally {
			db.close();
		}
		return null;
	}

}
