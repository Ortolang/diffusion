package fr.ortolang.diffusion.preview;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangJob;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.preview.entity.Preview;
import fr.ortolang.diffusion.preview.generator.PreviewGenerator;
import fr.ortolang.diffusion.preview.generator.PreviewGeneratorException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Local(PreviewService.class)
@Startup
@Singleton(name = PreviewService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class PreviewServiceBean implements PreviewService {

	private static final Logger LOGGER = Logger.getLogger(PreviewServiceBean.class.getName());
	private static final String GENERATORS_CONFIG_PARAMS = "preview.generators";
	private static final long DEFAULT_GENERATION_DELAY = 10000;

	@EJB
	private BinaryStoreService binaryStore;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private BinaryStoreService store;
	@EJB
	private NotificationService notification;
	@Resource
	private ManagedThreadFactory managedThreadFactory;
//	@Resource
//	private UserTransaction userTx;
	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;
	@Resource
	private SessionContext ctx;
	private PreviewServiceWorker worker;
	private DelayQueue<OrtolangJob> queue;
	private List<PreviewGenerator> generators = new ArrayList<PreviewGenerator>();
	private long lastModification = System.currentTimeMillis();

	public PreviewServiceBean() {
		this.worker = new PreviewServiceWorker();
		this.queue = new DelayQueue<OrtolangJob>();
	}

	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Initializing service, registering generators");
		String[] generatorsClass = OrtolangConfig.getInstance().getProperty(GENERATORS_CONFIG_PARAMS).split(",");
		for (String clazz : generatorsClass) {
			try {
				LOGGER.log(Level.INFO, "Instanciating generator for class: " + clazz);
				PreviewGenerator generator = (PreviewGenerator) Class.forName(clazz).newInstance();
				generators.add(generator);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				LOGGER.log(Level.WARNING, "Unable to instanciate generator for class: " + clazz, e);
			}
		}
		//Runnable ctxRunnable = contextService.createContextualProxy(worker, Runnable.class);
		Thread thread = managedThreadFactory.newThread(worker);
		thread.setName("Preview Worker Thread");
		thread.start();
	}

	@PreDestroy
	public void stop() {
		LOGGER.log(Level.INFO, "Stopping preview worker");
		worker.stop();
	}

	@Override
	@RolesAllowed(PreviewListenerBean.PREVIEW_LISTENER_ROLE)
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void submit(OrtolangJob job) throws PreviewServiceException {
		LOGGER.log(Level.FINE, "submit new preview job for target: " + job.getTarget());
		OrtolangJob existingJob = getJob(job.getTarget());
		if (existingJob != null) {
			queue.remove(existingJob);
		}
		job.setTimestamp(System.currentTimeMillis() + DEFAULT_GENERATION_DELAY);
		queue.put(job);
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long getLastModification() throws PreviewServiceException {
		LOGGER.log(Level.FINE, "get last service modification date");
		return lastModification;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public boolean exists(String key) throws PreviewServiceException {
		LOGGER.log(Level.FINE, "check if preview exists for key [" + key + "]");
		Preview preview = em.find(Preview.class, key);
		if (preview != null) {
			return true;
		}
		return false;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<Preview> list(List<String> keys) throws PreviewServiceException {
		LOGGER.log(Level.FINE, "listing existing previews for key set");
		List<Preview> existing = em.createNamedQuery("findExistingPreviews", Preview.class).setParameter("keysList", keys).getResultList();
		return existing;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Preview getPreview(String key) throws PreviewServiceException {
		LOGGER.log(Level.FINE, "get preview for key [" + key + "]");
		Preview preview = em.find(Preview.class, key);
		return preview;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public long getPreviewSize(String key, String size) throws PreviewServiceException {
		LOGGER.log(Level.FINE, "get preview size for key [" + key + "]");
		try {
			Preview preview = em.find(Preview.class, key);
			if (preview == null) {
				return -1;
			}
			String hash = preview.getSmall();
			if ( size.equals(Preview.LARGE) ) {
				hash = preview.getLarge();
			} 
			return store.size(hash);
		} catch (BinaryStoreServiceException | DataNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred during getting preview size", e);
			throw new PreviewServiceException("unable to get preview size", e);
		} 
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public InputStream getPreviewContent(String key, String size) throws PreviewServiceException, AccessDeniedException {
		LOGGER.log(Level.FINE, "get preview content for key [" + key + "] and size [" + size + "]");
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();

			authorisation.checkPermission(key, subjects, "read");
			Preview preview = em.find(Preview.class, key);
			if (preview == null) {
				throw new PreviewServiceException("unable to load preview for key [" + key + "] from storage");
			}
			String hash = preview.getSmall();
			if ( size.equals(Preview.LARGE) ) {
				hash = preview.getLarge();
			} 
			InputStream stream = store.get(hash);
			return stream;

		} catch (BinaryStoreServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | DataNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occurred during getting preview", e);
			throw new PreviewServiceException("unable to get preview", e);
		} 
	}

	private OrtolangJob getJob(String key) {
		for (OrtolangJob job : queue) {
			if (job.getTarget().equals(key)) {
				return job;
			}
		}
		return null;
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private void generate(String key, String mimetype, String hash, String wskey, String path) throws PreviewServiceException {
		LOGGER.log(Level.FINE, "Generating preview for key: " + key);
		try {
			Path smallOutput = Files.createTempFile("thumbs-", ".jpg");
			Path largeOutput = Files.createTempFile("thumbl-", ".jpg");
			File input = binaryStore.getFile(hash);
			boolean generated = false;
			for (PreviewGenerator generator : generators) {
				if (generator.getAcceptedMIMETypes().contains(mimetype)) {
					try {
						generator.generate(input, smallOutput.toFile(), Preview.SMALL_PREVIEW_WIDTH, Preview.SMALL_PREVIEW_HEIGHT);
						generator.generate(input, largeOutput.toFile(), Preview.LARGE_PREVIEW_WIDTH, Preview.LARGE_PREVIEW_HEIGHT);
						generated = true;
						LOGGER.log(Level.FINE, "preview generated for key: " + key + " in file: " + smallOutput);
						break;
					} catch (PreviewGeneratorException e) {
						LOGGER.log(Level.FINE, "generator failed to produce preview for key: " + key, e);
					}
				}
			}
			if (generated) {
				LOGGER.log(Level.FINE, "storing preview");
				try (InputStream iss = Files.newInputStream(smallOutput); InputStream isl = Files.newInputStream(largeOutput)) {
					String small = binaryStore.put(iss);
					String large = binaryStore.put(isl);
					Preview preview = em.find(Preview.class, key);
					if (preview == null) {
						preview = new Preview(key, small, large, System.currentTimeMillis());
						em.persist(preview);
					} else {
						preview.setSmall(small);
						preview.setLarge(large);
						preview.setGenerationDate(System.currentTimeMillis());
						em.merge(preview);
					}
					lastModification = System.currentTimeMillis();
				} catch (DataCollisionException e) {
					LOGGER.log(Level.WARNING, "unable to store preview for key: " + key, e);
				}
			}
			Files.deleteIfExists(smallOutput);
			Files.deleteIfExists(largeOutput);
		} catch (DataNotFoundException | BinaryStoreServiceException | IOException e) {
			throw new PreviewServiceException("error while generating previews for key: " + key, e);
		}
	}

	class PreviewServiceWorker implements Runnable {

		private boolean run = true;
		
		public PreviewServiceWorker() {
		}

		public void stop() {
			this.run = false;
		}

		@Override
		public void run() {
			while (run) {
				try {
					OrtolangJob job = queue.take();
					LOGGER.log(Level.FINE, "trying to generate preview for key: " + job.getTarget());
					try {
						InitialContext ctx = new InitialContext();
						UserTransaction ut = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
						ut.begin();
						generate(job.getTarget(), (String) job.getParameter("mimetype"), (String) job.getParameter("hash"), (String) job.getParameter("wskey"), (String) job.getParameter("path"));
						ut.commit();
					} catch (Exception e) {
						LOGGER.log(Level.FINE, "unable to generate preview for key: " + job.getTarget(), e);
					}
				} catch (InterruptedException e) {
					LOGGER.log(Level.SEVERE, "error occured while trying to take next preview generation job", e);
				}

			}
		}
	}

}
