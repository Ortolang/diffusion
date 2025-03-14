package fr.ortolang.diffusion.jobs;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.jobs.entity.Job;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;

@Startup
@Local(JobService.class)
@Stateless(name = JobService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class JobServiceBean implements JobService {

    private static final Logger LOGGER = Logger.getLogger(JobServiceBean.class.getName());

    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @EJB
    private NotificationService notification;
    @Resource
    private SessionContext ctx;

    public JobServiceBean() {
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Job read(Long id) {
        return em.find(Job.class, id);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Job create(String type, String action, String target, long timestamp) {
        return create(type, action, target, timestamp, new HashMap<>());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Job create(String type, String action, String target, long timestamp, Map<String, String> args) {
        LOGGER.log(Level.FINE, "Creating job of type '" + type + "' (target: " + target + ')');
        Job job = new Job(type, action, target, timestamp, args);
        em.persist(job);
        return job;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(Long id) {
        Job job = em.find(Job.class, id);
        if (job != null) {
            LOGGER.log(Level.FINE, "Removing job " + id + " of type '" + job.getType() + "' (target: " + job.getTarget() + ')');
            em.remove(job);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void update(Job job) {
        LOGGER.log(Level.FINE, "Updating job " + job.getId() + " of type '" + job.getType() + "' (target: " + job.getTarget() + ')');
        em.merge(job);
    }

    @Override
    public void updateFailingJob(Job job, Exception e) {
        if (e instanceof KeyNotFoundException || (e.getCause() != null && e.getCause() instanceof KeyNotFoundException)) {
            LOGGER.log(Level.WARNING, "Key not found: removing job of type " + job.getType() + " (action: " + job.getAction() + ", target: " + job.getTarget() + ")");
            LOGGER.log(Level.FINE, "", e);
            remove(job.getId());
            return;
        }
        String times = null;
        if (job.containsParameter(Job.FAILED_TIMES_KEY)) {
            String parameter = job.getParameter(Job.FAILED_TIMES_KEY);
            times = String.valueOf(Integer.valueOf(parameter) + 1);
        }
        job.setFailed(true);
        job.setParameter(Job.FAILED_TIMES_KEY, times != null ? times : "1");
        job.setParameter(Job.FAILED_EXPLANATION_KEY, e.getClass().getName());
        job.setParameter(Job.FAILED_EXPLANATION_MSG_KEY, e.getMessage());
        if (e.getCause() != null) {
            job.setParameter(Job.FAILED_CAUSED_BY_KEY, e.getCause().getClass().getName());
            if (e.getCause().getMessage() != null) {
                // Quick fix before hibernate crashing on varchar length exceeded (#85)
                int messageLength = e.getCause().getMessage().length() > 254 ? 254 : e.getCause().getMessage().length();
                job.setParameter(Job.FAILED_CAUSED_BY_MSG_KEY, e.getCause().getMessage().substring(0, messageLength));
            }
        }
        update(job);
    }

    @Override
    public long countJobs() {
        return em.createNamedQuery("countAllJobs", Long.class).getSingleResult();
    }

    @Override
    public long countUnprocessedJobs() {
        return em.createNamedQuery("countUnprocessedJobs", Long.class).getSingleResult();
    }

    @Override
    public long countFailedJobs() {
        return em.createNamedQuery("countFailedJobs", Long.class).getSingleResult();
    }

    @Override
    public List<Job> getJobs() {
        return getJobs(null, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Job> getJobs(Integer offset, Integer limit) {
        TypedQuery<Job> query = em.createNamedQuery("listAllJobs", Job.class);
        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    @Override
    public long countJobsOfType(String type) {
        return em.createNamedQuery("countJobsOfType", Long.class).setParameter("type", type).getSingleResult();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Job> getUnprocessedJobsOfType(String type) {
        return getUnprocessedJobsOfType(type, null, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Job> getUnprocessedJobsOfType(String type, Integer offset, Integer limit) {
        TypedQuery<Job> query;
        if (type == null || type.isEmpty()) {
            query = em.createNamedQuery("listUnprocessedJobs", Job.class);
        } else {
            query = em.createNamedQuery("listUnprocessedJobsOfType", Job.class).setParameter("type", type);
        }
        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    @Override
    public List<Job> getFailedJobs() {
        return getFailedJobsOfType(null, null, null);
    }

    @Override
    public List<Job> getFailedJobsOfType(String type) {
        return getFailedJobsOfType(type, null, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Job> getFailedJobsOfType(String type, Integer offset, Integer limit) {
        TypedQuery<Job> query;
        if (type == null || type.isEmpty()) {
            query = em.createNamedQuery("listFailedJobs", Job.class);
        } else {
            query = em.createNamedQuery("listFailedJobsOfType", Job.class).setParameter("type", type);
        }
        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    @Override
    public List<Job> getJobsOfType(String type) {
        return getJobsOfType(type, null, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Job> getJobsOfType(String type, Integer offset, Integer limit) {
        TypedQuery<Job> query = em.createNamedQuery("listJobsOfType", Job.class).setParameter("type", type);
        if (offset != null) {
            query.setFirstResult(offset);
        }
        if (limit != null) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<>();
        infos.put(INFO_SIZE, String.valueOf(countJobs()));
        return infos;
    }

}
