package fr.ortolang.diffusion.store.json;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexableObjectFactory;
import fr.ortolang.diffusion.OrtolangJob;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.registry.RegistryService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedThreadFactory;
import java.util.concurrent.DelayQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class JsonStoreServiceWorker {

    private static final Logger LOGGER = Logger.getLogger(JsonStoreServiceWorker.class.getName());
    private static final long DEFAULT_INDEXATION_DELAY = 15000;

    @EJB
    private JsonStoreService store;
    @EJB
    private RegistryService registry;
    @Resource
    private ManagedThreadFactory managedThreadFactory;
    private JsonStoreWorkerThread worker;
    private DelayQueue<OrtolangJob> queue;

    public JsonStoreServiceWorker() {
        this.worker = new JsonStoreWorkerThread();
        this.queue = new DelayQueue<OrtolangJob>();
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
        OrtolangJob existingJob = getJob(key);
        if (existingJob != null) {
            LOGGER.log(Level.FINE, "a job already exists for key: " + key);
            if (existingJob.getAction().equals(action)) {
                LOGGER.log(Level.FINE, "existing job action is the same, removing old job for key: " + key);
                queue.remove(existingJob);
                queue.put(new OrtolangJob(action, key, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
            } else if (existingJob.getAction().equals(IndexingService.INDEX_ACTION)) {
                LOGGER.log(Level.FINE, "existing job action is stale, removing old job for key: " + key);
                queue.remove(existingJob);
                queue.put(new OrtolangJob(action, key, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
            } else {
                LOGGER.log(Level.WARNING, "existing job action is conflicting, dropping new job for key: " + key);
            }
        } else {
            queue.put(new OrtolangJob(action, key, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
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

    class JsonStoreWorkerThread implements Runnable {

        private boolean run = true;

        public void stop() {
            this.run = false;
        }

        @Override
        public void run() {
            while (run) {
                try {
                    OrtolangJob job = queue.take();
                    LOGGER.log(Level.FINE, "treating action: " + job.getAction() + " for target: " + job.getTarget());
                    try {
                        switch (job.getAction()) {
                            case IndexingService.INDEX_ACTION:
                                OrtolangIndexableObject<IndexableJsonContent> object = null;
                                try {
                                    object = OrtolangIndexableObjectFactory.buildJsonIndexableObject(job.getTarget());
                                    store.index(object);
                                    LOGGER.log(Level.FINE, "key " + job.getTarget() + " added to json store");
                                } catch (NotIndexableContentException e) {
                                    LOGGER.log(Level.FINE, "key " + job.getTarget() + " not indexable");
                                }
                                break;
                            case IndexingService.REMOVE_ACTION:
                                store.remove(job.getTarget());
                                LOGGER.log(Level.FINE, "key " + job.getTarget() + " removed from json store");
                                break;
                            default:
                                LOGGER.log(Level.WARNING, "unknown job action: " + job.getAction());
                        }

                    } catch (JsonStoreServiceException | OrtolangException e) {
                        LOGGER.log(Level.WARNING, "unable to perform job action " + job.getAction() + " for key " + job.getTarget(), e);
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "interrupted while trying to take next job", e);
                }

            }
        }

    }

}
