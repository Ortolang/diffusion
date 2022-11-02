package fr.ortolang.diffusion.archive;

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

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.jobs.JobService;
import fr.ortolang.diffusion.jobs.entity.Job;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.OrtolangJob;
import fr.ortolang.diffusion.archive.facile.FacileService;
import fr.ortolang.diffusion.core.CoreService;

@Startup
@Singleton(name = ArchiveServiceWorker.WORKER_NAME)
@RunAs("system")
@SecurityDomain("ortolang")
@PermitAll
public class ArchiveServiceWorkerBean implements ArchiveServiceWorker {

    private static final Logger LOGGER = Logger.getLogger(ArchiveServiceWorkerBean.class.getName());

    private static final int DELAY = 3000;

    private static final String JOB_TYPE = "archive";

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

    @EJB
    private ArchiveService archive;

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
        LOGGER.log(Level.INFO, "Checking {0} worker thread", JOB_TYPE);
        
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
        Job job = jobService.create(JOB_TYPE, action, key, System.currentTimeMillis() + DELAY, args);
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
                            case ArchiveService.CHECK_ACTION: {
                                archive.validateDataobject(key);
                                break;
                            }
                            case ArchiveService.CREATE_SIP_ACTION: {
                                String schema = job.getParameter("schema");
                                archive.createSIPTar(key, schema);
                                break;
                            }
                            default:
                                LOGGER.log(Level.WARNING, "unknown job action {0}",job.getAction());
                        }
                        jobService.remove(job.getId());
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,
                                "unable to check archivable for data object with key {0}", key);
                        LOGGER.log(Level.FINE, e.getMessage(), e);
                        jobService.updateFailingJob(job, e);
                    }

                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "interrupted while trying to take next job", e);
                }
            }
        }

    }
}