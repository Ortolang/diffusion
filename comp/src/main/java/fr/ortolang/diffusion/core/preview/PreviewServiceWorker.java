package fr.ortolang.diffusion.core.preview;

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

import fr.ortolang.diffusion.OrtolangWorkerJob;

@Singleton
@Startup
public class PreviewServiceWorker {

	private static final Logger LOGGER = Logger.getLogger(PreviewServiceWorker.class.getName());
	private static final long DEFAULT_INDEXATION_DELAY = 10000;
	
	@EJB
	private PreviewService service;
	@Resource
	private ManagedThreadFactory managedThreadFactory;
	private ThumbnailServiceWorkerThread worker;
	private DelayQueue<OrtolangWorkerJob> queue;
	
	public PreviewServiceWorker() {
		this.worker = new ThumbnailServiceWorkerThread();
		this.queue = new DelayQueue<OrtolangWorkerJob>();
	}
	
	@PostConstruct
	public void init() {
		LOGGER.log(Level.INFO, "Starting preview worker thread");
		Thread thread = managedThreadFactory.newThread(worker);
		thread.setName("Preview Worker Thread");
		thread.start();
	}
	
	@PreDestroy
	public void stop() {
		LOGGER.log(Level.INFO, "Stopping preview worker thread");
		worker.stop();
	}
	
	public void submit(String key, String action) throws PreviewServiceException {
		LOGGER.log(Level.FINE, "submit new generate preview job action: " + action + " for key: " + key);
		OrtolangWorkerJob existingJob = getJob(key);
		if ( existingJob != null ) {
			queue.remove(existingJob);
		}
		queue.put(new OrtolangWorkerJob(key, action, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
	}
	
	private OrtolangWorkerJob getJob (String key) {
		for ( OrtolangWorkerJob job : queue ) {
			if ( job.getKey().equals(key) ) {
				return job;
			}
		}
		return null;
	}
	
	class ThumbnailServiceWorkerThread implements Runnable {
		
		private boolean run = true;
		
		public void stop() {
			this.run = false;
		}

		@Override
		public void run() {
			while ( run ) {
				try {
					OrtolangWorkerJob job = queue.take();
					LOGGER.log(Level.FINE, "trying to generate previews for key: " + job.getKey());
					try {
						service.generate(job.getKey());
					} catch ( PreviewServiceException e ) {
						LOGGER.log(Level.FINE, "unable to generate previews for key: " + job.getKey(), e);
					}
				} catch ( InterruptedException e ) {
					LOGGER.log(Level.SEVERE, "interrupted while trying to take next preview generation job", e);
				}
				
			}
		}
		
	}

}
