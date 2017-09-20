package fr.ortolang.diffusion.oai;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.xml.XMLConstants;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.xml.sax.SAXException;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangJob;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.jobs.JobService;
import fr.ortolang.diffusion.jobs.entity.Job;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.exception.MetadataConverterException;
import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.exception.MetadataPrefixUnknownException;
import fr.ortolang.diffusion.oai.exception.OaiServiceException;
import fr.ortolang.diffusion.oai.exception.RecordNotFoundException;
import fr.ortolang.diffusion.oai.exception.SetAlreadyExistsException;
import fr.ortolang.diffusion.oai.format.builder.XMLMetadataBuilder;
import fr.ortolang.diffusion.oai.format.converter.CmdiOutputConverter;
import fr.ortolang.diffusion.oai.format.converter.DublinCoreOutputConverter;
import fr.ortolang.diffusion.oai.format.handler.CmdiHandler;
import fr.ortolang.diffusion.oai.format.handler.DublinCoreHandler;
import fr.ortolang.diffusion.oai.format.handler.OlacHandler;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;
import fr.ortolang.diffusion.util.StreamUtils;
import fr.ortolang.diffusion.util.XmlUtils;

@Startup
@Singleton(name = OaiWorker.WORKER_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class OaiWorkerBean implements OaiWorker {

    private static final Logger LOGGER = Logger.getLogger(OaiWorkerBean.class.getName());

    private static final String JOB_TYPE = "oai";

    private static final int DELAY = 5000;

    @EJB
    private RegistryService registry;
    @EJB
    private JobService jobService;
    @EJB
    private CoreService core;
    @EJB
    private OaiService oai;
	@EJB
	private HandleStoreService handleStore;
	@EJB
	private BinaryStoreService binaryStore;
    
    @Resource
    @SuppressWarnings("EjbEnvironmentInspection")
    private ManagedThreadFactory managedThreadFactory;

    private OaiWorkerThread worker;

    private Thread workerThread;

    private DelayQueue<Job> queue;

    public OaiWorkerBean() { }
    
	@Override
    @PostConstruct
	public void start() {
		if (workerThread != null && workerThread.isAlive()) {
            LOGGER.log(Level.WARNING, "Oai Service worker already started");
            return;
        }
        LOGGER.log(Level.INFO, "Starting Oai Service worker thread");
        worker = new OaiWorkerThread();
        queue = new DelayQueue<>();
        workerThread = managedThreadFactory.newThread(worker);
        workerThread.setName("Oai Worker Thread");
        workerThread.start();
        retryAll(false);
	}

	@Override
    @PreDestroy
	public void stop() {
		LOGGER.log(Level.INFO, "Stopping extraction worker thread");
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
        LOGGER.log(Level.INFO, "Restoring " + jobs.size() + " extraction jobs in queue");
        if (failed) {
            List<Job> failedJobs = jobService.getFailedJobsOfType(JOB_TYPE);
            LOGGER.log(Level.INFO, "Retrying " + failedJobs.size() + " failed extraction jobs");
            jobs.addAll(failedJobs);
        }
        queue.addAll(jobs);
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void submit(String key, String action) {
		submit(key, action, new HashMap<>());
	}

	@Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void submit(String key, String action, Map<String, String> args) {
		Job job = jobService.create(JOB_TYPE, BUILD_ACTION, key, System.currentTimeMillis() + DELAY, args);
        queue.put(job);
	}

    private class OaiWorkerThread implements Runnable {

        private boolean run = true;

        public void stop() {
            this.run = false;
        }

        @Override
        public void run() {
            while (run) {
                try {
                    Job job = queue.take();
                    LOGGER.log(Level.FINE, "treating action: " + job.getAction() + " for target: " + job.getTarget());
                    try {
                        switch (job.getAction()) {
                        case BUILD_ACTION:
                        	String snapshot = job.getParameter("snapshot");
                        	buildFromWorkspace(job.getTarget(), snapshot);
                            break;
                        default:
                            LOGGER.log(Level.WARNING, "unknown job action: " + job.getAction());
                        }
                        jobService.remove(job.getId());
                    } catch (OaiServiceException e) {
                        LOGGER.log(Level.WARNING, "unable to perform job action " + job.getAction() + " for key " + job.getTarget() + ": " + e.getMessage());
                        jobService.updateFailingJob(job, e);
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "interrupted while trying to take next job", e);
                }

            }
        }
        
        public void buildFromWorkspace(String wskey, String snapshot) throws OaiServiceException {
    		try {
    			HashSet<String> setsWorkspace = new HashSet<String>(Arrays.asList(wskey));

    			if (snapshot == null) {
    				snapshot = core.findWorkspaceLatestPublishedSnapshot(wskey);
    			}
    			if (snapshot == null) {
    				LOGGER.log(Level.WARNING, "finds no published snapshot for workspace " + wskey);
    				return;
    			}
    			LOGGER.log(Level.FINE, "build from workspace " + wskey + " and snapshot " + snapshot);
    			Workspace workspace = core.readWorkspace(wskey);
    			String root = workspace.findSnapshotByName(snapshot).getKey();

    			List<Record> records = null;
    			try {
    				records = oai.listRecordsByIdentifier(wskey);
    			} catch (RecordNotFoundException e) {
    			}
    			if (records == null) {
    				LOGGER.log(Level.FINE, "creating OAI records and a set for workspace " + wskey);
    				// Creating a Set for the workspace
    				try {
    					oai.createSet(wskey, "Workspace " + wskey);
    				} catch (SetAlreadyExistsException e) {
    					LOGGER.log(Level.WARNING, "unable to create a Set " + wskey, e);
    				}
    				createRecordsForItem(wskey, root, setsWorkspace);
    			} else {
    				LOGGER.log(Level.FINE, "updating OAI records and set for workspace " + wskey);
    				// Set is already created (see below), so just cleans and
    				// creates new records
    				// Deleting all Records linking of the workspace
    				List<Record> recordsOfWorkspace;
    				try {
    					recordsOfWorkspace = oai.listRecordsBySet(wskey);
    					recordsOfWorkspace.forEach(rec -> {
    						try {
    							oai.deleteRecord(rec.getId());
    						} catch (RecordNotFoundException e) {
    						}
    					});
    				} catch (RecordNotFoundException e1) {
    					// No record
    				}
    				createRecordsForItem(wskey, root, setsWorkspace);
    			}
    		} catch (RegistryServiceException | KeyNotFoundException | OaiServiceException | CoreServiceException
    				| MetadataPrefixUnknownException | OrtolangException e) {
    			LOGGER.log(Level.SEVERE, "unable to create OAI record for workspace " + wskey, e);
    			throw new OaiServiceException("unable to create OAI records for workspace " + wskey, e);
    		}
    	}

    	private void createRecordsForItem(String wskey, String root, HashSet<String> setsWorkspace)
    			throws CoreServiceException, KeyNotFoundException, RegistryServiceException, OaiServiceException,
    			MetadataPrefixUnknownException, OrtolangException {
    		oai.createRecord(wskey, MetadataFormat.OAI_DC, registry.getLastModificationDate(root),
    				buildXMLFromItem(root, MetadataFormat.OAI_DC), setsWorkspace);
    		oai.createRecord(wskey, MetadataFormat.OLAC, registry.getLastModificationDate(root),
    				buildXMLFromItem(root, MetadataFormat.OLAC), setsWorkspace);
    		oai.createRecord(wskey, MetadataFormat.CMDI, registry.getLastModificationDate(root),
    				buildXMLFromItem(root, MetadataFormat.CMDI), setsWorkspace);

//    		createRecordsFromMetadataObject(root, setsWorkspace);
    		OrtolangObject object = core.findObject(root);
    		java.util.Set<CollectionElement> elements = ((Collection) object).getElements();
    		for (CollectionElement element : elements) {
    			createRecordsFromMetadataObject(element.getKey(), setsWorkspace);
    		}
    	}

    	/**
    	 * Creates records (one for each metadata format) related to an
    	 * OrtolangObject.
    	 * 
    	 * @param key
    	 * @param setsWorkspace
    	 *            the sets related
    	 * @throws OrtolangException
    	 * @throws OaiServiceException
    	 * @throws RegistryServiceException
    	 * @throws KeyNotFoundException
    	 * @throws CoreServiceException
    	 * @throws MetadataPrefixUnknownException
    	 */
    	private void createRecordsFromMetadataObject(String key, HashSet<String> setsWorkspace)
    			throws OrtolangException, OaiServiceException, RegistryServiceException, KeyNotFoundException,
    			CoreServiceException, MetadataPrefixUnknownException {
    		OrtolangObject object = core.findObject(key);
    		String type = object.getObjectIdentifier().getType();

    		switch (type) {
    		case Collection.OBJECT_TYPE:
    			createRecordsForEarchMetadataObject(key, setsWorkspace);
    			java.util.Set<CollectionElement> elements = ((Collection) object).getElements();
    			for (CollectionElement element : elements) {
    				createRecordsFromMetadataObject(element.getKey(), setsWorkspace);
    			}
    			break;
    		case DataObject.OBJECT_TYPE:
    			createRecordsForEarchMetadataObject(key, setsWorkspace);
    			break;
    		}
    	}

    	/**
    	 * Builds XML from root collection metadata (Item).
    	 * 
    	 * @param wskey
    	 * @param snapshot
    	 * @param metadataPrefix
    	 * @return
    	 * @throws OaiServiceException
    	 * @throws MetadataPrefixUnknownException
    	 */
    	private String buildXMLFromItem(String root, String metadataPrefix)
    			throws OaiServiceException, MetadataPrefixUnknownException {
    		LOGGER.log(Level.FINE,
    				"building XML from ITEM metadata of root collection " + root + " and metadataPrefix " + metadataPrefix);

    		String item = null;
    		try {
    			List<OrtolangIndexableContent> indexableContents = core.getIndexableContent(root);
    			if (indexableContents.size() > 0) {
    				item = indexableContents.get(0).getContent();
    			}
    		} catch (KeyNotFoundException | RegistryServiceException | IndexingServiceException | OrtolangException e1) {
    			LOGGER.log(Level.SEVERE, "unable to get json content from root collection " + root);
    			throw new OaiServiceException("unable to get json content from root collection " + root, e1);
    		}

    		if (item == null) {
    			LOGGER.log(Level.SEVERE, "unable to build xml from root collection cause item metadata is null " + root);
    			throw new OaiServiceException("unable to build xml from root collection cause item metadata " + root);
    		}

    		try {
    			StringWriter result = new StringWriter();
    			XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);
    		
    			XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);
    			
    			if (metadataPrefix.equals(MetadataFormat.OAI_DC)) {
    				// Writes DC metadata
    				DublinCoreHandler handler = new DublinCoreHandler();
    				handler.setListHandlesRoot(listHandlesForKey(root));
    				handler.writeItem(item, builder);
    			} else if (metadataPrefix.equals(MetadataFormat.OLAC)) {
    				// Writes OLAC metadata 
    				OlacHandler handler = new OlacHandler();
    				handler.setListHandlesRoot(listHandlesForKey(root));
    				handler.writeItem(item, builder);
    			} else if (metadataPrefix.equals(MetadataFormat.CMDI)) {
    				// Writes CMDI metadata 
    				CmdiHandler handler = new CmdiHandler();
    				handler.setListHandlesRoot(listHandlesForKey(root));
    				handler.writeItem(item, builder);
    			}
    	
    			if (writer != null) {
    				writer.flush();
    	            writer.close();
    			}
    			if (!result.toString().isEmpty()) {
    				// Validates the XML Document
    				try {
						XmlUtils.validateXml(result.toString());
					} catch (SAXException | IOException e) {
						throw new OaiServiceException(
        						"unable to build xml for oai record cause xml is not valid ", e);
					}
    					
    				return result.toString();
    			} else {
    				throw new MetadataPrefixUnknownException(
    						"unable to build xml for oai record cause metadata prefix unknown " + metadataPrefix);
    			}
    		} catch (XMLStreamException | FactoryConfigurationError | MetadataHandlerException e) {
    			throw new MetadataPrefixUnknownException(
    					"unable to build xml for oai record cause metadata prefix unknown " + metadataPrefix, e);
    		}
    	}

    	private List<String> listHandlesForKey(String key) {
    		List<String> urls = new ArrayList<String>();
    		try {
    			List<String> handles = handleStore.listHandlesForKey(key);
    			for (String handle : handles) {
    				urls.add("http://hdl.handle.net/" + handle);
    			}
    		} catch (NullPointerException | ClassCastException | HandleStoreServiceException e) {
    			LOGGER.log(Level.WARNING, "No handle for key " + key, e);
    		}
    		return urls;
    	}

    	/**
    	 * Creates records for each metadata format availabled on the
    	 * OrtolangObject.
    	 * 
    	 * @param key
    	 * @param setsWorkspace
    	 * @throws OaiServiceException
    	 * @throws RegistryServiceException
    	 * @throws KeyNotFoundException
    	 * @throws MetadataPrefixUnknownException
    	 */
    	private void createRecordsForEarchMetadataObject(String key, HashSet<String> setsWorkspace)
    			throws OaiServiceException, RegistryServiceException, KeyNotFoundException, MetadataPrefixUnknownException {

    		boolean olac = metadataObjectExists(key, MetadataFormat.OLAC);
    		boolean oai_dc = metadataObjectExists(key, MetadataFormat.OAI_DC);
    		boolean cmdi = metadataObjectExists(key, MetadataFormat.CMDI);
    		
    		if (oai_dc) {
    			String oai_dcXml = buildXMLFromMetadataObject(key, MetadataFormat.OAI_DC);
    			oai.createRecord(key, MetadataFormat.OAI_DC, registry.getLastModificationDate(key), oai_dcXml, setsWorkspace);
    		} else if (olac) {
    			String oai_dcXml = buildXMLFromMetadataObject(key, MetadataFormat.OLAC, MetadataFormat.OAI_DC);
    			oai.createRecord(key, MetadataFormat.OAI_DC, registry.getLastModificationDate(key), oai_dcXml, setsWorkspace);
    		} else if (cmdi) {
    			//TODO Downgrade CMDI OLAC to DC
    		}

    		if (olac) {
    			String olacXml = buildXMLFromMetadataObject(key, MetadataFormat.OLAC);
    			oai.createRecord(key, MetadataFormat.OLAC, registry.getLastModificationDate(key), olacXml, setsWorkspace);
    		} else if (cmdi) {
    			//TODO Downgrade CMDI OLAC to OLAC
    		} else if (oai_dc) {
    			String olacXml = buildXMLFromMetadataObject(key, MetadataFormat.OAI_DC, MetadataFormat.OLAC);
    			oai.createRecord(key, MetadataFormat.OLAC, registry.getLastModificationDate(key), olacXml, setsWorkspace);
    		}
    	}

    	/**
    	 * Builds XML from OrtolangObject.
    	 * 
    	 * @param key
    	 * @param metadataPrefix
    	 * @return
    	 * @throws OaiServiceException
    	 */
    	private String buildXMLFromMetadataObject(String key, String metadataPrefix)
    			throws OaiServiceException, MetadataPrefixUnknownException {
    		return buildXMLFromMetadataObject(key, metadataPrefix, metadataPrefix);
    	}

    	/**
    	 * Builds XML from OrtolangObject.
    	 * 
    	 * @param key
    	 * @param metadataPrefix
    	 * @return
    	 * @throws OaiServiceException
    	 */
    	private String buildXMLFromMetadataObject(String key, String metadataPrefix, String outputMetadataFormat)
    			throws OaiServiceException, MetadataPrefixUnknownException {
    		LOGGER.log(Level.FINE,
    				"creating OAI record for ortolang object " + key + " for metadataPrefix " + metadataPrefix);
    		try {
    			List<String> mdKeys = core.findMetadataObjectsForTargetAndName(key, metadataPrefix);
    			StringWriter result = new StringWriter();
    			
    			if (!mdKeys.isEmpty()) {
    				XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(result);
    				
    				XMLMetadataBuilder builder = new XMLMetadataBuilder(writer);

    				String mdKey = mdKeys.get(0);
    				MetadataObject md = core.readMetadataObject(mdKey);
    				if (outputMetadataFormat.equals(MetadataFormat.OAI_DC)) {
    					// Output : OAI_DC XML
    					if (metadataPrefix.equals(MetadataFormat.OAI_DC)) {
    						// Input : OAI_DC JSON
    						DublinCoreHandler handler = new DublinCoreHandler();
    						handler.setListHandlesRoot(listHandlesForKey(key));
    						handler.write(StreamUtils.getContent(binaryStore.get(md.getStream())), builder);
    					} else if (metadataPrefix.equals(MetadataFormat.OLAC)) {
    						// Downgrades
    						// Input : OLAC JSON
    						DublinCoreOutputConverter converter = new DublinCoreOutputConverter();
    						converter.setListHandles(listHandlesForKey(key));
    						converter.convert(StreamUtils.getContent(binaryStore.get(md.getStream())), metadataPrefix, builder);
    					}
    				} else if (outputMetadataFormat.equals(MetadataFormat.OLAC)) {
    					// Output : OLAC XML
    					// Input : OLAC | OAI_DC JSON
    					OlacHandler handler = new OlacHandler();
    					handler.setListHandlesRoot(listHandlesForKey(key));
    					handler.write(StreamUtils.getContent(binaryStore.get(md.getStream())), builder);
    				} else if (outputMetadataFormat.equals(MetadataFormat.CMDI)) {
    					// Output : CMDI OLAC XML
    					// Input : OLAC | OAI_DC JSON
    					CmdiOutputConverter converter = new CmdiOutputConverter();
    					converter.convert(StreamUtils.getContent(binaryStore.get(md.getStream())), metadataPrefix, builder);
    				}

    				if (writer != null) {
    					writer.flush();
    		            writer.close();
    				}
    			}

    			if (!result.toString().isEmpty()) {
    				// Validates the XML Document
    				try {
						XmlUtils.validateXml(result.toString());
					} catch (SAXException | IOException e) {
						throw new OaiServiceException(
        						"unable to build xml for oai record cause xml is not valid ", e);
					}
    				return result.toString();
    			} else {
    				throw new MetadataPrefixUnknownException(
    						"unable to build xml for oai record cause metadata prefix unknown " + metadataPrefix);
    			}
    		} catch (OrtolangException | KeyNotFoundException | CoreServiceException | IOException
    				| BinaryStoreServiceException | DataNotFoundException | XMLStreamException | FactoryConfigurationError | MetadataHandlerException | MetadataConverterException e) {
    			LOGGER.log(Level.SEVERE, "unable to build oai_dc from ortolang object  " + key, e);
    			throw new OaiServiceException("unable to build xml for oai record");
    		}
    	}

    	private boolean metadataObjectExists(String key, String metadataPrefix) {
    		try {
    			List<String> mdKeys = core.findMetadataObjectsForTargetAndName(key, metadataPrefix);
    			if (!mdKeys.isEmpty()) {
    				return true;
    			}
    		} catch (AccessDeniedException | CoreServiceException e) {
    		}
    		return false;
    	}
    	
    }

}
