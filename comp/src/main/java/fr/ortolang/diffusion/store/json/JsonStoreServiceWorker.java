package fr.ortolang.diffusion.store.json;

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
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.StoreWorkerJob;

@Singleton
@Startup
public class JsonStoreServiceWorker {

	private static final Logger LOGGER = Logger.getLogger(JsonStoreServiceWorker.class.getName());
	private static final long DEFAULT_INDEXATION_DELAY = 300000;

	@EJB
	private JsonStoreService store;
	@EJB
	private RegistryService registry;
	@Resource
	private ManagedThreadFactory managedThreadFactory;
	private JsonStoreWorkerThread worker;
	private DelayQueue<StoreWorkerJob> queue;

	public JsonStoreServiceWorker() {
		this.worker = new JsonStoreWorkerThread();
		this.queue = new DelayQueue<StoreWorkerJob>();
	}

	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Starting json store worker thread");
		Thread thread = managedThreadFactory.newThread(worker);
		thread.setName("Json Store Worker Thread");
		thread.start();
	}

	@PreDestroy
	public void stop() {
		LOGGER.log(Level.INFO, "Stopping json store worker thread");
		worker.stop();
	}

	public void submit(String key, String action) throws JsonStoreServiceException {
		LOGGER.log(Level.FINE, "submit new job action: " + action + " for key: " + key);
		StoreWorkerJob existingJob = getJob(key);
		if (existingJob != null) {
			LOGGER.log(Level.FINE, "a job already exists for key: " + key);
			if (existingJob.getAction().equals(action)) {
				LOGGER.log(Level.FINE, "existing job action is the same, removing old job for key: " + key);
				queue.remove(existingJob);
				queue.put(new StoreWorkerJob(key, action, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
			} else if (existingJob.getAction().equals(IndexingService.INDEX_ACTION)) {
				LOGGER.log(Level.FINE, "existing job action is stale, removing old job for key: " + key);
				queue.remove(existingJob);
				queue.put(new StoreWorkerJob(key, action, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
			} else {
				LOGGER.log(Level.WARNING, "existing job action is conflicting, dropping new job for key: " + key);
			}
		} else {
			queue.put(new StoreWorkerJob(key, action, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
		}
	}

	private StoreWorkerJob getJob(String key) {
		for (StoreWorkerJob job : queue) {
			if (job.getKey().equals(key)) {
				return job;
			}
		}
		return null;
	}

	class JsonStoreWorkerThread implements Runnable {

		private boolean run = true;

		public void stop() {
			this.run = false;
		}

		@Override
		public void run() {
			while (run) {
				try {
					StoreWorkerJob job = queue.take();
					LOGGER.log(Level.FINE, "treating action: " + job.getAction() + " for key: " + job.getKey());
					try {
						switch (job.getAction()) {
						case IndexingService.INDEX_ACTION:
							String status = registry.getPublicationStatus(job.getKey());
							if (status.equals(OrtolangObjectState.Status.PUBLISHED.value())) {
								LOGGER.log(Level.FINE, "key: " + job.getKey() + " is in state: " + status);
								OrtolangIndexableObject<IndexableJsonContent> object = OrtolangIndexableObjectFactory.buildJsonIndexableObject(job.getKey());
								store.index(object);
								LOGGER.log(Level.FINE, "key " + job.getKey() + " added to json store");
							} else {
								LOGGER.log(Level.FINE, "key is not in state: " + OrtolangObjectState.Status.PUBLISHED.value() + ", nothing to do");
							}
							break;
						case IndexingService.REMOVE_ACTION:
							store.remove(job.getKey());
							LOGGER.log(Level.FINE, "key " + job.getKey() + " removed from json store");
							break;
						default:
							LOGGER.log(Level.WARNING, "unknown job action: " + job.getAction());
						}

					} catch (KeyNotFoundException | RegistryServiceException | JsonStoreServiceException | OrtolangException e) {
						LOGGER.log(Level.WARNING, "unable to perform job action " + job.getAction() + " for key " + job.getKey(), e);
					}
				} catch (InterruptedException e) {
					LOGGER.log(Level.SEVERE, "interrupted while trying to take next job", e);
				}

			}
		}

	}

}
