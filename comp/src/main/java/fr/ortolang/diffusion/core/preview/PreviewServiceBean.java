package fr.ortolang.diffusion.core.preview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.preview.generator.PreviewGenerator;
import fr.ortolang.diffusion.core.preview.generator.PreviewGeneratorException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Local(PreviewService.class)
@Startup
@Singleton(name = PreviewService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
@RunAs(PreviewService.SERVICE_NAME)
@RunAsPrincipal(MembershipService.SUPERUSER_IDENTIFIER)
public class PreviewServiceBean implements PreviewService {
	
	private static final Logger LOGGER = Logger.getLogger(PreviewServiceBean.class.getName());
	
	@EJB
	private CoreService core;
	@EJB
	private BinaryStoreService binaryStore;
	private List<PreviewGenerator> generators = new ArrayList<PreviewGenerator>();
	
	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Initializing service, registering generators");
		String[] generatorsClass = OrtolangConfig.getInstance().getProperty(GENERATORS_CONFIG_PARAMS).split(",");
		for ( String clazz : generatorsClass ) {
			try {
				LOGGER.log(Level.INFO, "Instanciating generator for class: " + clazz);
				PreviewGenerator generator = (PreviewGenerator) Class.forName(clazz).newInstance();
				generators.add(generator);
			} catch ( InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				LOGGER.log(Level.WARNING, "Unable to instanciate generator for class: " + clazz, e);
			}
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void generate(String key) throws PreviewServiceException {
		LOGGER.log(Level.FINE, "Generating previews for key: " + key);
		try {
			DataObject object = core.readDataObject(key);
			Path smallOutput = Files.createTempFile("thumbs-", ".jpg");
			Path largeOutput = Files.createTempFile("thumbl-", ".jpg");
			File input = binaryStore.getFile(object.getStream());
			boolean generated = false;
			for ( PreviewGenerator generator : generators ) {
				if ( generator.getAcceptedMIMETypes().contains(object.getMimeType()) ) {
					try {
						generator.generate(input, smallOutput.toFile(), PreviewService.SMALL_IMAGE_WIDTH, PreviewService.SMALL_IMAGE_HEIGHT);
						generator.generate(input, largeOutput.toFile(), PreviewService.LARGE_IMAGE_WIDTH, PreviewService.LARGE_IMAGE_HEIGHT);
						generated = true;
						LOGGER.log(Level.FINE, "previews generated for key: " + key + " in file: " + smallOutput);
						break;
					} catch ( PreviewGeneratorException e ) {
						LOGGER.log(Level.FINE, "generator failed to produce previews for key: " + key, e);
					}
				}
			}
			if ( generated ) {
				LOGGER.log(Level.FINE, "storing previews in object");
				try (InputStream iss = Files.newInputStream(smallOutput); InputStream isl = Files.newInputStream(largeOutput)) {
					String hashs = binaryStore.put(iss);
					long sizes = binaryStore.size(hashs);
					String hashl = binaryStore.put(isl);
					long sizel = binaryStore.size(hashl);
					core.systemSetObjectPreview(key, hashs, sizes, hashl, sizel);
				} catch (DataCollisionException e) {
					LOGGER.log(Level.WARNING, "unable to store previews for object with key: " + key, e);
				} 
			}
			Files.deleteIfExists(smallOutput);
			Files.deleteIfExists(largeOutput);
		} catch ( CoreServiceException | KeyNotFoundException | AccessDeniedException | DataNotFoundException | BinaryStoreServiceException | IOException e ) {
			throw new PreviewServiceException("error while generating previews for key: " + key, e);
		}
	}

}
