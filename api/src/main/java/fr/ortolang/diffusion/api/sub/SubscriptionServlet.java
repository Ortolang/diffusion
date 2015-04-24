package fr.ortolang.diffusion.api.sub;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.security.authentication.TicketHelper;
import fr.ortolang.diffusion.subscription.EventMessage;
import fr.ortolang.diffusion.subscription.Filter;
import fr.ortolang.diffusion.subscription.SubscriptionService;
import org.atmosphere.config.service.*;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedService(path = "/sub/{username}")
public final class SubscriptionServlet {

    private static final Logger LOGGER = Logger.getLogger(SubscriptionServlet.class.getName());

    @PathParam("username")
    private String username;

    private SubscriptionService subscription;

    /**
     * Invoked when the connection as been fully established and suspended, e.g ready for receiving messages.
     *
     * @param atmosphereResource the atmosphere resource
     */
    @Ready
    public final void onReady(final AtmosphereResource atmosphereResource) {
        String t = atmosphereResource.getRequest().getParameter("t");
        TicketHelper.Ticket ticket = null;
        if (t != null) {
            ticket = TicketHelper.decodeTicket(t);
        }
        if (ticket == null || !ticket.getUsername().equals(username)) {
            try {
                LOGGER.log(Level.SEVERE, "Wrong ticket. Closing Atmosphere Resource");
                atmosphereResource.close();
                return;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close Atmosphere Resource", e);
            }
        }
        getSubscription().registerBroadcaster(username, atmosphereResource);
        LOGGER.log(Level.INFO, "User " + username + " connected (browser UUID: " +  atmosphereResource.uuid() + ")");
    }

    /**
     * Invoked when the client disconnect or when an unexpected closing of the underlying connection happens.
     *
     * @param event the event
     */
    @Disconnect
    public final void onDisconnect(final AtmosphereResourceEvent event) {
        if(event.isCancelled()) {
            LOGGER.log(Level.INFO, "User " + username + " unexpectedly disconnected (browser UUID: " + event.getResource().uuid() + ")");
        } else if(event.isClosedByClient()) {
            LOGGER.log(Level.INFO, "User " + username + " closed the connection (browser UUID: " + event.getResource().uuid() + ")");
        }
    }

    @Message(encoders = {EventMessageEncoder.class})
    public final EventMessage onEventMessage(final EventMessage eventMessage) throws IOException {
        LOGGER.log(Level.INFO, "Sending event message");
        return eventMessage;
    }

    @Message(encoders = {EventEncoder.class})
    public final Event onEvent(final Event event) throws IOException {
        LOGGER.log(Level.INFO, "Sending event message");
        return event;
    }

    @Message(decoders = {FilterDecoder.class})
    @DeliverTo(DeliverTo.DELIVER_TO.RESOURCE)
    public Filter onAddingFilter(Filter filter) {
        subscription.addFilter("root", filter);
        return filter;
    }

    private SubscriptionService getSubscription() {
        if (subscription == null) {
            try {
                subscription = (SubscriptionService) OrtolangServiceLocator.lookup(SubscriptionService.SERVICE_NAME);
            } catch (OrtolangException e) {
            }
        }
        return subscription;
    }

}
