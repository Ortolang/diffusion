package fr.ortolang.diffusion.subscription;

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
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.security.authentication.TicketHelper;
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
        boolean close = false;
        if (ticket == null || !ticket.getUsername().equals(username)) {
            LOGGER.log(Level.FINE, "Wrong ticket. Closing Atmosphere Resource");
            close = true;
        } else {
            try {
                getSubscription().registerBroadcaster(username, atmosphereResource);
                LOGGER.log(Level.INFO, "User " + username + " connected (browser UUID: " +  atmosphereResource.uuid() + ")");
            } catch (SubscriptionServiceException e) {
                LOGGER.log(Level.SEVERE, "An unexpected exception occurred while registering the broadcaster. Closing Atmosphere Resource", e);
                close = true;
            }
        }
        if (close) {
            try {
                atmosphereResource.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close Atmosphere Resource", e);
            }
        }
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
    public final EventMessage onEventMessage(final Event event) throws IOException {
        LOGGER.log(Level.FINEST, "Sending event message");
        EventMessage eventMessage = new EventMessage();
        return eventMessage.fromEvent(event);
    }

    private SubscriptionService getSubscription() {
        if (subscription == null) {
            try {
                subscription = (SubscriptionService) OrtolangServiceLocator.lookup(SubscriptionService.SERVICE_NAME, SubscriptionService.class);
            } catch (OrtolangException e) {
                LOGGER.log(Level.SEVERE, "unable to inject SubscriptionService", e);
            }
        }
        return subscription;
    }

}
