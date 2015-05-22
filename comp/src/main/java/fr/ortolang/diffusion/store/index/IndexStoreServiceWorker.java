package fr.ortolang.diffusion.store.index;

import java.util.concurrent.DelayQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedThreadFactory;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexableObjectFactory;
import fr.ortolang.diffusion.OrtolangWorkerJob;
import fr.ortolang.diffusion.indexing.IndexingService;

@Singleton
@Startup
public class IndexStoreServiceWorker {
	
	private static final Logger LOGGER = Logger.getLogger(IndexStoreServiceWorker.class.getName());
	private static final long DEFAULT_INDEXATION_DELAY = 300000;
	
	@EJB
	private IndexStoreService store;
	@Resource
	private ManagedThreadFactory managedThreadFactory;
	private IndexStoreWorkerThread worker;
	private DelayQueue<OrtolangWorkerJob> queue;
	
	public IndexStoreServiceWorker() {
		this.worker = new IndexStoreWorkerThread();
		this.queue = new DelayQueue<OrtolangWorkerJob>();
	}
	
	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Starting index store worker thread");
		Thread thread = managedThreadFactory.newThread(worker);
		thread.setName("Index Store Worker Thread");
		thread.start();
	}
	
	@PreDestroy
	public void stop() {
		LOGGER.log(Level.INFO, "Stopping index store worker thread");
		worker.stop();
	}
	
	public void submit(String key, String action) throws IndexStoreServiceException {
		LOGGER.log(Level.FINE, "submit new job action: " + action + " for key: " + key);
		OrtolangWorkerJob existingJob = getJob(key);
		if ( existingJob != null ) {
			LOGGER.log(Level.FINEST, "a job already exists for this key: " + key);
			if ( existingJob.getAction().equals(action) ) {
				LOGGER.log(Level.FINEST, "existing job action is the same, removing old job for key: " + key);
				queue.remove(existingJob);
				queue.put(new OrtolangWorkerJob(key, action, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
			} else if ( existingJob.getAction().equals(IndexingService.INDEX_ACTION) ) {
				LOGGER.log(Level.FINEST, "existing job action is stale, removing old job for key: " + key);
				queue.remove(existingJob);
				queue.put(new OrtolangWorkerJob(key, action, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
			} else {
				LOGGER.log(Level.WARNING, "existing job action is conflicting, dropping new job for key: " + key);
			}
		} else {
			queue.put(new OrtolangWorkerJob(key, action, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
		}
	}
	
	private OrtolangWorkerJob getJob (String key) {
		for ( OrtolangWorkerJob job : queue ) {
			if ( job.getKey().equals(key) ) {
				return job;
			}
		}
		return null;
	}
	
	class IndexStoreWorkerThread implements Runnable {
		
		private boolean run = true;
		
		public void stop() {
			this.run = false;
		}

		@Override
		public void run() {
			while ( run ) {
				try {
					OrtolangWorkerJob job = queue.take();
					LOGGER.log(Level.FINE, "treating action: " + job.getAction() + " for key: " + job.getKey());
					try {
						switch ( job.getAction() ) {
							case IndexingService.INDEX_ACTION :
								OrtolangIndexableObject<IndexablePlainTextContent> object = OrtolangIndexableObjectFactory.buildPlainTextIndexableObject(job.getKey());
								store.index(object);
								LOGGER.log(Level.FINE, "key " + job.getKey() + " added to index store");
								break;
							case IndexingService.REMOVE_ACTION :
								store.remove(job.getKey());
								LOGGER.log(Level.FINE, "key " + job.getKey() + " removed from index store");
								break;
							default : 
								LOGGER.log(Level.WARNING, "unknown job action: " + job.getAction());
						}
					} catch ( IndexStoreServiceException | OrtolangException e ) {
						LOGGER.log(Level.WARNING, "unable to perform job action " + job.getAction() + " for key " + job.getKey(), e);
					}
				} catch ( InterruptedException e ) {
					LOGGER.log(Level.SEVERE, "interrupted while trying to take next job", e);
				}
				
			}
		}
		
	}

}
