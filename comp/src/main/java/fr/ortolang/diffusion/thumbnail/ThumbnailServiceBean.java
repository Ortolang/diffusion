package fr.ortolang.diffusion.thumbnail;

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
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.*;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.thumbnail.generator.ThumbnailGenerator;
import fr.ortolang.diffusion.thumbnail.generator.ThumbnailGeneratorException;

@Local(ThumbnailService.class)
@Startup
@Singleton(name = ThumbnailService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ThumbnailServiceBean implements ThumbnailService {

	private static final Logger LOGGER = Logger.getLogger(ThumbnailServiceBean.class.getName());

	private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
    
    public static final String DEFAULT_THUMBNAILS_HOME = "thumbs";
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
	private List<ThumbnailGenerator> generators = new ArrayList<ThumbnailGenerator>();

	public ThumbnailServiceBean() {
	}

	@PostConstruct
	public void init() {
		this.base = Paths.get(OrtolangConfig.getInstance().getHomePath().toString(), DEFAULT_THUMBNAILS_HOME);
		LOGGER.log(Level.INFO, "Initializing service with base folder: " + base);
		try {
			Files.createDirectories(base);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to initialize thumbs store", e);
		}
		LOGGER.log(Level.INFO, "Registering generators");
		String[] generatorsClass = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.THUMBNAIL_GENERATORS).split(",");
		for (String clazz : generatorsClass) {
			try {
				LOGGER.log(Level.INFO, "Instantiating generator for class: " + clazz);
				ThumbnailGenerator generator = (ThumbnailGenerator) Class.forName(clazz).newInstance();
				generators.add(generator);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				LOGGER.log(Level.WARNING, "Unable to instantiate generator for class: " + clazz, e);
			}
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Thumbnail getThumbnail(String key, int size) throws ThumbnailServiceException, AccessDeniedException, KeyNotFoundException, CoreServiceException, BinaryStoreServiceException {
		LOGGER.log(Level.FINE, "get thumbnail for key [" + key + "] and size [" + size + "]");
		try {
			OrtolangObjectState state = browser.getState(key);
			if (needGeneration(key, size, state.getLastModification())) {
				OrtolangObject object = browser.findObject(key);
				boolean generated = false;
				if (object.getObjectIdentifier().getService().equals(CoreService.SERVICE_NAME)) {
					if (object instanceof MetadataSource) {
						MetadataElement metadataElement = ((MetadataSource) object).findMetadataByName(MetadataFormat.THUMB);
						if (metadataElement != null) {
							MetadataObject metadataObject = core.readMetadataObject(metadataElement.getKey());
							File file = store.getFile(metadataObject.getStream());
							generated = generate(key, metadataObject.getContentType(), metadataObject.getStream(), size);
							if (!generated) {
								return new Thumbnail(file, metadataObject.getContentType());
							}
						}
					}
				}
				if (!generated && object.getObjectIdentifier().getService().equals(CoreService.SERVICE_NAME) && object.getObjectIdentifier().getType().equals(DataObject.OBJECT_TYPE)) {
					generated = generate(key, ((DataObject) object).getMimeType(), ((DataObject) object).getStream(), size);
				}
				if ( !generated ) {
					throw new ThumbnailServiceException("unable to generate thumbnail for object that are not dataobjects");
				}
			}
			return new Thumbnail(getFile(key, size), ThumbnailService.THUMBS_MIMETYPE);
		} catch (DataNotFoundException | IOException | OrtolangException | BrowserServiceException e) {
			LOGGER.log(Level.WARNING, "unexpected error while retrieving thumbnail", e);
			throw new ThumbnailServiceException("error while retrieving thumbnail", e);
		}
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private boolean generate(String key, String mimetype, String hash, int size) throws ThumbnailServiceException {
		LOGGER.log(Level.FINE, "Generating thumbnail for key: " + key + " and size: " + size);
		try {
			File output = getFile(key, size);
			if ( output.exists() ) {
				output.delete();
			}
			output.getParentFile().mkdirs();
			output.createNewFile();
			File input = store.getFile(hash);
			boolean generated = false;
			for (ThumbnailGenerator generator : generators) {
				if (generator.getAcceptedMIMETypes().contains(mimetype)) {
					try {
						generator.generate(input, output, size, size);
						generated = true;
						LOGGER.log(Level.FINE, "thumbnail generated for key: " + key + " in file: " + output);
						break;
					} catch (ThumbnailGeneratorException e) {
						LOGGER.log(Level.FINE, "generator failed to produce thumbnail for key: " + key, e);
					}
				}
			}
			if ( !generated ) {
				output.delete();
			}
			return generated;
		} catch (DataNotFoundException | BinaryStoreServiceException | IOException e) {
			throw new ThumbnailServiceException("error while generating thumbnail for key: " + key, e);
		}
	}

	private File getFile(String key, int size) throws DataNotFoundException {
		String digit = key.substring(0, DISTINGUISH_SIZE);
		return Paths.get(base.toString(), digit, key + "_" + size + ".jpg").toFile();
	}

	private boolean needGeneration(String key, int size, long lmd) throws DataNotFoundException, IOException {
		File thumb = getFile(key, size);
		if (thumb.exists()) {
			return lmd > thumb.lastModified();
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
        } catch ( Exception e ) {
            return 0;
        }
    }
	
	//Service methods
    
    @Override
    public String getServiceName() {
        return ThumbnailService.SERVICE_NAME;
    }
    
    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String> ();
        infos.put(INFO_PATH, this.base.toString());
        try {
            infos.put(INFO_FILES, Long.toString(getStoreNbFiles()));
        } catch ( Exception e ) { 
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_FILES, e);
        }
        try {
            infos.put(INFO_SIZE, Long.toString(getStoreSize()));
        } catch ( Exception e ) { 
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
