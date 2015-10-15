package fr.ortolang.diffusion.api.event;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.api.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.event.EventService;
import fr.ortolang.diffusion.event.EventServiceException;
import fr.ortolang.diffusion.event.entity.EventFeed;
import fr.ortolang.diffusion.event.entity.EventFeedFilter;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/feeds")
@Produces({ MediaType.APPLICATION_JSON })
public class EventFeedResource {

    private static final Logger LOGGER = Logger.getLogger(EventFeedResource.class.getName());

    @EJB
    private EventService events;
    @Context
    private UriInfo uriInfo;

    @GET
    @Path("/{key}")
    public Response getEventFeed(@PathParam(value = "key") String key, @Context Request request) throws KeyNotFoundException, AccessDeniedException, EventServiceException {
        LOGGER.log(Level.INFO, "GET /feeds/" + key);
        EventFeed feed = events.readEventFeed(key);
        EventFeedRepresentation representation = EventFeedRepresentation.fromEventFeed(feed);
        return Response.ok(representation).build();
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateEventFeed(@PathParam(value = "key") String key, EventFeedRepresentation representation) throws EventServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "PUT /feeds/" + key);
        events.updateEventFeed(key, representation.getName(), representation.getDescription());
        EventFeed feed = events.readEventFeed(key);
        EventFeedRepresentation newrepresentation = EventFeedRepresentation.fromEventFeed(feed);
        return Response.ok(newrepresentation).build();
    }
    
    @GET
    @Path("/{key}/filters")
    public Response listEventFeedFilters(@PathParam(value = "key") String key, @Context Request request) throws KeyNotFoundException, AccessDeniedException, EventServiceException {
        LOGGER.log(Level.INFO, "GET /feeds/" + key + "/filters");
        EventFeed feed = events.readEventFeed(key);
        GenericCollectionRepresentation<EventFeedFilterRepresentation> representation = new GenericCollectionRepresentation<EventFeedFilterRepresentation>();
        for ( EventFeedFilter filter : feed.getFilters() ) {
            representation.addEntry(EventFeedFilterRepresentation.fromEventFeedFilter(filter));
        }
        return Response.ok(representation).build();
    }
    
    @POST
    @Path("/{key}/filters")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addEventFeedFilter(@PathParam(value = "key") String key, EventFeedFilterRepresentation representation) throws KeyNotFoundException, AccessDeniedException, EventServiceException {
        LOGGER.log(Level.INFO, "POST /feeds/" + key + "/filters");
        events.addEventFeedFilter(key, representation.getEventType(), representation.getSourceKey(), representation.getObjectType(), representation.getThrowedBy());
        return Response.ok().build();
    }
    
    @DELETE
    @Path("/{key}/filters/{id}")
    public Response removeEventFeedFilter(@PathParam(value = "key") String key, @PathParam(value = "id") String id) throws KeyNotFoundException, AccessDeniedException, EventServiceException {
        LOGGER.log(Level.INFO, "DELETE /feeds/" + key + "/filters/" + id);
        events.removeEventFeedFilter(key, id);
        return Response.ok().build();
    }

}
