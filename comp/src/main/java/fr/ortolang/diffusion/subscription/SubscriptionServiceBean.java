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

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Local(SubscriptionService.class)
@Singleton(name = SubscriptionService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class SubscriptionServiceBean implements SubscriptionService {

    private static final Logger LOGGER = Logger.getLogger(SubscriptionServiceBean.class.getName());

    private Map<String, Subscription> subscriptionRegistry = new HashMap<>();

    public SubscriptionServiceBean() {
        LOGGER.log(Level.INFO, "Instantiating service");
    }

    @Override
    public Broadcaster getBroadcaster(String username) {
        return subscriptionRegistry.get(username).getBroadcaster();
    }

    @Override
    public void registerBroadcaster(String username, AtmosphereResource atmosphereResource) {
        LOGGER.log(Level.INFO, "Registering a broadcaster for user " + username);
        if (!subscriptionRegistry.containsKey(username)) {
            subscriptionRegistry.put(username, new Subscription(atmosphereResource.getBroadcaster()));
        }
    }

    @Override
    public void addFilter(String username, Filter filter) {
        LOGGER.log(Level.INFO, "Adding filter to user " + username + " subscription " + filter);
        if (!subscriptionRegistry.get(username).getFilters().add(filter)) {
            LOGGER.log(Level.FINE, "Filter already present");
        }
    }

    @Override
    public void removeFilter(String username, Filter filter) {
        LOGGER.log(Level.INFO, "Removing filter from the subscription of user " + username);
        if (subscriptionRegistry.get(username).getFilters().remove(filter)) {
            LOGGER.log(Level.FINE, "Filter removed from user " + username + " subscription " + filter);
        }
    }

    @Override
    public Map<String, Subscription> getSubscriptions() {
        return subscriptionRegistry;
    }

}
