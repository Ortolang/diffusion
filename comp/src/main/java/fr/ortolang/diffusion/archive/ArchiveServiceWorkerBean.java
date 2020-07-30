package fr.ortolang.diffusion.archive;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.tika.metadata.Metadata;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.jobs.JobService;
import fr.ortolang.diffusion.jobs.entity.Job;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStream;
import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStreamFactory;
import fr.ortolang.diffusion.store.binary.hash.MD5FilterInputStreamFactory;
import fr.ortolang.diffusion.OrtolangJob;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.archive.exception.CheckArchivableException;
import fr.ortolang.diffusion.archive.facile.FacileService;
import fr.ortolang.diffusion.archive.facile.entity.Validator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.MetadataFormatException;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.extraction.parser.OrtolangXMLParser;
import fr.ortolang.diffusion.indexing.IndexingServiceException;

@Startup
@Singleton(name = ArchiveServiceWorker.WORKER_NAME)
@RunAs("system")
@SecurityDomain("ortolang")
@PermitAll
public class ArchiveServiceWorkerBean implements ArchiveServiceWorker {

    private static final Logger LOGGER = Logger.getLogger(ArchiveServiceWorkerBean.class.getName());

    private static final int DELAY = 3000;

    private static final String JOB_TYPE = "archive";

    private static final String CHECK_ACTION = "check";


    private HashedFilterInputStreamFactory factory;
    
    @EJB
    private JobService jobService;

    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;

    @EJB
    private CoreService core;

    @EJB
    private BinaryStoreService binarystore;

    @EJB
    private RegistryService registry;

    @EJB
    private FacileService facile;

    @Resource
    private ManagedThreadFactory managedThreadFactory;

    private ArchiveWorkerThread worker;

    private Thread workerThread;

    private DelayQueue<Job> queue;

    public ArchiveServiceWorkerBean() {
        // no need to initialize
    }

    @Override
    @PostConstruct
    public void start() {
        if (workerThread != null && workerThread.isAlive()) {
            LOGGER.log(Level.WARNING, "Checking {0} worker already started", JOB_TYPE);
            return;
        }
        LOGGER.log(Level.INFO, "Checking {Ø} worker thread", JOB_TYPE);
        this.factory = new MD5FilterInputStreamFactory();
        worker = new ArchiveWorkerThread();
        queue = new DelayQueue<>();
        workerThread = managedThreadFactory.newThread(worker);
        workerThread.setName("Archive Worker Thread");
        workerThread.start();
        Thread.UncaughtExceptionHandler h = (th, ex) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception", ex);
            start();
        };
        workerThread.setUncaughtExceptionHandler(h);
        retryAll(false);
    }

    @Override
    @PreDestroy
    public void stop() {
        LOGGER.log(Level.INFO, "Stopping archive worker thread");
        worker.stop();
    }

    @Override
    public String getName() {
        return WORKER_NAME;
    }

    @Override
    public String getType() {
        return JOB_TYPE;
    }

    @Override
    public String getState() {
        return workerThread.getState().name();
    }

    @Override
    public List<OrtolangJob> getQueue() {
        return queue.stream().collect(Collectors.toList());
    }

    @Override
    public void retry(Long id) {
        Job job = jobService.read(id);
        if (job != null && !queue.contains(job)) {
            queue.add(job);
        }
    }

    @Override
    public void retryAll(boolean failed) {
        if (!queue.isEmpty()) {
            queue.clear();
        }
        List<Job> jobs = jobService.getUnprocessedJobsOfType(JOB_TYPE);
        LOGGER.log(Level.FINE, "Restoring {0} archive jobs in queue", jobs.size());
        if (failed) {
            List<Job> failedJobs = jobService.getFailedJobsOfType(JOB_TYPE);
            LOGGER.log(Level.FINE, "Retrying {0} failed archive jobs", failedJobs.size());
            jobs.addAll(failedJobs);
        }
        queue.addAll(jobs);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void submit(String key, String action) {
        submit(key, action, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void submit(String key, String action, Map<String, String> args) {
        Job job = jobService.create(JOB_TYPE, CHECK_ACTION, key, System.currentTimeMillis() + DELAY);
        queue.put(job);
    }

    class ArchiveWorkerThread implements Runnable {

        private boolean run = true;

        void stop() {
            this.run = false;
        }

        @Override
        public void run() {
            while (run) {
                try {
                    Job job = queue.take();
                    String key = job.getTarget();
                    LOGGER.log(Level.FINE, "treating action {0} for target {1}", new Object[] {job.getAction(), key});
                    try {

                        switch (job.getAction()) {
                            case CHECK_ACTION: {
                                OrtolangObjectIdentifier identifier = registry.lookup(key);
                                if (!identifier.getService().equals(core.getServiceName())
                                        || !identifier.getType().equals(DataObject.OBJECT_TYPE)) {
                                    throw new CoreServiceException("target can only be DataObject.");
                                }
                                OrtolangObject object = em.find(DataObject.class, identifier.getId());
                                DataObject dataObject = (DataObject) object;
                                dataObject.setKey(key);
                                validate(dataObject);
                                break;
                            }
                            default:
                                LOGGER.log(Level.WARNING, "unknown job action {0}",job.getAction());
                        }
                        jobService.remove(job.getId());
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,
                                "unable to check archivable for data object with key {0}", new Object[] {key, e});
                        LOGGER.log(Level.FINE, e.getMessage(), e);
                        jobService.updateFailingJob(job, e);
                    }

                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "interrupted while trying to take next job", e);
                }
            }
        }

        private void validate(DataObject dataObject) throws BinaryStoreServiceException, DataNotFoundException, CheckArchivableException, JsonProcessingException, DataCollisionException, KeyNotFoundException, CoreServiceException, MetadataFormatException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException, RegistryServiceException, AuthorisationServiceException, IndexingServiceException {
            // Validation via FACILE
            String hash = dataObject.getStream();
            LOGGER.log(Level.FINE, "Checking archivable for data object {0}", dataObject.getKey());
            File content = binarystore.getFile(hash);
            InputStream input = binarystore.get(hash);
            Validator validator = null;
            XMLMetadata xmlMetadata = getXMLMetadata(dataObject);
            if (xmlMetadata.getFormat().contentEquals(OrtolangXMLParser.XMLType.TEI.name())) {
            	validator = new Validator();
            	validator.setFormat(xmlMetadata.getFormat());
            	validator.setValid(true);
            	validator.setWellFormed(true);
            	validator.setArchivable(true); // TODO check with the schema
            	validator.setEncoding(xmlMetadata.getEncoding());
            	validator.setVersion(xmlMetadata.getVersion());
            	validator.setFileName(dataObject.getName());
            	validator.setSize(dataObject.getSize());
				try {
					HashedFilterInputStream hashInputStream = factory.getHashedFilterInputStream(input);
					byte[] buffer = new byte[10240];
		            while (hashInputStream.read(buffer) >= 0) {
		            }
					validator.setMd5sum(hashInputStream.getHash());
				} catch (NoSuchAlgorithmException | IOException e) {
					validator.setMessage(e.getMessage());
					LOGGER.log(Level.WARNING, "Cant generate md5 for the data object", e);
				}
            } else {
            	validator = facile.checkArchivableFile(content, dataObject.getName());
            }
            
            if (validator != null) {
                String json = writeJson(validator);
                String metadataHash = binarystore
                        .put(new ByteArrayInputStream(json.getBytes()));
                core.systemCreateMetadata(dataObject.getKey(), MetadataFormat.FACILE_VALIDATOR, metadataHash,
                        MetadataFormat.FACILE_VALIDATOR + ".json");
            } else {
                LOGGER.log(Level.WARNING, "Validator XML cant be parsed for data object {0}", dataObject.getKey());
            }
		}
        
		private String writeJson(Validator validator) throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(validator);
        }
		
		private XMLMetadata getXMLMetadata(DataObject dataObject) {
			try {
				MetadataElement xmlMd = dataObject.findMetadataByName(MetadataFormat.XML);
				if (xmlMd != null) {
	            	OrtolangObjectIdentifier itemMetadataIdentifier;
						itemMetadataIdentifier = registry.lookup(xmlMd.getKey());
	                MetadataObject metadataObject = em.find(MetadataObject.class, itemMetadataIdentifier.getId());
	                return extractXMLMetadata(metadataObject);
	            }
			} catch (RegistryServiceException | KeyNotFoundException e) {
				LOGGER.log(Level.WARNING, "Enable to check the format of the data object {0}", new Object[] {dataObject.getKey(), e});
			}
			return null;
		}
		
		private XMLMetadata extractXMLMetadata(MetadataObject metadataObject) {
			XMLMetadata md = new XMLMetadata();
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> xmlContent;
			try {
				xmlContent = mapper.readValue(binarystore.getFile(metadataObject.getStream()), new TypeReference<Map<String, Object>>(){});
				if (xmlContent.containsKey(OrtolangXMLParser.XML_TYPE_KEY)) {
					md.setFormat((String) xmlContent.get(OrtolangXMLParser.XML_TYPE_KEY));
				}
				if (xmlContent.containsKey(Metadata.CONTENT_ENCODING)) {
					md.setEncoding((String) xmlContent.get(Metadata.CONTENT_ENCODING));
				}
				if (xmlContent.containsKey("XML-Version")) {
					md.setVersion((String) xmlContent.get("XML-Version"));
				}
			} catch (IOException | BinaryStoreServiceException | DataNotFoundException e) {
				LOGGER.log(Level.WARNING, "Enable to extract format from metadata object", e);
			}
			return md;
		}
		
		class XMLMetadata {
			private String format;
			public String getFormat() {
				return format;
			}
			public void setFormat(String format) {
				this.format = format;
			}
			public String getEncoding() {
				return encoding;
			}
			public void setEncoding(String encoding) {
				this.encoding = encoding;
			}
			public String getVersion() {
				return version;
			}
			public void setVersion(String version) {
				this.version = version;
			}
			private String encoding;
			private String version;
			
		}
    }
}