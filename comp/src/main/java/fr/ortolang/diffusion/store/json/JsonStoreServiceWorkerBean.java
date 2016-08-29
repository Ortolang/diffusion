package fr.ortolang.diffusion.store.json;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import com.orientechnologies.common.exception.OException;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexableObjectFactory;
import fr.ortolang.diffusion.OrtolangJob;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.jobs.JobService;
import fr.ortolang.diffusion.jobs.entity.Job;
import fr.ortolang.diffusion.registry.RegistryService;
import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedThreadFactory;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Startup
@Singleton(name = JsonStoreServiceWorker.WORKER_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class JsonStoreServiceWorkerBean implements JsonStoreServiceWorker {

    private static final Logger LOGGER = Logger.getLogger(JsonStoreServiceWorkerBean.class.getName());

    private static final long DEFAULT_INDEXATION_DELAY = 15000;

    private static final String JOB_TYPE = "json-indexing";

    @EJB
    private JsonStoreService store;
    @EJB
    private RegistryService registry;
    @EJB
    private JobService jobService;
    @Resource
    @SuppressWarnings("EjbEnvironmentInspection")
    private ManagedThreadFactory managedThreadFactory;

    private JsonStoreWorkerThread worker;

    private Thread workerThread;

    private DelayQueue<Job> queue;

    public JsonStoreServiceWorkerBean() {
    }

    @Override
    @PostConstruct
    public void start() {
        if (workerThread != null && workerThread.isAlive()) {
            LOGGER.log(Level.WARNING, "JSON worker already started");
            return;
        }
        LOGGER.log(Level.INFO, "Starting json store worker thread");
        worker = new JsonStoreWorkerThread();
        queue = new DelayQueue<>();
        workerThread = managedThreadFactory.newThread(worker);
        workerThread.setName("Json Store Worker Thread");
        workerThread.start();
        retryAll(false);
    }

    @Override
    @PreDestroy
    public void stop() {
        LOGGER.log(Level.INFO, "Stopping json store worker thread");
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
        if (failed) {
            List<Job> failedJobs = jobService.getFailedJobsOfType(JOB_TYPE);
            jobs.addAll(failedJobs);
        }
        LOGGER.log(Level.INFO, "Put " + jobs.size() + " json-indexing jobs in queue");
        queue.addAll(jobs);
    }

    @Override
    public void submit(String key, String action) {
        LOGGER.log(Level.FINE, "submit new job action: " + action + " for key: " + key);
        Job existingJob = getJob(key);
        if (existingJob != null) {
            LOGGER.log(Level.FINE, "a job already exists for key: " + key);
            if (existingJob.getAction().equals(action)) {
                LOGGER.log(Level.FINE, "existing job action is the same, removing old job for key: " + key);
                queue.remove(existingJob);
                jobService.remove(existingJob.getId());
                queue.put(jobService.create(JOB_TYPE, action, key, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
            } else if (existingJob.getAction().equals(IndexingService.INDEX_ACTION)) {
                LOGGER.log(Level.FINE, "existing job action is stale, removing old job for key: " + key);
                queue.remove(existingJob);
                jobService.remove(existingJob.getId());
                queue.put(jobService.create(JOB_TYPE, action, key, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
            } else {
                LOGGER.log(Level.WARNING, "existing job action is conflicting, dropping new job for key: " + key);
            }
        } else {
            queue.put(jobService.create(JOB_TYPE, action, key, System.currentTimeMillis() + DEFAULT_INDEXATION_DELAY));
        }
    }

    private Job getJob(String key) {
        for (Job job : queue) {
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
                    Job job = queue.take();
                    LOGGER.log(Level.FINE, "treating action: " + job.getAction() + " for target: " + job.getTarget());
                    try {
                        switch (job.getAction()) {
                        case IndexingService.INDEX_ACTION:
                            OrtolangIndexableObject<IndexableJsonContent> object;
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
                        jobService.remove(job.getId());
                    } catch (OException | JsonStoreServiceException | OrtolangException e) {
                        LOGGER.log(Level.WARNING, "unable to perform job action " + job.getAction() + " for key " + job.getTarget() + ": " + e.getMessage());
                        jobService.updateFailingJob(job, e);
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "interrupted while trying to take next job", e);
                }

            }
        }

    }

}