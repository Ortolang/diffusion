package fr.ortolang.diffusion.api.statistics;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import fr.ortolang.diffusion.statistics.StatisticNameNotFoundException;
import fr.ortolang.diffusion.statistics.StatisticsService;
import fr.ortolang.diffusion.statistics.StatisticsServiceException;

@Path("/stats")
@Produces({ MediaType.APPLICATION_JSON })
public class StatisticsResource {
    
    private static final Logger LOGGER = Logger.getLogger(StatisticsResource.class.getName());
    
    @EJB
    private StatisticsService stats;
    
    @GET
    public List<String> getNames() throws StatisticsServiceException {
        LOGGER.log(Level.INFO, "GET /stats");
        return stats.list();
    }
    
    @GET
    @Path("/{name}")
    public StatisticsRepresentation getValue(@PathParam("name") String name, @QueryParam("from") @DefaultValue("0") String from, @QueryParam("to") @DefaultValue("5000000000000") String to)
            throws NumberFormatException, StatisticsServiceException, StatisticNameNotFoundException {
        LOGGER.log(Level.INFO, "GET /stats/" + name);
        StatisticsRepresentation representation = new StatisticsRepresentation();
        representation.setKey(name);;
        representation.setValues(stats.history(name, Long.parseLong(from), Long.parseLong(to)));
        return representation;
    }

}
