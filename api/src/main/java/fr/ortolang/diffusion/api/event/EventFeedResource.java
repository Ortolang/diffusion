package fr.ortolang.diffusion.api.event;

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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.annotations.GZIP;

import fr.ortolang.diffusion.OrtolangEvent;
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
    @GZIP
    public Response getEventFeed(@PathParam(value = "key") String key, @QueryParam(value = "id") Long id, @QueryParam(value = "o") @DefaultValue(value = "0") int offset, @QueryParam(value = "l") @DefaultValue(value = "25") int limit, @Context Request request) throws KeyNotFoundException, AccessDeniedException, EventServiceException {
        LOGGER.log(Level.INFO, "GET /feeds/" + key);
        EventFeed feed = events.readEventFeed(key);
        List<OrtolangEvent> content;
        if ( id == null || id == 0 ) {
            content = events.browseEventFeed(key, offset, limit);
        } else {
            content = events.browseEventFeedSinceEvent(key, id);
        }
        EventFeedRepresentation representation = EventFeedRepresentation.fromEventFeed(feed);
        representation.setEvents(content);
        return Response.ok(representation).build();
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    public Response updateEventFeed(@PathParam(value = "key") String key, EventFeedRepresentation representation) throws EventServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.INFO, "PUT /feeds/" + key);
        events.updateEventFeed(key, representation.getName(), representation.getDescription());
        EventFeed feed = events.readEventFeed(key);
        EventFeedRepresentation newrepresentation = EventFeedRepresentation.fromEventFeed(feed);
        return Response.ok(newrepresentation).build();
    }
    
    @GET
    @Path("/{key}/filters")
    @GZIP
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
