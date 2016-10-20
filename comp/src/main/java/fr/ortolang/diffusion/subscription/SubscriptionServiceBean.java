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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.atmosphere.cpr.AtmosphereResource;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.runtime.entity.HumanTask;
import fr.ortolang.diffusion.runtime.entity.Process;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Local(SubscriptionService.class)
@Singleton(name = SubscriptionService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class SubscriptionServiceBean implements SubscriptionService {

    private static final Logger LOGGER = Logger.getLogger(SubscriptionServiceBean.class.getName());

    private static final String PROCESS_CREATE_TYPE = "runtime.process.create";
    private static final String MEMBERSHIP_GROUP_ADD_MEMBER_TYPE = "membership.group.add-member";
    private static final String WORKSPACE_CREATE_TYPE = "core.workspace.create";

    @EJB
    private MembershipService membership;
    @EJB
    private RuntimeService runtime;
    @EJB
    private CoreService core;
    private Map<String, Subscription> registry = new HashMap<>();

    public SubscriptionServiceBean() {
        LOGGER.log(Level.INFO, "Instantiating Subscription service");
    }

    @Override
    public void registerBroadcaster(String username, AtmosphereResource atmosphereResource) {
        LOGGER.log(Level.INFO, "Registering a broadcaster for user " + username);
        if (!registry.containsKey(username)) {
            registry.put(username, new Subscription(atmosphereResource.getBroadcaster()));
        }
    }

    @Override
    public void addFilter(String username, Filter filter) throws SubscriptionServiceException {
        if (!registry.containsKey(username)) {
            throw new SubscriptionServiceException("Could not find subscription for user [" + username + "]");
        }
        if (!registry.get(username).addFilter(filter)) {
            LOGGER.log(Level.FINE, "Filter already present for user " + username + " " + filter);
        } else {
            LOGGER.log(Level.FINE, "Filter added to user " + username + " subscription " + filter);
        }
    }

    @Override
    public void processEvent(Event event) throws SubscriptionServiceException {
        switch (event.getType()) {
        case PROCESS_CREATE_TYPE:
            if (registry.containsKey(event.getThrowedBy())) {
                LOGGER.log(Level.FINE, "Process created by user " + event.getThrowedBy() + "; adding filter to follow process events");
                addFilter(event.getThrowedBy(), new Filter(SubscriptionService.RUNTIME_PROCESS_PATTERN, event.getFromObject(), null));
            }
            break;
        case MEMBERSHIP_GROUP_ADD_MEMBER_TYPE:
            Map<String, String> arguments = event.getArguments();
            if (arguments.containsKey("member") && registry.containsKey(arguments.get("member"))) {
                LOGGER.log(Level.FINE, "User " + arguments.get("member") + " added to group " + event.getFromObject() + "; adding filter to follow group events");
                addFilter(arguments.get("member"), new Filter(SubscriptionService.MEMBERSHIP_GROUP_ALL_PATTERN, event.getFromObject(), null));
            }
            break;
        case WORKSPACE_CREATE_TYPE:
            if (registry.containsKey(event.getThrowedBy())) {
                LOGGER.log(Level.FINE, "Workspace created by user " + event.getThrowedBy() + "; adding filter to follow workspace events");
                addFilter(event.getThrowedBy(), new Filter(null, event.getFromObject(), null));
            }
            break;
        }

        Map<String, Filter> toRemove = new HashMap<>();
        for (Map.Entry<String, Subscription> entry : registry.entrySet()) {
            entry.getValue().getFilters().stream().filter(filter -> filter.matches(event)).forEach(filter -> {

                LOGGER.log(Level.FINE, "Matching filter " + filter);
                LOGGER.log(Level.FINE, "Sending atmosphere message to " + entry.getKey());
                entry.getValue().broadcast(event);

                // Check if filter needs to be removed
                if (event.getType().equals("runtime.process.change-state")) {
                    if (event.getArguments().containsKey("state") && event.getArguments().get("state").equals(Process.State.COMPLETED.name())) {
                        toRemove.put(entry.getKey(), filter);
                    }
                } else if (event.getType().equals("core.workspace.delete")) {
                    // if from pattern equals null then the filter matches any workspace key
                    // and thus is not specific to the deleted workspace
                    if (filter.getFromPattern() != null) {
                        toRemove.put(entry.getKey(), filter);
                    }
                }
            });
        }

        for (Map.Entry<String, Filter> entry : toRemove.entrySet()) {
            LOGGER.log(Level.INFO, "Removing filter from " + entry.getKey() + " subscription");
            registry.get(entry.getKey()).removeFilter(entry.getValue());
        }
    }

    @Override
    public void addDefaultFilters() throws SubscriptionServiceException, RuntimeServiceException, AccessDeniedException {
        String username = membership.getProfileKeyForConnectedIdentifier();
        List<String> profileGroups = null;
        try {
            profileGroups = membership.getProfileGroups(username);
        } catch (MembershipServiceException | KeyNotFoundException | AccessDeniedException e) {
            LOGGER.log(Level.SEVERE, "Cannot read " + username + " profile and thus cannot add filters for user groups", e);
        }
        if (username != null) {
            LOGGER.log(Level.FINE, "Adding default filters to user " + username + " subscription");
            // Runtime events
            List<Process> processes = runtime.listCallerProcesses(null);
            for (Process process : processes) {
                if (!process.getState().equals(Process.State.ABORTED) && !process.getState().equals(Process.State.COMPLETED)) {
                    addFilter(username, new Filter(RUNTIME_PROCESS_PATTERN, process.getKey(), null));
                }
            }
            List<HumanTask> candidateTasks = runtime.listCandidateTasks();
            for (HumanTask candidateTask : candidateTasks) {
                addFilter(username, new Filter("runtime\\.task\\..*", candidateTask.getId(), null));
            }
            // User's workspaces related filters
            addWorkspacesFilters();
            // User's groups related filters
            if (profileGroups != null) {
                for (String profileGroup : profileGroups) {
                    if (profileGroup != null && profileGroup.length() > 0) {
                        addFilter(username, new Filter(MEMBERSHIP_GROUP_ALL_PATTERN, profileGroup, null));
                        addFilter(username, new Filter("runtime\\.task\\..*", null, null, "group," + profileGroup));
                    }
                }
            }
            addFilter(username, new Filter("runtime\\.remote\\.create", null, username));
        } else {
            throw new SubscriptionServiceException("Cannot get profile key for connected identifier");
        }
    }

    @Override
    public void addWorkspacesFilters() {
        String username = membership.getProfileKeyForConnectedIdentifier();
        try {
            List<String> workspaces = core.findWorkspacesForProfile(username);
            for (String workspace : workspaces) {
                addFilter(username, new Filter(null, workspace, null));
            }
        } catch (AccessDeniedException | CoreServiceException | SubscriptionServiceException e) {
            LOGGER.log(Level.SEVERE, "Cannot read " + username + " profile and thus cannot add filters for user groups", e);
        }
    }

    @Override
    @RolesAllowed("admin")
    public void addAdminFilters() throws SubscriptionServiceException {
        String username = membership.getProfileKeyForConnectedIdentifier();
        if (username != null) {
            LOGGER.log(Level.FINE, "Adding admin filters to user " + username + " subscription");
            // Core events
            addFilter(username, new Filter("core\\.workspace\\..*", null, null));
            // Membership events
            addFilter(username, new Filter("membership\\.group\\..*", null, null));
            // Runtime events
            addFilter(username, new Filter("runtime\\.process\\..*", null, null));
            addFilter(username, new Filter("runtime\\.task\\..*", null, null));
            addFilter(username, new Filter("runtime\\.remote\\..*", null, null));
            addFilter(username, new Filter("job\\.job\\..*", null, null));
        }
    }

    @Schedule(hour = "5")
    private void cleanupSubscriptions() {
        LOGGER.log(Level.INFO, "Starting to cleanup subscriptions (registry size: " + registry.size() + ")");
        Set<String> subscriptionsToBeRemoved = new HashSet<>();
        registry.entrySet().stream().filter(subscriptionRegistryEntry -> !subscriptionRegistryEntry.getValue().hasAtmosphereResources())
                .forEach(subscriptionRegistryEntry -> {
                    LOGGER.log(Level.INFO, "No more resources associated to " + subscriptionRegistryEntry.getKey() + " broadcaster; destroying broadcaster");
                    subscriptionsToBeRemoved.add(subscriptionRegistryEntry.getKey());
                });
        if (subscriptionsToBeRemoved.isEmpty()) {
            LOGGER.log(Level.INFO, "No subscription removed from subscription registry");
        } else {
            for (String key : subscriptionsToBeRemoved) {
                LOGGER.log(Level.FINE, "Removing subscription of user " + key);
                registry.get(key).destroy();
                registry.remove(key);
            }
            LOGGER.log(Level.INFO, subscriptionsToBeRemoved.size() + " subscription(s) removed from subscription registry (registry new size: " + registry.size() + ")");
        }
    }

    //Service methods

    @Override
    public String getServiceName() {
        return SubscriptionService.SERVICE_NAME;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> getServiceInfos() {
        return new HashMap<>();
    }

}
