package fr.ortolang.diffusion.jobs;

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
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.jobs.entity.Job;
import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.security.PermitAll;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Local(JobService.class)
@Stateless(name = JobService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class JobServiceBean implements JobService {

    private static final Logger LOGGER = Logger.getLogger(JobServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };

    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;

    public JobServiceBean() {
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Job read(String id) {
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
        Job job = new Job(type, action, target, timestamp, args);
        em.persist(job);
        return job;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(Long id) {
        Job job = em.find(Job.class, id);
        if (job != null) {
            em.remove(job);
        }
    }

    @Override
    public long countJobs() {
        return em.createNamedQuery("countAllJobs", Long.class).getSingleResult();
    }

    @Override
    public List<Job> getJobs() {
        return em.createNamedQuery("listAllJobs", Job.class).getResultList();
    }

    @Override
    public long countJobsOfType(String type) {
        return em.createNamedQuery("countJobsOfType", Long.class).setParameter("type", type).getSingleResult();
    }

    @Override
    public List<Job> getJobsOfType(String type) {
        return em.createNamedQuery("listJobsOfType", Job.class).setParameter("type", type).getResultList();
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

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        return OBJECT_PERMISSIONS_LIST;
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }
}
