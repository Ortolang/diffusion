package fr.ortolang.diffusion.viewer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.thumbnail.ThumbnailService;
import fr.ortolang.diffusion.viewer.engine.MarkdownViewer;
import fr.ortolang.diffusion.viewer.engine.XmlViewer;

@Startup
@Local(ViewerService.class)
@Singleton(name = ViewerService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ViewerServiceBean implements ViewerService {

    private static final Logger LOGGER = Logger.getLogger(ViewerServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] {};
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] {};

    public static final String DEFAULT_THUMBNAILS_HOME = "views";
    public static final int DISTINGUISH_SIZE = 2;

    @EJB
    private MembershipService membership;
    @EJB
    private AuthorisationService authorisation;
    @EJB
    private BinaryStoreService store;
    @EJB
    private BrowserService browser;
    @EJB
    private CoreService core;
    private Path base;
    private Map<String, ViewerEngine> engines = new HashMap<String, ViewerEngine>();

    public ViewerServiceBean() {
    }

    @PostConstruct
    public void init() {
        this.base = Paths.get(OrtolangConfig.getInstance().getHomePath().toString(), DEFAULT_THUMBNAILS_HOME);
        LOGGER.log(Level.INFO, "Initializing service with base folder: " + base);
        try {
            Files.createDirectories(base);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to initialize views store", e);
        }
        LOGGER.log(Level.INFO, "Registering engines: ");
        ViewerEngine md = new MarkdownViewer();
        engines.put(md.getId(), md);
        ViewerEngine xml = new XmlViewer();
        engines.put(xml.getId(), xml);
        LOGGER.log(Level.INFO, engines.size() + " engines registered.");
    }

    @Override
    public Collection<ViewerEngine> listViewers() {
        LOGGER.log(Level.FINE, "listing all viewers");
        return engines.values();
    }

    @Override
    public Collection<ViewerEngine> listViewersForType(String mimetype) {
        LOGGER.log(Level.FINE, "listing viewers availables for mimetype: " + mimetype);
        List<ViewerEngine> viewers = new ArrayList<ViewerEngine>();
        for (ViewerEngine viewer : engines.values()) {
            if (viewer.canRender(mimetype)) {
                viewers.add(viewer);
            }
        }
        return viewers;
    }

    @Override
    public File getView(String key) throws ViewerServiceException, AccessDeniedException, KeyNotFoundException, CoreServiceException, BinaryStoreServiceException {
        LOGGER.log(Level.FINE, "get view of key [" + key + "]");
        try {
            OrtolangObjectState state = browser.getState(key);
            OrtolangObject object = browser.findObject(key);
            if (object.getObjectIdentifier().getService().equals(CoreService.SERVICE_NAME) && object.getObjectIdentifier().getType().equals(DataObject.OBJECT_TYPE)) {
                boolean rendered = false;
                for (ViewerEngine engine : engines.values()) {
                    if (engine.canRender(((DataObject) object).getMimeType())) {
                        LOGGER.log(Level.FINE, "found potential compatible viewer engine: " + engine.getId());
                        if (isStale(key, engine.getId(), state.getLastModification())) {
                            LOGGER.log(Level.FINE, "cache is stale, generating new view");
                            rendered = render(key, ((DataObject) object).getStream(), engine);
                        } else {
                            LOGGER.log(Level.FINE, "cache is good, returning cached view");
                            rendered = true;
                        }
                    }
                    if (rendered) {
                        return getPath(key, engine.getId()).toFile();
                    }
                }
            } else {
                throw new ViewerServiceException("only DataObject have a view");
            }
            throw new ViewerServiceException("unable to find a suitable engine for generating a view for this key: " + key);
        } catch (DataNotFoundException | IOException | OrtolangException | BrowserServiceException e) {
            LOGGER.log(Level.WARNING, "unexpected error while getting view", e);
            throw new ViewerServiceException("error while getting view", e);
        }
    }

    @Override
    public File getView(String key, String engineid) throws ViewerServiceException, AccessDeniedException, KeyNotFoundException, CoreServiceException, BinaryStoreServiceException {
        LOGGER.log(Level.FINE, "get view of key [" + key + "] using engine: [" + engineid + "]");
        try {
            if (!engines.containsKey(engineid)) {
                throw new ViewerServiceException("unknown engine id: " + engineid);
            }
            OrtolangObjectState state = browser.getState(key);
            OrtolangObject object = browser.findObject(key);
            boolean rendered = false;
            if (object.getObjectIdentifier().getService().equals(CoreService.SERVICE_NAME) && object.getObjectIdentifier().getType().equals(DataObject.OBJECT_TYPE)) {
                ViewerEngine engine = engines.get(engineid);
                if (engine.canRender(((DataObject) object).getMimeType())) {
                    if (isStale(key, engine.getId(), state.getLastModification())) {
                        LOGGER.log(Level.FINE, "cache is stale, generating new view");
                        rendered = render(key, ((DataObject) object).getStream(), engine);
                    } else {
                        LOGGER.log(Level.FINE, "cache is good, returning cached view");
                        rendered = true;
                    }
                } else {
                    throw new ViewerServiceException("the viewer engine with id: " + engineid + " is not able to render key: " + key);
                }
            }
            if (rendered) {
                return getPath(key, engineid).toFile();
            }
            throw new ViewerServiceException("unable to find a view for key: " + key + " using viewer engine with id: " + engineid);
        } catch (DataNotFoundException | IOException | OrtolangException | BrowserServiceException e) {
            LOGGER.log(Level.WARNING, "unexpected error while getting view", e);
            throw new ViewerServiceException("error while getting view", e);
        }
    }

    private synchronized boolean render(String key, String hash, ViewerEngine engine) throws ViewerServiceException {
        LOGGER.log(Level.FINE, "Rendering content for key: " + key + " using viewer: " + engine.getId());
        // TODO increase lock granularity in order to allow parallel rendering on different keys.
        try {
            Path output = getPath(key, engine.getId());
            if (Files.exists(output)) {
                Files.delete(output);
            }
            Files.createDirectories(output.getParent());
            Files.createFile(output);
            Path input = store.getFile(hash).toPath();
            LOGGER.log(Level.FINE, "starting rendering done for key: " + key + " in file: " + output);
            boolean rendered = engine.render(input, output);
            if (!rendered) {
                LOGGER.log(Level.WARNING, "rendering failed for key: " + key + " using engine: " + engine.getId());
                Files.delete(output);
            }
            LOGGER.log(Level.FINE, "rendering done for key: " + key + " in file: " + output);
            return rendered;
        } catch (DataNotFoundException | BinaryStoreServiceException | IOException e) {
            throw new ViewerServiceException("unexpected error while rendering key: " + key, e);
        }
    }

    private Path getPath(String key, String viewer) throws DataNotFoundException {
        String digit = key.substring(0, DISTINGUISH_SIZE);
        return Paths.get(base.toString(), digit, key + "_" + viewer);
    }

    private boolean isStale(String key, String viewer, long lmd) throws DataNotFoundException, IOException {
        Path view = getPath(key, viewer);
        if (Files.exists(view)) {
            return lmd > Files.getLastModifiedTime(view).toMillis();
        }
        return true;
    }

    private long getStoreNbFiles() throws IOException {
        long nbfiles = Files.walk(base).count();
        return nbfiles;
    }

    private long getStoreSize() throws IOException {
        long size = Files.walk(base).mapToLong(this::size).sum();
        return size;
    }

    private long size(Path p) {
        try {
            return Files.size(p);
        } catch (Exception e) {
            return 0;
        }
    }

    // Service methods

    @Override
    public String getServiceName() {
        return ThumbnailService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String>();
        infos.put(INFO_PATH, this.base.toString());
        try {
            infos.put(INFO_FILES, Long.toString(getStoreNbFiles()));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_FILES, e);
        }
        try {
            infos.put(INFO_SIZE, Long.toString(getStoreSize()));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_SIZE, e);
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
