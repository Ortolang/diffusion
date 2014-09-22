package fr.ortolang.diffusion.runtime.task;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.transaction.UserTransaction;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.utilities.SimpleResult;

public class ImportWorkspaceTask extends Task {
	
	private static final int TRANSACTION_SIZE = 500;

	public static final String BAG_HASH_PARAM_NAME = "bag-hash";
	public static final String WSKEY_PARAM_NAME = "workspace-key";
	public static final String WSNAME_PARAM_NAME = "workspace-name";
	public static final String WSTYPE_PARAM_NAME = "workspace-type";
	public static final String OBJECTS_PREFIX = "data/objects/";
	public static final String METADATAS_PREFIX = "data/metadatas/";

	private Logger logger = Logger.getLogger(ImportWorkspaceTask.class.getName());
	private BinaryStoreService store;
	private CoreService core;
	private UserTransaction userTx;
	
	public ImportWorkspaceTask() {
		super();
	}
	
	public BinaryStoreService getBinaryStore() throws Exception {
		if (store == null) {
			store = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME);
		}
		return store;
	}

	protected void setBinaryStore(BinaryStoreService store) {
		this.store = store;
	}
	
	public CoreService getCoreService() throws Exception {
		if (core == null) {
			core = (CoreService) OrtolangServiceLocator.findService(CoreService.SERVICE_NAME);
		}
		return core;
	}

	protected void setCoreService(CoreService core) {
		this.core = core;
	}
	
	public UserTransaction getUserTransaction() throws Exception {
		if (userTx == null) {
			userTx = (UserTransaction) new InitialContext().lookup("java:jboss/UserTransaction");
		}
		return userTx;
	}

	@Override
	public TaskState execute() {
		this.log("Starting ImportBag task");
		try {
			if (!getParams().containsKey(BAG_HASH_PARAM_NAME)) {
				logger.log(Level.WARNING, "unable to find mandatory " + BAG_HASH_PARAM_NAME + " parameter");
				this.log("ImportBag task error : mandatory parameter " + BAG_HASH_PARAM_NAME + " not found in context!!");
				return TaskState.ERROR;
			}
			if (!getParams().containsKey(WSKEY_PARAM_NAME)) {
				logger.log(Level.WARNING, "unable to find mandatory " + WSKEY_PARAM_NAME + " parameter");
				this.log("ImportBag task error : mandatory parameter " + WSKEY_PARAM_NAME + " not found in context!!");
				return TaskState.ERROR;
			}
			if (!getParams().containsKey(WSNAME_PARAM_NAME)) {
				logger.log(Level.WARNING, "unable to find mandatory " + WSNAME_PARAM_NAME + " parameter");
				this.log("ImportBag task error : mandatory parameter " + WSNAME_PARAM_NAME + " not found in context!!");
				return TaskState.ERROR;
			}
			if (!getParams().containsKey(WSTYPE_PARAM_NAME)) {
				logger.log(Level.WARNING, "unable to find mandatory " + WSTYPE_PARAM_NAME + " parameter");
				this.log("ImportBag task error : mandatory parameter " + WSTYPE_PARAM_NAME + " not found in context!!");
				return TaskState.ERROR;
			}
			
			String hash = getParam(BAG_HASH_PARAM_NAME);
			logger.log(Level.INFO, BAG_HASH_PARAM_NAME + " parameter found: " + hash);
			
			this.log("loading bag from hash: " + hash);
			BagFactory factory = new BagFactory();
			Bag bag = factory.createBag(getBinaryStore().getFile(hash));
			this.log("bag loaded: " + bag.getBagItTxt());

			this.log("verifying bag content integrity...");
			long start = System.currentTimeMillis();
			SimpleResult result = bag.verifyPayloadManifests();
			if (!result.isSuccess()) {
				this.log("bag verification failed: " + result.messagesToString());
				return TaskState.ERROR;
			}
			long stop = System.currentTimeMillis();
			this.log("bag verification success done in " + (stop-start) + " ms");

			Collection<BagFile> payload = bag.getPayload();
			this.log("starting binary import of " + payload.size() + " bag elements...");
			start = System.currentTimeMillis();
			long size = 0;
			long cpt = 0;
			Map<String, String> objects = new HashMap<String, String>();
			Map<String, String> metadatas = new HashMap<String, String>();
			for (BagFile file : payload) {
				if ( file.getFilepath().startsWith(OBJECTS_PREFIX) ) {
					String object = file.getFilepath().substring(OBJECTS_PREFIX.length());
					try {
						InputStream is = file.newInputStream();
						String ohash = getCoreService().put(is);
						objects.put(object, ohash);
						is.close();
						cpt++;
						size += file.getSize();
					} catch ( CoreServiceException | DataCollisionException e ) {
						logger.log(Level.SEVERE, "unable to import binary content", e);
						this.log("error importing binary content of bag entry [" + file.getFilepath() + "]");
					}
				} else if ( file.getFilepath().startsWith(METADATAS_PREFIX) ) {
					String metadata = file.getFilepath().substring(METADATAS_PREFIX.length());
					try {
						InputStream is = file.newInputStream();
						String mdhash = getCoreService().put(is);
						metadatas.put(metadata, mdhash);
						is.close();
						cpt++;
						size += file.getSize();
					} catch ( CoreServiceException | DataCollisionException e ) {
						logger.log(Level.SEVERE, "unable to import binary content", e);
						this.log("error importing binary content of bag entry [" + file.getFilepath() + "]");
					}
				} else {
					this.log("unable to determine type for bag entry [" + file.getFilepath() + "], wrong folder");
				}
			}
			stop = System.currentTimeMillis();
			bag.close();
			this.log(cpt + " binary elements imported successfully in " + (stop-start) + " ms for a total size of " + size + " octets");
			
			this.log("starting objects creation...");
			start = System.currentTimeMillis();
			cpt = 0;
			String wskey = getParam(WSKEY_PARAM_NAME);
			String wsname = getParam(WSNAME_PARAM_NAME);
			String wstype = getParam(WSTYPE_PARAM_NAME);
			
			getUserTransaction().begin();
			int txCpt = 0;
			try {
				getCoreService().createWorkspace(wskey, wsname, wstype);
				cpt++;
			} catch ( Exception e ) {
				getUserTransaction().rollback();
				logger.log(Level.SEVERE, "unable to create workspace", e);
				this.log("- error creating workspace with key: " + wskey + ", see server log for more details");
				return TaskState.ERROR;
			}
			List<String> collections = new ArrayList<String>();
			for (Entry<String, String> object : objects.entrySet()) {
				logger.log(Level.FINE, "treating object entry: " + object.getKey());
				try {
					PathBuilder opath = PathBuilder.fromPath(object.getKey());
					PathBuilder oppath = opath.clone().parent();
					if ( !oppath.isRoot() && !collections.contains(oppath.build()) ) {
						String[] parents = opath.clone().parent().buildParts();
						String current = "";
						for ( int i=0; i<parents.length; i++ ) {
							current += "/" + parents[i];
							if ( !collections.contains(current) ) {
								try {
									logger.log(Level.FINE, "creating collection for path: " + current);
									getCoreService().createCollection(wskey, current, "no description provided");
									collections.add(current);
									cpt++;
								} catch ( Exception e ) {
									logger.log(Level.SEVERE, "unable to create collection", e);
									this.log("- error creating collection at path: " + current + ", should result in data object creation error, see server log for more details");
								}
							} 
						}
					}
					String current = opath.build();
					try {
						logger.log(Level.FINE, "creating data object for path: " + current);
						getCoreService().createDataObject(wskey, current, "no description provided", object.getValue());
						cpt++;
					} catch ( Exception e ) {
						logger.log(Level.SEVERE, "unable to create object", e);
						this.log("- error creating data object at path: " + current + ", see server log for more details");
					}
					if ( (cpt / TRANSACTION_SIZE) > txCpt) {
						logger.log(Level.FINE, "commiting transaction");
						getUserTransaction().commit();
						logger.log(Level.FINE, "beginning new transaction");
						getUserTransaction().begin();
						txCpt++;
					}
				} catch ( InvalidPathException e ) {
					this.log("- error creating data object with path: " + object.getKey() + ", invalid path");
				}
			}
			for (Entry<String, String> metadata : metadatas.entrySet()) {
				logger.log(Level.FINE, "treating metadata entry: " + metadata.getKey());
				int lastPathIndex = metadata.getKey().lastIndexOf("/");
				String mdfullname = metadata.getKey();
				String mdname = mdfullname;
				String mdformat = "unknown";
				String mdpath = "/";
				if ( lastPathIndex > -1 ) {
					mdfullname = metadata.getKey().substring(lastPathIndex+1);
					mdpath = metadata.getKey().substring(0, lastPathIndex);
				}
				if (mdfullname.indexOf("[") == 0 && mdfullname.indexOf("]") >= 0) {
					mdformat = mdfullname.substring(1, mdfullname.indexOf("]")).trim();
					mdname = mdfullname.substring(mdfullname.indexOf("]")+1).trim();
				}
				try {
					logger.log(Level.FINE, "creating metadata object for path: " + mdpath + " with name: " + mdname + " and format: " + mdformat);
					getCoreService().createMetadataObject(wskey, mdpath, mdname, mdformat, metadata.getValue());
					cpt++;
				} catch ( Exception e ) {
					logger.log(Level.SEVERE, "unable to create metadata", e);
					this.log("- error creating metadata for path: " + mdpath + " and name: " + mdname + ", see server log for more details");
				}
				if ( (cpt / TRANSACTION_SIZE) > txCpt) {
					logger.log(Level.FINE, "commiting transaction");
					getUserTransaction().commit();
					logger.log(Level.FINE, "beginning new transaction");
					getUserTransaction().begin();
					txCpt++;
				}
			}
			logger.log(Level.FINE, "commiting transaction");
			getUserTransaction().commit();
			stop = System.currentTimeMillis();
			this.log(cpt + " objects created in " + (stop-start) + " ms");
	
			this.log("ImportBag task ended");
			return TaskState.COMPLETED;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error during task execution", e);
			this.log("unexpected error occured during task execution: " + e.getMessage() + ", see server log for further details");
			return TaskState.ERROR;
		}
	}
	
}
