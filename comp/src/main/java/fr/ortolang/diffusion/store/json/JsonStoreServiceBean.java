package fr.ortolang.diffusion.store.json;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.*;

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
        LOGGER.log(Level.FINE, "Instantiating json store service");
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
            try (ODatabaseDocumentTx db = new ODatabaseDocumentTx("plocal:" + this.base.toFile().getAbsolutePath())) {
                if (!db.exists()) {
                    db.create();
                    db.command(new OCommandSQL("CREATE INDEX ortolangKey unique string")).execute();
                }
            }
            pool = new OPartitionedDatabasePool("plocal:" + this.base.toFile().getAbsolutePath(), "admin", "admin");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to initialize json store", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.log(Level.INFO, "Shutting down json store");
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
        try (ODatabaseDocumentTx db = pool.acquire()) {
            List<ODocument> results = db.query(new OSQLSynchQuery<ODocument>(query));
            for (ODocument doc : results) {
                jsonResults.add(doc.toJSON());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "query failed in json store : " + e.getMessage());
            throw new JsonStoreServiceException(e.getMessage());
        }
        return jsonResults;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ODocument> systemSearch(String query) throws JsonStoreServiceException {
        try (ODatabaseDocumentTx db = pool.acquire()) {
            return db.query(new OSQLSynchQuery<ODocument>(query));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "query failed in json store : " + e.getMessage());
            throw new JsonStoreServiceException(e.getMessage());
        }
    }

	@Override
	@Lock(LockType.WRITE)
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void index(OrtolangIndexableObject<IndexableJsonContent> object) throws JsonStoreServiceException {
		LOGGER.log(Level.FINE, "Indexing object with key: " + object.getKey());
		try (ODatabaseDocumentTx db = pool.acquire()) {
			ODocument oldDoc = getDocumentByKey(object.getKey());
			ODocument doc = JsonStoreDocumentBuilder.buildDocument(object, oldDoc);
			//			if (oldDoc != null) {
			//				oldDoc.delete();
			//			}
			db.save(doc);
			OIndex<?> ortolangKeyIdx = db.getMetadata().getIndexManager().getIndex("ortolangKey");
			ortolangKeyIdx.remove(object.getKey());
			ortolangKeyIdx.put(object.getKey(), doc);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to index object ", e);
		}
	}

    @Override
    @Lock(LockType.WRITE)
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void remove(String key) throws JsonStoreServiceException {
        LOGGER.log(Level.FINE, "Removing object with key: " + key);
        try (ODatabaseDocumentTx db = pool.acquire()) {
            ODocument oldDoc = getDocumentByKey(key);
            if (oldDoc != null) {
                oldDoc.delete();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to remove object ", e);
        }
    }

    @Override
    @Lock(LockType.WRITE)
    @RolesAllowed("admin")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void systemInsertDocument(String type, InputStream document) throws JsonStoreServiceException {
        LOGGER.log(Level.FINE, "Importing document type " + type);
        try (ODatabaseDocumentTx db = pool.acquire()) {
            ODocument doc = new ODocument(type).fromJSON(document);
            db.save(doc);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to import document", e);
        }
    }

    @Override
    @Lock(LockType.WRITE)
    @RolesAllowed("admin")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void systemInsertDocument(String type, String document) throws JsonStoreServiceException {
        LOGGER.log(Level.FINE, "Importing document type " + type);
        try (ODatabaseDocumentTx db = pool.acquire()) {
            ODocument doc = new ODocument(type).fromJSON(document);
            db.save(doc);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to import document", e);
        }
    }

    @Override
    @RolesAllowed("admin")
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
        Map<String, String> infos = new HashMap<String, String> ();
        infos.put(INFO_POOL_SIZE_MAX, Integer.toString(pool.getMaxSize()));
        infos.put(INFO_AVAIL_CONNECTIONS, Integer.toString(pool.getAvailableConnections()));
        infos.put(INFO_INSTANCES_CREATED, Integer.toString(pool.getCreatedInstances()));
        try {
            infos.put(INFO_PATH, base.toString());
            infos.put(INFO_SIZE, Long.toString(getStoreSize()));
        } catch ( Exception e ) {
            //
        }
        try (ODatabaseDocumentTx db = pool.acquire()) {
            infos.put(INFO_DB_NAME, db.getName());
            infos.put(INFO_DB_SIZE, Long.toString(db.getSize()));
            infos.put(INFO_DB_STATUS, db.getStatus().toString());
        } catch (Exception e) {
            //
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
        try (ODatabaseDocumentTx db = pool.acquire()) {
            OIndex<?> ortolangKeyIdx = db.getMetadata().getIndexManager().getIndex("ortolangKey");
            OIdentifiable ident = (OIdentifiable) ortolangKeyIdx.get(key);
            if (ident != null) {
                return (ODocument) ident.getRecord();
            }
        }
        return null;
    }

    protected String getJSONByKey(String key) {
        try (ODatabaseDocumentTx db = pool.acquire()) {
            OIndex<?> ortolangKeyIdx = db.getMetadata().getIndexManager().getIndex("ortolangKey");
            OIdentifiable ident = (OIdentifiable) ortolangKeyIdx.get(key);
            if (ident != null) {
                ODocument document = ident.getRecord();
                return document.toJSON("fetchPlan:*:-1");
            }
        }
        return null;
    }

    private long getStoreSize() throws IOException {
        return Files.walk(base).mapToLong(this::size).sum();
    }

    private long size(Path p) {
        try {
            return Files.size(p);
        } catch ( Exception e ) {
            return 0;
        }
    }
}
