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

import fr.ortolang.diffusion.OrtolangException;
import org.apache.http.HttpResponse;
import org.piwik.java.tracking.PiwikRequest;
import org.piwik.java.tracking.PiwikTracker;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class PiwikLatestCollector extends PiwikCollector {

    private static final Logger LOGGER = Logger.getLogger(PiwikLatestCollector.class.getName());

    PiwikLatestCollector(List<String> workspaces) throws OrtolangException {
        super(workspaces);
    }

    @Override
    public void run() {
        PiwikTracker tracker = new PiwikTracker(host + "index.php");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
        String range = dateFormat.format(calendar.getTime()) + "-01,yesterday";
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyyMM");
        long timestamp = Long.parseLong(monthFormat.format(new Date()));

        for (String alias : workspaces) {
            try {
                LOGGER.log(Level.INFO, "Collect stats for workspace with alias [" + alias + "] (range: " + range + ")");
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
    }
}
