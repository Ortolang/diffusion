package fr.ortolang.diffusion.extraction;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 * *
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

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.MetadataFormatException;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.extraction.parser.OrtolangXMLParser;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.jobs.JobService;
import fr.ortolang.diffusion.jobs.entity.Job;
import fr.ortolang.diffusion.registry.*;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.*;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TransactionRequiredException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Singleton
@Startup
@RunAs("system")
@SecurityDomain("ortolang")
@PermitAll
public class ExtractionServiceWorker {

    private static final Logger LOGGER = Logger.getLogger(ExtractionServiceWorker.class.getName());

    private static final int DELAY = 3000;

    @EJB
    private CoreService core;

    @EJB
    private BinaryStoreService binarystore;

    @EJB
    private RegistryService registry;

    @EJB
    private JobService jobService;

    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;

    @SuppressWarnings("EjbEnvironmentInspection")
    @Resource
    private ManagedThreadFactory managedThreadFactory;

    private ExtractionWorkerThread worker;

    private DelayQueue<Job> queue;

    public ExtractionServiceWorker() {
        this.worker = new ExtractionWorkerThread();
        this.queue = new DelayQueue<>();
    }

    @PostConstruct
    public void init() {
        startThread();
        List<Job> extractionJobs = jobService.getJobsOfType(ExtractionService.JOB_TYPE);
        LOGGER.log(Level.INFO, "Restoring " + extractionJobs.size() + " extraction jobs in queue");
        queue.addAll(extractionJobs);
    }

    private void startThread() {
        LOGGER.log(Level.INFO, "Starting extraction worker thread");
        Thread thread = managedThreadFactory.newThread(worker);
        thread.setName("Extraction Worker Thread");
        thread.start();
        Thread.UncaughtExceptionHandler h = (th, ex) -> {
            LOGGER.log(Level.SEVERE, "Uncaught exception", ex);
            startThread();
        };
        thread.setUncaughtExceptionHandler(h);
    }

    @PreDestroy
    public void stop() {
        LOGGER.log(Level.INFO, "Stopping extraction worker thread");
        worker.stop();
    }

    public int getQueueSize() {
        return queue.size();
    }

    public List<Job> getQueueJobs() {
        return queue.stream().collect(Collectors.toList());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void submit(String key) {
        Job job = jobService.create(ExtractionService.JOB_TYPE, ExtractionService.EXTRACT_ACTION, key, System.currentTimeMillis() + DELAY);
        queue.put(job);
    }

    class ExtractionWorkerThread implements Runnable {

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
                    String key = job.getTarget();
                    try {
                        switch (job.getAction()) {
                        case ExtractionService.EXTRACT_ACTION: {
                            OrtolangObjectIdentifier identifier;
                            identifier = registry.lookup(key);
                            if (!identifier.getService().equals(core.getServiceName()) || !identifier.getType().equals(DataObject.OBJECT_TYPE)) {
                                throw new CoreServiceException("target can only be DataObject.");
                            }
                            OrtolangObject object = em.find(DataObject.class, identifier.getId());
                            DataObject dataObject = (DataObject) object;
                            String mimeType = dataObject.getMimeType();
                            String hash = dataObject.getStream();
                            LOGGER.log(Level.FINEST, "Extracting metadata for data object with key: " + key);
                            // Do not parse xml files that are too large (greater than 50 MB)
                            if ("application/xml".equals(mimeType) && binarystore.size(hash) > 50000000) {
                                jobService.remove(job.getId());
                                continue;
                            }
                            Metadata metadata = binarystore.parse(hash);
                            String contentType = metadata.get("Content-Type");
                            String metadataName = null;
                            if (mimeType.startsWith("audio/") || contentType.startsWith("audio/")) {
                                metadataName = MetadataFormat.AUDIO;
                            } else if (mimeType.startsWith("image/") || contentType.startsWith("image/")) {
                                metadataName = MetadataFormat.IMAGE;
                            } else if (mimeType.startsWith("video/") || contentType.startsWith("video/")) {
                                metadataName = MetadataFormat.VIDEO;
                            } else if ("application/xml".equals(mimeType) || "application/xml".equals(contentType) || mimeType.endsWith("+xml") || contentType.endsWith("+xml")) {
                                if (metadata.get(OrtolangXMLParser.XML_TYPE_KEY) != null) {
                                    metadataName = MetadataFormat.XML;
                                }
                            } else if ("application/pdf".equals(contentType)) {
                                metadataName = MetadataFormat.PDF;
                            } else if (contentType.contains("text/")) {
                                metadataName = MetadataFormat.TEXT;
                            } else {
                                String[] parsers = metadata.getValues("X-Parsed-By");
                                for (String parser : parsers) {
                                    if (parser.contains("org.apache.tika.parser.microsoft") || parser.contains("org.apache.tika.parser.odf")) {
                                        metadataName = MetadataFormat.OFFICE;
                                        break;
                                    }
                                }
                            }
                            if (metadataName != null) {
                                JSONObject metadataJson = new JSONObject();
                                for (String name : metadata.names()) {
                                    if (metadata.isMultiValued(name)) {
                                        JSONArray jsonArray = new JSONArray(Arrays.asList(metadata.getValues(name)));
                                        metadataJson.put(name, jsonArray);
                                    } else {
                                        if ("ortolang:json".equals(name)) {
                                            String tmp = metadataJson.toString();
                                            String ortolangJson = metadata.get("ortolang:json");
                                            tmp = tmp.substring(0, tmp.lastIndexOf("}"));
                                            tmp += ortolangJson.replaceFirst("\\{", ",");
                                            metadataJson = new JSONObject(tmp);
                                        } else {
                                            if ("File Name".equals(name) && metadata.get(name).startsWith("apache-tika")) {
                                                continue;
                                            }
                                            metadataJson.put(name, metadata.get(name));
                                        }
                                    }
                                }
                                String metadataHash = binarystore.put(new ByteArrayInputStream(metadataJson.toString().getBytes()));
                                core.systemCreateMetadata(key, metadataName, metadataHash, metadataName + ".json");
                            }
                            break;
                        }
                        default:
                            LOGGER.log(Level.WARNING, "unknown job action: " + job.getAction());
                        }
                        jobService.remove(job.getId());
                    } catch (MetadataFormatException | BinaryStoreServiceException | DataCollisionException | KeyNotFoundException | CoreServiceException | IdentifierAlreadyRegisteredException | RegistryServiceException | KeyAlreadyExistsException | DataNotFoundException | IOException | TikaException | JSONException | IndexingServiceException | AuthorisationServiceException e) {
                        LOGGER.log(Level.WARNING, "unable to extract metadata for data object with key " + key, e);
                        jobService.updateFailingJob(job, e);
                    } catch (SAXException e) {
                        LOGGER.log(Level.WARNING, "Could not parse XML document: removing extraction job " + job.getId() + "for data object with key " + key, e);
                        jobService.remove(job.getId());
                    }

                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "interrupted while trying to take next job", e);
                } catch (TransactionRequiredException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }

            }
        }

    }

}
