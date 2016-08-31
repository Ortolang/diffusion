package fr.ortolang.diffusion.api.statistics;

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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;

import fr.ortolang.diffusion.statistics.StatisticNameNotFoundException;
import fr.ortolang.diffusion.statistics.StatisticsService;
import fr.ortolang.diffusion.statistics.StatisticsServiceException;
import org.jboss.resteasy.annotations.GZIP;

@Path("/stats")
@Produces({ MediaType.APPLICATION_JSON })
public class StatisticsResource {

    private static final Logger LOGGER = Logger.getLogger(StatisticsResource.class.getName());

    @EJB
    private StatisticsService stats;

    @GET
    @GZIP
    public List<String> getNames() throws StatisticsServiceException {
        LOGGER.log(Level.INFO, "GET /stats");
        return stats.list();
    }

    @GET
    @Path("/{names}")
    @GZIP
    public Response getValue(@PathParam("names") String names, @QueryParam("from") @DefaultValue("0") String from, @QueryParam("to") @DefaultValue("5000000000000") String to, @QueryParam("cc") @DefaultValue("true") boolean cache, @Context Request request)
            throws NumberFormatException, StatisticsServiceException, StatisticNameNotFoundException {
        LOGGER.log(Level.INFO, "GET /stats/" + names);
        Map<String, StatisticsRepresentation> representations = new HashMap<>();
        for (String name : names.split(",")) {
            StatisticsRepresentation representation = new StatisticsRepresentation();
            representation.setKey(name);
            representation.setValues(stats.history(name, Long.parseLong(from), Long.parseLong(to)));
            representations.put(name, representation);
        }
        Response.ResponseBuilder builder = Response.ok(representations);
        if (cache) {
            CacheControl cc = new CacheControl();
            cc.setPrivate(true);
            cc.setMaxAge(3600);
            builder.cacheControl(cc);
        }
        return builder.build();
    }

}
