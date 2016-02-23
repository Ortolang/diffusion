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

import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    
    public int getQueueSize() {
        return queue.size();
    }
    
    public List<OrtolangJob> getQueueJobs() {
        return queue.stream().collect(Collectors.toList());
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
