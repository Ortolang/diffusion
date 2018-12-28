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

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.statistics.entity.WorkspaceStatisticValue;
import org.apache.http.HttpResponse;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.piwik.java.tracking.PiwikRequest;

import javax.ejb.EJB;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class PiwikCollector implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(PiwikCollector.class.getName());

    Integer siteId;

    String authToken;

    String host;

    List<String> workspaces;

    private StatisticsService statistics;

    PiwikCollector(List<String> workspaces) throws OrtolangException {
        try {
            statistics = (StatisticsService) OrtolangServiceLocator.findService(StatisticsService.SERVICE_NAME);
        } catch (Exception e) {
            throw new OrtolangException(e);
        }

        this.workspaces = workspaces;
        String siteIdString = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_SITE_ID);
        host = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_HOST_FULL);
        authToken = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_AUTH_TOKEN);
        if (siteIdString == null || siteIdString.isEmpty() || host == null || host.isEmpty() || authToken == null || authToken.isEmpty()) {
            throw new OrtolangException("Missing configuration values for collecting Piwik Statistics");
        }
        this.siteId = Integer.parseInt(siteIdString);
    }

    PiwikRequest makePiwikRequestForDownloads(Integer siteId, String authToken, String alias, String range) {
        return makePiwikRequest(siteId, authToken, alias, range, true, false);
    }

    PiwikRequest makePiwikRequestForSingleDownloads(Integer siteId, String authToken, String alias, String range) {
        return makePiwikRequest(siteId, authToken, alias, range, true, true);
    }

    PiwikRequest makePiwikRequestForViews(Integer siteId, String authToken, String alias, String range) {
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
            request.setCustomTrackingParameter("filter_pattern_recursive", "^/market/(?!item)\\w*/" + alias + "($|/.*$)");
        }
        request.setAuthToken(authToken);
        LOGGER.log(Level.FINEST,"url sended " + request.getUrlEncodedQueryString());
        return request;
    }

    void compileResults(String alias, long timestamp, HttpResponse viewsResponse, HttpResponse downloadsResponse, HttpResponse singleDownloadsResponse) throws IOException {
        try {
            if (viewsResponse.getStatusLine().getStatusCode() != 200 || downloadsResponse.getStatusLine().getStatusCode() != 200 || singleDownloadsResponse.getStatusLine().getStatusCode() != 200) {
                LOGGER.log(Level.SEVERE, "Piwik response status code not OK: " + viewsResponse.getStatusLine().getReasonPhrase() + ", " + downloadsResponse.getStatusLine().getReasonPhrase() + ", " + singleDownloadsResponse.getStatusLine().getReasonPhrase());
                return;
            }
            WorkspaceStatisticValue statisticValue = new WorkspaceStatisticValue(alias, timestamp);
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

            statistics.storeWorkspaceStatisticValue(statisticValue);

            try {
                viewsReader.close();
                downloadsReader.close();
                singleDownloadsReader.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close buffered reader while compiling piwik results: " + e.getMessage(), e);
            }
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Cannot read Piwik stats for workspace '" + alias + "' : " + e.getMessage());
        } catch (StatisticsServiceException e) {
            LOGGER.log(Level.WARNING, "Cannot store Piwik stats for workspace '" + alias + "' : " + e.getMessage());
        }
    }
}
