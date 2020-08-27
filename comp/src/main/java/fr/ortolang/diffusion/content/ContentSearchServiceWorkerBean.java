package fr.ortolang.diffusion.content;

import java.util.HashMap;
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

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangJob;
import fr.ortolang.diffusion.jobs.JobService;
import fr.ortolang.diffusion.jobs.entity.Job;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.store.handle.HandleStoreService;

@Startup
@Singleton(name = ContentSearchServiceWorker.WORKER_NAME)
@RunAs("system")
@SecurityDomain("ortolang")
@PermitAll
public class ContentSearchServiceWorkerBean implements ContentSearchServiceWorker {

    private static final Logger LOGGER = Logger.getLogger(ContentSearchServiceWorkerBean.class.getName());

    private static final String JOB_TYPE = "content";

    private static final int DELAY = 5000;

    @EJB
    private RegistryService registry;
    @EJB
    private JobService jobService;
    @EJB
    private ContentSearchService contentSearch;
    @EJB
    private HandleStoreService handle;
    
    @Resource
    private ManagedThreadFactory managedThreadFactory;

    private ContentSearchWorkerThread worker;

    private Thread workerThread;

    private DelayQueue<Job> queue;
    
    public ContentSearchServiceWorkerBean() { }

	@Override
    @PostConstruct
	public void start() {
		if (workerThread != null && workerThread.isAlive()) {
            LOGGER.log(Level.WARNING, "ContentSearch Service worker already started");
            return;
        }
        LOGGER.log(Level.INFO, "Starting ContentSearch Service worker thread");
        worker = new ContentSearchWorkerThread();
        queue = new DelayQueue<>();
        workerThread = managedThreadFactory.newThread(worker);
        workerThread.setName("ContentSearch Worker Thread");
        workerThread.start();
        retryAll(false);
	}

	@Override
    @PreDestroy
	public void stop() {
		LOGGER.log(Level.INFO, "Stopping ContentSearch worker thread");
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
        LOGGER.log(Level.INFO, "Restoring " + jobs.size() + " ContentSearch jobs in queue");
        if (failed) {
            List<Job> failedJobs = jobService.getFailedJobsOfType(JOB_TYPE);
            LOGGER.log(Level.INFO, "Retrying " + failedJobs.size() + " failed ContentSearch jobs");
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
		Job job = jobService.create(JOB_TYPE, INDEX_ACTION, key, System.currentTimeMillis() + DELAY, args);
        queue.put(job);
	}


    private class ContentSearchWorkerThread implements Runnable {

        private boolean run = true;

        public void stop() {
            this.run = false;
        }

        @Override
        public void run() {
            while (run) {
                try {
                    Job job = queue.take();
                    LOGGER.log(Level.FINE, "treating ContentSearch action: " + job.getAction() + " for target: " + job.getTarget());
                    try {
                        switch (job.getAction()) {
                        case INDEX_ACTION:
                        	String snapshot = job.getParameter("snapshot");
                        	contentSearch.indexResourceFromWorkspace(job.getTarget(), snapshot);
                            break;
                            //TODO case remove ACTION
                        default:
                            LOGGER.log(Level.WARNING, "unknown ContentSearch job action: " + job.getAction());
                        }
                        jobService.remove(job.getId());
                    } catch (ContentSearchNotFoundException | ContentSearchServiceException e) {
                        LOGGER.log(Level.WARNING, "unable to perform ContentSearch job action " + job.getAction() + " for key " + job.getTarget() + ": " + e.getMessage());
                        jobService.updateFailingJob(job, e);
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "interrupted while trying to take next ContentSearch job", e);
                }

            }
        }
        
    }
}
