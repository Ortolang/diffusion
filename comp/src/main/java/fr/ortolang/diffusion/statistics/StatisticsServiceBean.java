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
import org.apache.http.HttpResponse;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.piwik.java.tracking.PiwikRequest;
import org.piwik.java.tracking.PiwikTracker;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
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
    private static final Map<String, List<String>> STATS_NAMES = new HashMap<String, List<String>> ();

    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @Resource
    private SessionContext ctx;

    public StatisticsServiceBean() {
    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEntityManager() {
        return this.em;
    }

    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext() {
        return this.ctx;
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
        List<String> names = new ArrayList<String> ();
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
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void probePiwik() throws StatisticsServiceException {
        LOGGER.log(Level.INFO, "Probing Piwik stats for fresh values");
        try {
            String siteIdString = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_SITE_ID);
            String host = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_HOST_FULL);
            String authToken = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_AUTH_TOKEN);
            if (siteIdString == null || siteIdString.isEmpty() || host == null || host.isEmpty() || authToken == null || authToken.isEmpty()) {
                LOGGER.log(Level.INFO, "Do not attempt to probe Piwik stats, missing configuration values");
                return;
            }
            Integer siteId = Integer.parseInt(siteIdString);
            PiwikTracker tracker = new PiwikTracker(host + "index.php");

            List<String> aliasList = (List<String>) em.createNamedQuery("listAllWorkspaceAlias").getResultList();
            TypedQuery<Long> countWorkspaceValues = em.createNamedQuery("countWorkspaceValues", Long.class);
            if (countWorkspaceValues.getSingleResult() == 0L) {
                getPreviousStatistics(siteId, authToken, aliasList, tracker);
                return;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
            String range = dateFormat.format(calendar.getTime()) + "-01,yesterday";
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyyMM");
            long timestamp = Long.parseLong(monthFormat.format(new Date()));

            for (String alias : aliasList) {
                probeWorkspaceStats(siteId, authToken, alias, range, timestamp, tracker);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new StatisticsServiceException("Could not probe Piwik stats", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void probeWorkspaceStats(Integer siteId, String authToken, String alias, String range, long timestamp, PiwikTracker tracker) {
        try {
            // Views
            PiwikRequest request = makePiwikRequestForViews(siteId, authToken, alias, range);
            HttpResponse viewsResponse = tracker.sendRequest(request);
            // Downloads
            request = makePiwikRequestForDownloads(siteId, authToken, alias, range);
            HttpResponse downloadsResponse = tracker.sendRequest(request);
            // Single Downloads
            request = makePiwikRequestForSingleDownloads(siteId, authToken, alias, range);
            HttpResponse singleDownloadsResponse = tracker.sendRequest(request);
            compileResults(alias, timestamp, viewsResponse, downloadsResponse, singleDownloadsResponse);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not probe Piwik stats for workspace with alias ["  + alias + "]: " + e.getMessage(), e);
        }
    }


    private PiwikRequest makePiwikRequestForDownloads(Integer siteId, String authToken, String alias, String range) {
        return makePiwikRequest(siteId, authToken, alias, range, true, false);
    }

    private PiwikRequest makePiwikRequestForSingleDownloads(Integer siteId, String authToken, String alias, String range) {
        return makePiwikRequest(siteId, authToken, alias, range, true, true);
    }

    private PiwikRequest makePiwikRequestForViews(Integer siteId, String authToken, String alias, String range) {
        return makePiwikRequest(siteId, authToken, alias, range, false, null);
    }

    private PiwikRequest makePiwikRequest(Integer siteId, String authToken, String alias, String range, boolean downloads, Boolean single) {
        PiwikRequest request = new PiwikRequest(siteId, null);
        request.setCustomTrackingParameter("module", "API");
        request.setCustomTrackingParameter("idSite", siteId);
        if (downloads) {
            request.setCustomTrackingParameter("method", "Actions.getDownloads");
        } else {
            request.setCustomTrackingParameter("method", "Actions.getPageUrls");
        }
        if (range != null) {
            request.setCustomTrackingParameter("date", range);
            request.setCustomTrackingParameter("period", "range");
        } else {
            request.setCustomTrackingParameter("date", "yesterday");
            request.setCustomTrackingParameter("period", "day");
        }
        request.setCustomTrackingParameter("format", "JSON");
        request.setCustomTrackingParameter("expanded", "1");
        request.setCustomTrackingParameter("flat", "1");
        if (downloads) {
            request.setCustomTrackingParameter("filter_pattern_recursive", "api/content/" + (single ? "" : "export/") + alias + "/");
        } else {
            // Get stats for all the different versions and not if url /market/item/.*
            request.setCustomTrackingParameter("filter_pattern_recursive", "^market/(?!item)\\w*/" + alias + "($|/.*$)");
        }
        request.setAuthToken(authToken);
        return request;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void compileResults(String alias, long timestamp, HttpResponse viewsResponse, HttpResponse downloadsResponse, HttpResponse singleDownloadsResponse) throws IOException {
        try {
            if (viewsResponse.getStatusLine().getStatusCode() != 200 || downloadsResponse.getStatusLine().getStatusCode() != 200 || singleDownloadsResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.log(Level.SEVERE, "Piwik response status code not OK: " + viewsResponse.getStatusLine().getReasonPhrase() + ", " + downloadsResponse.getStatusLine().getReasonPhrase() + ", " + singleDownloadsResponse.getStatusLine().getReasonPhrase());
                return;
            }
            WorkspaceStatisticValue lastStatisticValue;
            WorkspaceStatisticValue statisticValue;
            try {
                lastStatisticValue = readWorkspaceValue(alias);
            } catch (StatisticNameNotFoundException e) {
                lastStatisticValue = null;
            }
            boolean alreadyExist = lastStatisticValue != null && lastStatisticValue.getTimestamp() == timestamp;
            if (alreadyExist) {
                statisticValue = lastStatisticValue;
            } else {
                statisticValue = new WorkspaceStatisticValue(alias, timestamp);
            }
            // Views
            BufferedReader viewsReader = new BufferedReader(new InputStreamReader(viewsResponse.getEntity().getContent()));
            String viewsContent = viewsReader.readLine();
            JSONArray viewsResults = new JSONArray(viewsContent);
            for (int i = 0; i < viewsResults.length(); i++) {
                JSONObject result = viewsResults.getJSONObject(i);
                if (!result.has("url")) {
                    continue;
                }
                if (result.has("nb_visits")) {
                    statisticValue.addVisits(result.getLong("nb_visits"));
                }
                if (result.has("nb_uniq_visitors")) {
                    statisticValue.addUniqueVisitors(result.getLong("nb_uniq_visitors"));
                }
                if (result.has("nb_hits")) {
                    statisticValue.addHits(result.getLong("nb_hits"));
                }
            }
            // Downloads
            BufferedReader downloadsReader = new BufferedReader(new InputStreamReader(downloadsResponse.getEntity().getContent()));
            String downloadsContent = downloadsReader.readLine();
            JSONArray downloadsResults = new JSONArray(downloadsContent);
            for (int i = 0; i < downloadsResults.length(); i++) {
                JSONObject result = downloadsResults.getJSONObject(i);
                if (!result.has("url")) {
                    continue;
                }
                if (result.has("nb_hits")) {
                    statisticValue.addDownloads(result.getLong("nb_hits"));
                }
            }
            // Single Downloads
            BufferedReader singleDownloadsReader = new BufferedReader(new InputStreamReader(singleDownloadsResponse.getEntity().getContent()));
            String singleDownloadsContent = singleDownloadsReader.readLine();
            JSONArray singleDownloadsResults = new JSONArray(singleDownloadsContent);
            for (int i = 0; i < singleDownloadsResults.length(); i++) {
                JSONObject result = singleDownloadsResults.getJSONObject(i);
                if (!result.has("url")) {
                    continue;
                }
                if (result.has("nb_hits")) {
                    statisticValue.addSingleDownloads(result.getLong("nb_hits"));
                }
            }

            if (!statisticValue.isEmpty() && !alreadyExist) {
                em.persist(statisticValue);
            }
            try {
                viewsReader.close();
                downloadsReader.close();
                singleDownloadsReader.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close buffered reader while compiling piwik results: " + e.getMessage(), e);
            }
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Cannot read Piwik stats for workspace '" + alias + "' : " + e.getMessage());
            ctx.setRollbackOnly();
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

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void getPreviousStatistics(Integer siteId, String authToken, List<String> aliasList, PiwikTracker tracker) throws IOException {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);
        for (int year = 2015; year <= currentYear; year++) {
            for (int month = 0; year == currentYear ? month <= currentMonth: month <= 11; month++) {
                calendar.set(year, month, 1);
                String start = dateFormat.format(calendar.getTime());
                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.DATE, -1);
                String end = dateFormat.format(calendar.getTime());
                String range = start + "," + end;
                SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMM");
                long timestamp = Long.parseLong(timestampFormat.format(calendar.getTime()));
                for (String alias : aliasList) {
                    LOGGER.log(Level.INFO, alias + ":" + start + "," + end + " [" + timestamp + "]");
                    // Views
                    PiwikRequest request = makePiwikRequestForViews(siteId, authToken, alias, range);
                    HttpResponse viewsResponse = tracker.sendRequest(request);
                    // Downloads
                    request = makePiwikRequestForDownloads(siteId, authToken, alias, range);
                    HttpResponse downloadsResponse = tracker.sendRequest(request);
                    // Single Downloads
                    request = makePiwikRequestForSingleDownloads(siteId, authToken, alias, range);
                    HttpResponse singleDownloadsResponse = tracker.sendRequest(request);
                    compileResults(alias, timestamp, viewsResponse, downloadsResponse, singleDownloadsResponse);
                }
            }
        }
    }

    //Service methods

    @Override
    public String getServiceName() {
        return RegistryService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return new HashMap<String, String> ();
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
