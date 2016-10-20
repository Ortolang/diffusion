package fr.ortolang.diffusion.rendering;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.rendering.engine.TEIRenderEngine;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.thumbnail.ThumbnailService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@Startup
@Local(RenderingService.class)
@Singleton(name = RenderingService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class RenderingServiceBean implements RenderingService {

    private static final Logger LOGGER = Logger.getLogger(RenderingServiceBean.class.getName());

    public static final String DEFAULT_THUMBNAILS_HOME = "views";
    public static final int DISTINGUISH_SIZE = 2;

    @EJB
    private BinaryStoreService store;
    @EJB
    private BrowserService browser;

    private Path base;

    private Map<String, RenderEngine> engines = new HashMap<String, RenderEngine>();

    private Configuration config;

    public RenderingServiceBean() {
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
        RenderEngine xml = new TEIRenderEngine();
        engines.put(xml.getId(), xml);
        LOGGER.log(Level.INFO, engines.size() + " engines registered.");
        LOGGER.log(Level.INFO, "Building Template Engine Configuration");
        config = new Configuration(Configuration.getVersion());
        config.setDefaultEncoding("UTF-8");
        config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        config.setTemplateLoader(new BinaryStoreTemplateLoader(store));
        LOGGER.log(Level.INFO, "Template Engine configuration built.");
    }

    @Override
    public Collection<RenderEngine> listEngines() {
        LOGGER.log(Level.FINE, "listing all engines");
        return engines.values();
    }

    @Override
    public Collection<RenderEngine> listEnginesForType(String mimetype) {
        LOGGER.log(Level.FINE, "listing engines availables for mimetype: " + mimetype);
        List<RenderEngine> viewers = new ArrayList<RenderEngine>();
        for (RenderEngine viewer : engines.values()) {
            if (viewer.canRender(mimetype)) {
                viewers.add(viewer);
            }
        }
        return viewers;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public File getView(String key, String engineid) throws RenderingServiceException, AccessDeniedException, KeyNotFoundException, CoreServiceException, BinaryStoreServiceException {
        LOGGER.log(Level.FINE, "get view of key [" + key + "]");
        try {
            if (engineid == null || engineid.length() == 0) {
                engineid = "";
            } else if (engineid.length() > 0 && !engines.containsKey(engineid)) {
                throw new RenderingServiceException("render engine with id : " + engineid + " does not exists");
            }
            OrtolangObjectState state = browser.getState(key);
            if (isCached(key, engineid, state.getLastModification())) {
                LOGGER.log(Level.FINEST, "get view from cache");
                return getPath(key, engineid).toFile();
            }
            LOGGER.log(Level.FINEST, "no view in cache, generating...");
            OrtolangObject object = browser.findObject(key);
            if (object.getObjectIdentifier().getService().equals(CoreService.SERVICE_NAME)) {
                if (object.getObjectIdentifier().getType().equals(fr.ortolang.diffusion.core.entity.Collection.OBJECT_TYPE)) {
                    LOGGER.log(Level.FINEST, "key is a collection, loading template metadata");
                    fr.ortolang.diffusion.core.entity.Collection collection = (fr.ortolang.diffusion.core.entity.Collection) object;
                    Set<MetadataElement> metadatas = collection.getMetadatas();
                    String template = null;
                    for (MetadataElement metadata : metadatas) {
                        // TODO for externalization, try to get a template that include Locale...
                        if (metadata.getName().equals(MetadataFormat.TEMPLATE)) {
                            LOGGER.log(Level.FINEST, "template metadata found");
                            template = metadata.getKey();
                            break;
                        }
                    }
                    if (template != null) {
                        Map<String, Object> data = new HashMap<String, Object>();
                        // TODO populate data using collection and collection metadata.
                        process(key, template, data);
                    }
                } else if (object.getObjectIdentifier().getType().equals(DataObject.OBJECT_TYPE)) {
                    LOGGER.log(Level.FINEST, "key is a DataObject, searching render engine");
                    RenderEngine engine = null;
                    if (engineid.length() > 0) {
                        engine = engines.get(engineid);
                        if (!engine.canRender(((DataObject) object).getMimeType())) {
                            throw new RenderingServiceException("the render engine with id: " + engineid + " is not able to render key: " + key);
                        }
                    } else {
                        LOGGER.log(Level.FINEST, "no engine specified, searching for a suitable one");
                        for (RenderEngine rengine : engines.values()) {
                            if (rengine.canRender(((DataObject) object).getMimeType())) {
                                LOGGER.log(Level.FINE, "found potential compatible engine: " + rengine.getId());
                                engine = rengine;
                                break;
                            }
                        }
                    }
                    if (engine == null) {
                        throw new RenderingServiceException("unable to find suitable render engine for key: " + key);
                    }
                    LOGGER.log(Level.FINEST, "starting rendering...");
                    boolean rendered = render(key, ((DataObject) object).getStream(), engine);
                    if (rendered) {
                        LOGGER.log(Level.FINEST, "rendering OK");
                        return getPath(key, engine.getId()).toFile();
                    }
                    LOGGER.log(Level.FINEST, "rendering FAILED");
                }
            }
            throw new RenderingServiceException("unable to generate a view for this key: " + key);
        } catch (DataNotFoundException | IOException | OrtolangException | BrowserServiceException e) {
            LOGGER.log(Level.WARNING, "unexpected error while getting view", e);
            throw new RenderingServiceException("error while getting view", e);
        }
    }

    private boolean render(String key, String hash, RenderEngine engine) throws RenderingServiceException {
        try {
            Path input = store.getFile(hash).toPath();
            return render(key, input, engine);
        } catch (DataNotFoundException | BinaryStoreServiceException e) {
            throw new RenderingServiceException("unexpected error while rendering key: " + key, e);
        }
    }

    private boolean render(String key, Path input, RenderEngine engine) throws RenderingServiceException {
        LOGGER.log(Level.FINE, "Rendering content for key: " + key + " using viewer: " + engine.getId());
        synchronized (engine) {
            try {
                Path output = getPath(key, engine.getId());
                if (Files.exists(output)) {
                    Files.delete(output);
                }
                Files.createDirectories(output.getParent());
                Files.createFile(output);
                LOGGER.log(Level.FINE, "starting rendering for key: " + key + " in file: " + output);
                boolean rendered = engine.render(input, output);
                if (!rendered) {
                    LOGGER.log(Level.WARNING, "rendering failed for key: " + key + " using engine: " + engine.getId());
                    Files.delete(output);
                }
                LOGGER.log(Level.FINE, "rendering done for key: " + key + " in file: " + output);
                return rendered;
            } catch (IOException e) {
                throw new RenderingServiceException("unexpected error while rendering key: " + key, e);
            }
        }
    }

    private boolean process(String key, String template, Object model) throws RenderingServiceException {
        LOGGER.log(Level.FINE, "Processing template: " + template + " for key: " + key);
        try {
            Path output = getPath(key, "tpl");
            if (Files.exists(output)) {
                Files.delete(output);
            }
            Files.createDirectories(output.getParent());
            Files.createFile(output);
            Template temp = config.getTemplate(template);
            try {
                LOGGER.log(Level.FINEST, "processing template...");
                temp.process(model, Files.newBufferedWriter(output));
            } catch (TemplateException e) {
                LOGGER.log(Level.WARNING, "template processing failed for key: " + key + " using template: " + template, e);
                return false;
            }
            LOGGER.log(Level.FINEST, "processing done for key: " + key + " in file: " + output);
            return true;
        } catch (IOException e) {
            throw new RenderingServiceException("unexpected error while processing template for key: " + key, e);
        }
    }

    private Path getPath(String key, String engine) {
        String digit = key.substring(0, DISTINGUISH_SIZE);
        return Paths.get(base.toString(), digit, key + "_" + engine);
    }

    private boolean isCached(String key, String engine, long lmd) throws DataNotFoundException, IOException {
        Path view = getPath(key, engine);
        return Files.exists(view) && lmd < Files.getLastModifiedTime(view).toMillis();
    }

    private long getStoreNbFiles() throws IOException {
        return Files.walk(base).count();
    }

    private long getStoreSize() throws IOException {
        return Files.walk(base).mapToLong(this::size).sum();
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

}
