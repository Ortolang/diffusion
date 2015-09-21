package fr.ortolang.diffusion.store.json;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
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
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;

@Startup
@Local(JsonStoreService.class)
@Singleton(name = JsonStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@Lock(LockType.READ)
@PermitAll
public class JsonStoreServiceBean implements JsonStoreService {

	private static final Logger LOGGER = Logger.getLogger(JsonStoreServiceBean.class.getName());
	public static final String DEFAULT_JSON_HOME = "/json-store";
	
	private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
    
    private Path base;
	private OServer server;
	private OPartitionedDatabasePool pool;
	
	public JsonStoreServiceBean() {
		LOGGER.log(Level.FINE, "Instanciating json store service");
		this.base = Paths.get(OrtolangConfig.getInstance().getHomePath().toString(), DEFAULT_JSON_HOME);
	}

	public Path getBase() {
		return base;
	}

	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Initializing service with base folder: " + base);
		try {
			Files.createDirectories(base);
			server = OServerMain.create();
			server.startup(this.getClass().getResourceAsStream("/orientdb-config.xml"));
			server.activate();
			ODatabaseDocumentTx db = new ODatabaseDocumentTx("plocal:" + this.base.toFile().getAbsolutePath());
			try {
				if (!db.exists()) {
					db.create();
					db.command(new OCommandSQL("CREATE INDEX ortolangKey unique string")).execute();
				}
			} finally {
				db.close();
			}
			pool = new OPartitionedDatabasePool("plocal:" + this.base.toFile().getAbsolutePath(), "admin", "admin");
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to initialize json store", e);
		}
	}

	@PreDestroy
	public void shutdown() {
		LOGGER.log(Level.INFO, "Shuting down json store");
		try {
			pool.close();
			server.shutdown();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to shutdown json store", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<String> search(String query) throws JsonStoreServiceException {
		List<String> jsonResults = new ArrayList<String>();
		ODatabaseDocumentTx db = pool.acquire();
		try {
			List<ODocument> results = db.query(new OSQLSynchQuery<ODocument>(query));
			for (ODocument doc : results) {
				jsonResults.add(doc.toJSON());
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "query failed in json store : " + e.getMessage());
			throw new JsonStoreServiceException(e.getMessage());
		} finally {
			db.close();
		}
		return jsonResults;
	}

	@Override
	@Lock(LockType.WRITE)
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void index(OrtolangIndexableObject<IndexableJsonContent> object) throws JsonStoreServiceException {
		LOGGER.log(Level.FINE, "Indexing object with key: " + object.getKey());
		ODatabaseDocumentTx db = pool.acquire();
		try {
			ODocument doc = JsonStoreDocumentBuilder.buildDocument(object);
			ODocument oldDoc = getDocumentByKey(object.getKey());
			if (oldDoc != null) {
				oldDoc.delete();
			}
			db.save(doc);
			OIndex<?> ortolangKeyIdx = db.getMetadata().getIndexManager().getIndex("ortolangKey");
			ortolangKeyIdx.remove(object.getKey());
			ortolangKeyIdx.put(object.getKey(), doc);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to index object ", e);
		} finally {
			db.close();
		}
	}

	@Override
	@Lock(LockType.WRITE)
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void remove(String key) throws JsonStoreServiceException {
		LOGGER.log(Level.FINE, "Removing object with key: " + key);
		ODatabaseDocumentTx db = pool.acquire();
		try {
			ODocument oldDoc = getDocumentByKey(key);
			if (oldDoc != null) {
				oldDoc.delete();
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to remove object ", e);
		} finally {
			db.close();
		}
	}
	
	@Override
    @Lock(LockType.WRITE)
	@RolesAllowed("system")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void systemInsertDocument(String type, InputStream document) throws JsonStoreServiceException {
        LOGGER.log(Level.FINE, "Importing document type " + type);
        ODatabaseDocumentTx db = pool.acquire();
        try {
            ODocument doc = new ODocument(type).fromJSON(document);
            db.save(doc);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to import document", e);
        } finally {
            db.close();
        }
    }
	
	@Override
    @Lock(LockType.WRITE)
	@RolesAllowed("system")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void systemInsertDocument(String type, String document) throws JsonStoreServiceException {
        LOGGER.log(Level.FINE, "Importing document type " + type);
        ODatabaseDocumentTx db = pool.acquire();
        try {
            ODocument doc = new ODocument(type).fromJSON(document);
            db.save(doc);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to import document", e);
        } finally {
            db.close();
        }
    }
    
    @Override
	@RolesAllowed("system")
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String systemGetDocument(String key) throws JsonStoreServiceException {
        String json = getJSONByKey(key);
        if ( json != null ) {
            return json;
        }
        return null;
    }
    
    //Service methods
    
    @Override
    public String getServiceName() {
        return JsonStoreService.SERVICE_NAME;
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> getServiceInfos() {
        Map<String, String>infos = new HashMap<String, String> ();
        infos.put("pool.size.max", Integer.toString(pool.getMaxSize()));
        infos.put("pool.connections.availables", Integer.toString(pool.getAvailableConnections()));
        infos.put("pool.created.instances", Integer.toString(pool.getCreatedInstances()));
        infos.put("server.directory", server.getDatabaseDirectory());
        ODatabaseDocumentTx db = pool.acquire();
        try {
            infos.put("database.name", db.getName());
            infos.put("database.size", Long.toString(db.getSize()));
            infos.put("database.status", db.getStatus().toString());
        } finally {
            db.close();
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
	
	protected ODocument getDocumentByKey(String key) {
		ODatabaseDocumentTx db = pool.acquire();
		try {
			OIndex<?> ortolangKeyIdx = db.getMetadata().getIndexManager().getIndex("ortolangKey");
			OIdentifiable ident = (OIdentifiable) ortolangKeyIdx.get(key);
			if (ident != null) {
			    ODocument document = (ODocument) ident.getRecord();
			    return document;
			}
		} finally {
			db.close();
		}
		return null;
	}

	protected String getJSONByKey(String key) {
        ODatabaseDocumentTx db = pool.acquire();
        try {
            OIndex<?> ortolangKeyIdx = db.getMetadata().getIndexManager().getIndex("ortolangKey");
            OIdentifiable ident = (OIdentifiable) ortolangKeyIdx.get(key);
            if (ident != null) {
                ODocument document = (ODocument) ident.getRecord();
                return document.toJSON("fetchPlan:*:-1");
            }
        } finally {
            db.close();
        }
        return null;
    }
    

}
