package fr.ortolang.diffusion.thumbnail;

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

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.DataObject;
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
				LOGGER.log(Level.INFO, "Instanciating generator for class: " + clazz);
				ThumbnailGenerator generator = (ThumbnailGenerator) Class.forName(clazz).newInstance();
				generators.add(generator);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				LOGGER.log(Level.WARNING, "Unable to instanciate generator for class: " + clazz, e);
			}
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public File getThumbnail(String key, int size) throws ThumbnailServiceException, AccessDeniedException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "get thumbnail size for key [" + key + "] and size [" + size + "]");
		try {
			OrtolangObjectState state = browser.getState(key);
			if (needGeneration(key, size, state.getLastModification())) {
				OrtolangObject object = browser.findObject(key);
				boolean generated = false;
				if (object.getObjectIdentifier().getService().equals(CoreService.SERVICE_NAME) && object.getObjectIdentifier().getType().equals(DataObject.OBJECT_TYPE)) {
					generated = generate(key, ((DataObject) object).getMimeType(), ((DataObject) object).getStream(), size);
				} 
				if ( !generated ) {
					throw new ThumbnailServiceException("unable to generate thumbnail for object that are not dataobjects");
				}
			}
			return getFile(key, size);
		} catch (DataNotFoundException | IOException | OrtolangException | BrowserServiceException e) {
			LOGGER.log(Level.WARNING, "unexpected error while retreiving thumbnail", e);
			throw new ThumbnailServiceException("error while retreiving thumbnail", e);
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
	
	//Service methods
    
    @Override
    public String getServiceName() {
        return ThumbnailService.SERVICE_NAME;
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
