package fr.ortolang.diffusion.statistics;

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

import fr.ortolang.diffusion.*;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.statistics.entity.StatisticValue;
import fr.ortolang.diffusion.statistics.entity.WorkspaceStatisticValue;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreService;
import fr.ortolang.diffusion.thumbnail.ThumbnailService;
import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.*;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Local(StatisticsService.class)
@Singleton(name = StatisticsService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class StatisticsServiceBean implements StatisticsService {

    private static final Logger LOGGER = Logger.getLogger(StatisticsServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
    private static final String SEPARATOR = ".";
    private static final Map<String, List<String>> STATS_NAMES = new HashMap<>();

    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @Resource
    private ManagedThreadFactory managedThreadFactory;

    private Thread piwikAllCollectorThread;

    private Thread piwikLatestCollectorThread;

    public StatisticsServiceBean() {
    }

    @PostConstruct
    private void init() {
        String[] registryInfos = new String[] { RegistryService.INFO_SIZE, RegistryService.INFO_PUBLISHED, RegistryService.INFO_DELETED, RegistryService.INFO_HIDDEN };
        STATS_NAMES.put(RegistryService.SERVICE_NAME, Arrays.asList(registryInfos));
        String[] coreInfos = new String[] { CoreService.INFO_WORKSPACES_ALL, CoreService.INFO_COLLECTIONS_ALL, CoreService.INFO_OBJECTS_ALL };
        STATS_NAMES.put(CoreService.SERVICE_NAME, Arrays.asList(coreInfos));
        String[] binaryInfos = new String[] { BinaryStoreService.INFO_SIZE, BinaryStoreService.INFO_FILES };
        STATS_NAMES.put(BinaryStoreService.SERVICE_NAME, Arrays.asList(binaryInfos));
        String[] membershipInfos = new String[] { MembershipService.INFO_PROFILES_ALL, MembershipService.INFO_GROUPS_ALL };
        STATS_NAMES.put(MembershipService.SERVICE_NAME, Arrays.asList(membershipInfos));
        String[] handleInfos = new String[] { HandleStoreService.INFO_TOTAL_SIZE };
        STATS_NAMES.put(HandleStoreService.SERVICE_NAME, Arrays.asList(handleInfos));
        String[] jsonInfos = new String[] { JsonStoreService.INFO_SIZE, JsonStoreService.INFO_DB_SIZE };
        STATS_NAMES.put(JsonStoreService.SERVICE_NAME, Arrays.asList(jsonInfos));
        String[] thumbnailInfos = new String[] { ThumbnailService.INFO_SIZE, ThumbnailService.INFO_FILES };
        STATS_NAMES.put(ThumbnailService.SERVICE_NAME, Arrays.asList(thumbnailInfos));
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> list() throws StatisticsServiceException {
        LOGGER.log(Level.FINEST, "listing all stats names");
        List<String> names = new ArrayList<> ();
        for ( Entry<String, List<String>> stat : STATS_NAMES.entrySet() ) {
            for ( String info : stat.getValue() ) {
                names.add(stat.getKey() + SEPARATOR + info);
            }
        }
        return names;
    }

    @Override
    @Schedule(hour="23")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void probe() throws StatisticsServiceException {
        LOGGER.log(Level.FINEST, "probing stat for fresh values");
        for ( Entry<String, List<String>> stat : STATS_NAMES.entrySet() ) {
            LOGGER.log(Level.FINEST, "looking up service: " + stat.getKey());
            try {
                OrtolangService service = OrtolangServiceLocator.findService(stat.getKey());
                Map<String, String> infos = service.getServiceInfos();
                for ( String info : stat.getValue() ) {
                    if ( infos.containsKey(info) ) {
                        try {
                            StatisticValue value = new StatisticValue(stat.getKey() + SEPARATOR + info, System.currentTimeMillis(), Long.parseLong(infos.get(info)));
                            em.persist(value);
                            LOGGER.log(Level.FINEST, "value persisted for stat name: " + value.getName());
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "unable to persist value", e);
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "unable to probe service: " + stat.getKey() + " for info: " + info);
                    }
                }
            } catch ( OrtolangException e ) {
                LOGGER.log(Level.WARNING, "unable to probe some stats", e);
            }
        }
    }

    @Override
    @Schedule(hour="2")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void probePiwik() throws StatisticsServiceException, OrtolangException {
        LOGGER.log(Level.INFO, "Probing Piwik stats for fresh values");
        List<String> workspaces = em.createNamedQuery("listAllWorkspaceAlias", String.class).getResultList();
        TypedQuery<Long> countWorkspaceValues = em.createNamedQuery("countWorkspaceValues", Long.class);
        if (countWorkspaceValues.getSingleResult() == 0L) {
            if (piwikAllCollectorThread != null && piwikAllCollectorThread.isAlive()) {
                LOGGER.log(Level.WARNING, "Already collecting all Piwik statistics");
                return;
            }
            LOGGER.log(Level.INFO, "Starting to collect all Piwik statistics");
            PiwikAllCollector piwikAllCollector = new PiwikAllCollector(workspaces);
            piwikAllCollectorThread = managedThreadFactory.newThread(piwikAllCollector);
            piwikAllCollectorThread.setName("All Piwik Statistics Collector Thread");
            piwikAllCollectorThread.start();
        } else {
            if (piwikLatestCollectorThread != null && piwikLatestCollectorThread.isAlive()) {
                LOGGER.log(Level.WARNING, "Already collecting latest Piwik statistics");
                return;
            }
            LOGGER.log(Level.INFO, "Starting to collect latest Piwik statistics");
            PiwikLatestCollector piwikLatestCollector = new PiwikLatestCollector(workspaces);
            piwikLatestCollectorThread = managedThreadFactory.newThread(piwikLatestCollector);
            piwikLatestCollectorThread.setName("Latest Piwik Statistics Collector Thread");
            piwikLatestCollectorThread.start();
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void storeWorkspaceStatisticValue(WorkspaceStatisticValue value) throws StatisticsServiceException {
        boolean alreadyExist;
        WorkspaceStatisticValue latestStoredValue = null;
        try {
            latestStoredValue = readWorkspaceValue(value.getName());
            alreadyExist = latestStoredValue.getTimestamp() == value.getTimestamp();
        } catch (StatisticNameNotFoundException e) {
            alreadyExist = false;
        }
        if (alreadyExist) {
            latestStoredValue.copy(latestStoredValue);
            em.merge(latestStoredValue);
        } else if ((latestStoredValue != null && latestStoredValue.getTimestamp() < value.getTimestamp()) || !value.isEmpty()) {
            em.persist(value);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long[] read(String name) throws StatisticsServiceException, StatisticNameNotFoundException {
        LOGGER.log(Level.FINEST, "reading stat value for name : " + name);
        TypedQuery<StatisticValue> query = em.createNamedQuery("findValuesForName", StatisticValue.class).setParameter("name", name).setMaxResults(1);
        StatisticValue v = query.getSingleResult();
        if ( v == null ) {
            throw new StatisticNameNotFoundException("unable to find a value for stat with name: " + name);
        } else {
            long[] value = new long[2];
            value[0] = v.getTimestamp();
            value[1] = v.getValue();
            return value;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long[][] history(String name, long from, long to) throws StatisticsServiceException, StatisticNameNotFoundException {
        LOGGER.log(Level.FINEST, "reading stat history for name : " + name);
        TypedQuery<StatisticValue> query = em.createNamedQuery("findValuesForNameFromTo", StatisticValue.class).setParameter("name", name).setParameter("from", from).setParameter("to", to);
        List<StatisticValue> values = query.getResultList();
        if ( values == null ) {
            throw new StatisticNameNotFoundException("unable to find values for stat with name: " + name);
        } else {
            long[][] result = new long[values.size()][2];
            int cpt = 0;
            for ( StatisticValue value : values ) {
                result[cpt][0] = value.getTimestamp();
                result[cpt][1] = value.getValue();
                cpt++;
            }
            return result;
        }
    }

    @Override
    public WorkspaceStatisticValue readWorkspaceValue(String alias) throws StatisticNameNotFoundException {
        TypedQuery<WorkspaceStatisticValue> query = em.createNamedQuery("findWorkspaceValues", WorkspaceStatisticValue.class).setParameter("name", alias).setMaxResults(1);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            throw new StatisticNameNotFoundException("unable to find a value for workspace stat with alias: " + alias);
        }
    }

    @Override
    public List<WorkspaceStatisticValue> workspaceHistory(String alias, long from, long to) throws StatisticNameNotFoundException {
        TypedQuery<WorkspaceStatisticValue> query = em.createNamedQuery("findWorkspaceValuesFromTo", WorkspaceStatisticValue.class).setParameter("name", alias).setParameter("from", from).setParameter("to", to);
        try {
            return query.getResultList();
        } catch (NoResultException e) {
            throw new StatisticNameNotFoundException("unable to find a value for workspace stat with alias: " + alias);
        }
    }

    @Override
    public WorkspaceStatisticValue sumWorkspaceHistory(String alias, long from, long to) throws StatisticNameNotFoundException {
        TypedQuery<WorkspaceStatisticValue> query = em.createNamedQuery("sumWorkspaceValuesFromTo", WorkspaceStatisticValue.class).setParameter("name", alias).setParameter("from", from).setParameter("to", to);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            throw new StatisticNameNotFoundException("unable to find a value for workspace stat with alias: " + alias);
        }
    }

    //Service methods

    @Override
    public String getServiceName() {
        return RegistryService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return new HashMap<>();
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
