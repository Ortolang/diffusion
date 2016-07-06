package fr.ortolang.diffusion.subscription;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 * *
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
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.runtime.entity.Process;
import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(name = "AtmosphereMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/notification"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue="eventtype LIKE 'core.%' OR eventtype LIKE 'runtime.process.%' OR eventtype LIKE 'runtime.task.%' OR eventtype LIKE 'runtime.remote.%' OR eventtype LIKE 'membership.group.%' OR eventtype LIKE 'message.%' OR eventtype LIKE 'job.%'")})
@SecurityDomain("ortolang")
public class AtmosphereListenerBean implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(AtmosphereListenerBean.class.getName());

    private static final String PROCESS_CHANGE_STATE_TYPE = "runtime.process.change-state";
    private static final String PROCESS_CREATE_TYPE = "runtime.process.create";
    private static final String MEMBERSHIP_GROUP_ADD_MEMBER_TYPE = "membership.group.add-member";
    private static final String WORKSPACE_CREATE_TYPE = "core.workspace.create";
    private static final String WORKSPACE_DELETE_TYPE = "core.workspace.delete";

    @EJB
    SubscriptionService subscription;

    @Override
    @PermitAll
    public void onMessage(Message message) {
        try {
            Event event = new Event();
            event.fromJMSMessage(message);

            switch (event.getType()) {
            case PROCESS_CREATE_TYPE:
                if (subscription.getSubscriptions().containsKey(event.getThrowedBy())) {
                    LOGGER.log(Level.FINE, "Process created by user " + event.getThrowedBy() + "; adding filter to follow process events");
                    subscription.getSubscriptions().get(event.getThrowedBy()).addFilter(new Filter(SubscriptionService.RUNTIME_PROCESS_PATTERN, event.getFromObject(), null));
                }
                break;
            case MEMBERSHIP_GROUP_ADD_MEMBER_TYPE:
                Map<String, String> arguments = event.getArguments();
                if (arguments.containsKey("member") && subscription.getSubscriptions().containsKey(arguments.get("member"))) {
                    LOGGER.log(Level.FINE, "User " + arguments.get("member") + " added to group " + event.getFromObject() + "; adding filter to follow group events");
                    subscription.getSubscriptions().get(arguments.get("member")).addFilter(new Filter(SubscriptionService.MEMBERSHIP_GROUP_ALL_PATTERN, event.getFromObject(), null));
                }
                break;
            case WORKSPACE_CREATE_TYPE:
                if (subscription.getSubscriptions().containsKey(event.getThrowedBy())) {
                    LOGGER.log(Level.FINE, "Workspace created by user " + event.getThrowedBy() + "; adding filter to follow workspace events");
                    subscription.getSubscriptions().get(event.getThrowedBy()).addFilter(new Filter(null, event.getFromObject(), null));
                }
                break;
            }

            for (Map.Entry<String, Subscription> subscriptionRegistryEntry : subscription.getSubscriptions().entrySet()) {
                Set<Filter> filters = new HashSet<>(subscriptionRegistryEntry.getValue().getFilters());
                filters.stream().filter(filter -> filter.matches(event)).forEach(filter -> {
                    LOGGER.log(Level.FINE, "Matching filter " + filter);
                    LOGGER.log(Level.FINE, "Sending atmosphere message to " + subscriptionRegistryEntry.getKey());
                    subscriptionRegistryEntry.getValue().getBroadcaster().broadcast(event);
                    if (hasToBeRemoved(filter, event)) {
                        LOGGER.log(Level.INFO, "Removing filter from " + subscriptionRegistryEntry.getKey() + " subscription");
                        subscriptionRegistryEntry.getValue().removeFilter(filter);
                    }
                });
            }
        } catch (OrtolangException e) {
            LOGGER.log(Level.WARNING, "unable to process event", e);
        }
    }

    private boolean hasToBeRemoved(Filter filter, Event event) {
        if (event.getType().equals(PROCESS_CHANGE_STATE_TYPE)) {
            if (event.getArguments().containsKey("state") && event.getArguments().get("state").equals(Process.State.COMPLETED.name())) {
                return true;
            }
        } else if (event.getType().equals(WORKSPACE_DELETE_TYPE)) {
            // if from pattern equals null then the filter matches any workspace key
            // and thus is not specific to the deleted workspace
            if (filter.getFromPattern() != null) {
                return true;
            }
        }
        return false;
    }
}
