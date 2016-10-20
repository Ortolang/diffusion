package fr.ortolang.diffusion.event;

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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.event.entity.EventFeed;
import fr.ortolang.diffusion.event.entity.EventFeedFilter;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

@Local(EventService.class)
@Stateless(name = EventService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class EventServiceBean implements EventService {

    private static final Logger LOGGER = Logger.getLogger(EventServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { EventFeed.OBJECT_TYPE };
    private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { { EventFeed.OBJECT_TYPE, "read,update,delete" } };

    @EJB
    private RegistryService registry;
    @EJB
    private MembershipService membership;
    @EJB
    private NotificationService notification;
    @EJB
    private AuthorisationService authorisation;
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @Resource
    private SessionContext ctx;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createEventFeed(String key, String name, String description) throws EventServiceException, AccessDeniedException, KeyAlreadyExistsException {
        LOGGER.log(Level.FINE, "creating event feed for key [" + key + "] and name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);

            EventFeed ef = new EventFeed();
            ef.setId(UUID.randomUUID().toString());
            ef.setName(name);
            ef.setDescription(description);
            em.persist(ef);

            registry.register(key, ef.getObjectIdentifier(), caller);

            authorisation.createPolicy(key, caller);

            notification.throwEvent(key, caller, EventFeed.OBJECT_TYPE, OrtolangEvent.buildEventType(EventService.SERVICE_NAME, EventFeed.OBJECT_TYPE, "create"));
        } catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException e) {
            ctx.setRollbackOnly();
            throw new EventServiceException("unable to create event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public EventFeed readEventFeed(String key) throws EventServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "reading event feed for key [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, EventFeed.OBJECT_TYPE);
            EventFeed feed = em.find(EventFeed.class, identifier.getId());
            if (feed == null) {
                throw new EventServiceException("unable to find an event feed for id " + identifier.getId());
            }
            feed.setKey(key);
            return feed;
        } catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
            throw new EventServiceException("unable to read the event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<OrtolangEvent> browseEventFeed(String key, int offset, int limit) throws EventServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "browsing event feed for key [" + key + "] from event [" + offset + "] and limit [" + limit + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, EventFeed.OBJECT_TYPE);
            EventFeed feed = em.find(EventFeed.class, identifier.getId());
            if (feed == null) {
                throw new EventServiceException("unable to find an event feed for id " + identifier.getId());
            }
            feed.setKey(key);

            int cpt = 0;
            int ioffset = 0;
            int ilimit = 5000;
            boolean endOfEvents = false;
            List<OrtolangEvent> oevents = new ArrayList<OrtolangEvent>();
            while (!endOfEvents) {
                List<Event> events = em.createNamedQuery("listAllEvents", Event.class).setFirstResult(ioffset).setMaxResults(ilimit).setLockMode(LockModeType.NONE).getResultList();
                if (events.size() < ilimit) {
                    LOGGER.log(Level.FINEST, "listAllEvents returned only " + events.size() + " events, seem that end is reached.");
                    endOfEvents = true;
                } else {
                    LOGGER.log(Level.FINEST, "listAllEvents returned " + ilimit + " results, setting offset to next segment.");
                    ioffset += ilimit;
                }
                for (Event event : events) {
                    for (EventFeedFilter filter : feed.getFilters()) {
                        if (filter.match(event)) {
                            try {
                                if (event.getFromObject() != null && event.getFromObject().length() > 0) {
                                    authorisation.checkPermission(event.getFromObject(), subjects, "read");
                                }
                                if (cpt >= offset) {
                                    oevents.add(event);
                                }
                                cpt++;
                            } catch (AccessDeniedException e) {
                                LOGGER.log(Level.FINEST, "no permission to read event with id: " + event.getId());
                            }
                            break;
                        }
                    }
                    if (cpt >= (offset + limit)) {
                        endOfEvents = true;
                        break;
                    }
                }
            }

            return oevents;
        } catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
            throw new EventServiceException("unable to browse the event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<OrtolangEvent> browseEventFeedSinceDate(String key, Date from) throws EventServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "browsing event feed for key [" + key + "] since date [" + from + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, EventFeed.OBJECT_TYPE);
            EventFeed feed = em.find(EventFeed.class, identifier.getId());
            if (feed == null) {
                throw new EventServiceException("unable to find an event feed for id " + identifier.getId());
            }
            feed.setKey(key);

            List<OrtolangEvent> oevents = new ArrayList<>();
            List<Event> events = em.createNamedQuery("listAllEventsFromDate", Event.class).setParameter("date", from).setLockMode(LockModeType.NONE).getResultList();
            for (Event event : events) {
                for (EventFeedFilter filter : feed.getFilters()) {
                    if (filter.match(event)) {
                        try {
                            if (event.getFromObject() != null && event.getFromObject().length() > 0) {
                                authorisation.checkPermission(event.getFromObject(), subjects, "read");
                            }
                            oevents.add(event);
                        } catch (AccessDeniedException e) {
                            LOGGER.log(Level.FINEST, "no permission to read event with id: " + event.getId());
                        }
                        break;
                    }
                }
            }

            return oevents;
        } catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
            throw new EventServiceException("unable to browse the event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<OrtolangEvent> browseEventFeedSinceEvent(String key, long id) throws EventServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "browsing event feed for key [" + key + "] since id [" + id + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, EventFeed.OBJECT_TYPE);
            EventFeed feed = em.find(EventFeed.class, identifier.getId());
            if (feed == null) {
                throw new EventServiceException("unable to find an event feed for id " + identifier.getId());
            }
            feed.setKey(key);

            List<OrtolangEvent> oevents = new ArrayList<OrtolangEvent>();
            List<Event> events = em.createNamedQuery("listAllEventsFromId", Event.class).setParameter("id", id).setLockMode(LockModeType.NONE).getResultList();
            for (Event event : events) {
                for (EventFeedFilter filter : feed.getFilters()) {
                    if (filter.match(event)) {
                        try {
                            if (event.getFromObject() != null && event.getFromObject().length() > 0) {
                                authorisation.checkPermission(event.getFromObject(), subjects, "read");
                            }
                            oevents.add(event);
                        } catch (AccessDeniedException e) {
                            LOGGER.log(Level.FINEST, "no permission to read event with id: " + event.getId());
                        }
                        break;
                    }
                }
            }

            return oevents;
        } catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
            throw new EventServiceException("unable to browse the event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateEventFeed(String key, String name, String description) throws EventServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "updating event feed for key [" + key + "] and name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, EventFeed.OBJECT_TYPE);
            EventFeed ef = em.find(EventFeed.class, identifier.getId());
            if (ef == null) {
                throw new EventServiceException("unable to find an event feed for id " + identifier.getId());
            }
            ef.setName(name);
            ef.setDescription(description);
            em.merge(ef);

            registry.update(key);

            notification.throwEvent(key, caller, EventFeed.OBJECT_TYPE, OrtolangEvent.buildEventType(EventService.SERVICE_NAME, EventFeed.OBJECT_TYPE, "update"));
        } catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | KeyLockedException e) {
            ctx.setRollbackOnly();
            throw new EventServiceException("unable to update event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteEventFeed(String key) throws EventServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "deleting event feed for key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "delete");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, EventFeed.OBJECT_TYPE);
            registry.delete(key);
            notification.throwEvent(key, caller, EventFeed.OBJECT_TYPE, OrtolangEvent.buildEventType(EventService.SERVICE_NAME, EventFeed.OBJECT_TYPE, "delete"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
            ctx.setRollbackOnly();
            throw new EventServiceException("unable to delete event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addEventFeedFilter(String key, String eventTypeRE, String fromObjectRE, String objectTypeRE, String throwedByRE) throws EventServiceException, AccessDeniedException,
            KeyNotFoundException {
        LOGGER.log(Level.FINE, "adding filter in event feed with key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, EventFeed.OBJECT_TYPE);
            EventFeed ef = em.find(EventFeed.class, identifier.getId());
            if (ef == null) {
                throw new EventServiceException("unable to find an event feed for id " + identifier.getId());
            }
            EventFeedFilter filter = new EventFeedFilter();
            filter.setId(UUID.randomUUID().toString());
            filter.setEventTypeRE(eventTypeRE);
            filter.setFromObjectRE(fromObjectRE);
            filter.setObjectTypeRE(objectTypeRE);
            filter.setThrowedByRE(throwedByRE);
            ef.addFilter(filter);
            em.merge(ef);

            registry.update(key);

            notification.throwEvent(key, caller, EventFeed.OBJECT_TYPE, OrtolangEvent.buildEventType(EventService.SERVICE_NAME, EventFeed.OBJECT_TYPE, "add-filter"));
        } catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | KeyLockedException e) {
            ctx.setRollbackOnly();
            throw new EventServiceException("unable to add filter in event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeEventFeedFilter(String key, String id) throws EventServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "removing filter in event feed with key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, EventFeed.OBJECT_TYPE);
            EventFeed ef = em.find(EventFeed.class, identifier.getId());
            if (ef == null) {
                throw new EventServiceException("unable to find an event feed for id " + identifier.getId());
            }
            for (EventFeedFilter filter : ef.getFilters()) {
                if (filter.getId().equals(id)) {
                    ef.removeFilter(filter);
                }
            }
            em.merge(ef);

            registry.update(key);

            notification.throwEvent(key, caller, EventFeed.OBJECT_TYPE, OrtolangEvent.buildEventType(EventService.SERVICE_NAME, EventFeed.OBJECT_TYPE, "remove-filter"));
        } catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | KeyLockedException e) {
            ctx.setRollbackOnly();
            throw new EventServiceException("unable to remove filter in event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void persistEvent(Event event) throws EventServiceException {
        try {
            em.persist(event);
        } catch (Exception e) {
            ctx.setRollbackOnly();
            throw new EventServiceException("unable to persist event", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<OrtolangEvent> findEvents(String eventTypeFilter, String fromResourceFilter, String resourceTypeFilter, String throwedByFilter, long after, int offset, int limit)
            throws EventServiceException {
        LOGGER.log(Level.FINE, "finding events");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            int cpt = 0;
            int ioffset = 0;
            int ilimit = 5000;
            boolean endOfEvents = false;
            List<OrtolangEvent> oevents = new ArrayList<OrtolangEvent>();
            while (!endOfEvents) {
                List<? extends OrtolangEvent> events = systemFindEvents(eventTypeFilter, fromResourceFilter, resourceTypeFilter, throwedByFilter, after, ioffset, ilimit);
                if (events.size() < ilimit) {
                    LOGGER.log(Level.FINEST, "systemFindEvents returned only " + events.size() + " events, seem that end is reached.");
                    endOfEvents = true;
                } else {
                    LOGGER.log(Level.FINEST, "systemFindEvents returned " + ilimit + " results, setting offset to next segment.");
                    ioffset += ilimit;
                }
                for (OrtolangEvent event : events) {
                    try {
                        if (event.getFromObject() != null && event.getFromObject().length() > 0) {
                            authorisation.checkPermission(event.getFromObject(), subjects, "read");
                        }
                        if (cpt >= offset) {
                            oevents.add(event);
                        }
                        cpt++;
                    } catch (AccessDeniedException e) {
                        //
                    }
                    if (cpt >= (offset + limit)) {
                        endOfEvents = true;
                        break;
                    }
                }
            }

            return oevents;
        } catch (AuthorisationServiceException | MembershipServiceException | KeyNotFoundException e) {
            throw new EventServiceException("unable to find events", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<? extends OrtolangEvent> systemFindEvents(String eventTypeFilter, String fromResourceFilter, String resourceTypeFilter, String throwedByFilter, long after, int offset, int limit)
            throws EventServiceException {
        LOGGER.log(Level.FINE, "##SYSTEM## finding events");
        String eventTypeFilterRE = "%";
        if (eventTypeFilter != null && eventTypeFilter.length() > 0) {
            eventTypeFilterRE += eventTypeFilter + "%";
        }
        String fromResourceFilterRE = "%";
        if (fromResourceFilter != null && fromResourceFilter.length() > 0) {
            fromResourceFilterRE += fromResourceFilter + "%";
        }
        String resourceTypeFilterRE = "%";
        if (resourceTypeFilter != null && resourceTypeFilter.length() > 0) {
            resourceTypeFilterRE += resourceTypeFilter + "%";
        }
        String throwedByFilterRE = "%";
        if (throwedByFilter != null && throwedByFilter.length() > 0) {
            throwedByFilterRE += throwedByFilter + "%";
        }
        List<Event> events;
        if (after > 0) {
            events = em.createNamedQuery("findEventsAfterDate", Event.class).setParameter("eventTypeFilter", eventTypeFilterRE).setParameter("fromObjectFilter", fromResourceFilterRE)
                    .setParameter("objectTypeFilter", resourceTypeFilterRE).setParameter("throwedByFilter", throwedByFilterRE).setParameter("after", new Date(after)).setFirstResult(offset)
                    .setMaxResults(limit).setLockMode(LockModeType.NONE).getResultList();

        } else {
            events = em.createNamedQuery("findEvents", Event.class).setParameter("eventTypeFilter", eventTypeFilterRE).setParameter("fromObjectFilter", fromResourceFilterRE)
                    .setParameter("objectTypeFilter", resourceTypeFilterRE).setParameter("throwedByFilter", throwedByFilterRE).setFirstResult(offset).setMaxResults(limit)
                    .setLockMode(LockModeType.NONE).getResultList();
        }
        return events;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long systemCountEvents(String eventTypeFilter, String fromResourceFilter, String resourceTypeFilter, String throwedByFilter, long after) throws EventServiceException {
        LOGGER.log(Level.FINE, "#SYSTEM# counting events");
        String eventTypeFilterRE = "%";
        if (eventTypeFilter != null && eventTypeFilter.length() > 0) {
            eventTypeFilterRE += eventTypeFilter + "%";
        }
        String fromResourceFilterRE = "%";
        if (fromResourceFilter != null && fromResourceFilter.length() > 0) {
            fromResourceFilterRE += fromResourceFilter + "%";
        }
        String resourceTypeFilterRE = "%";
        if (resourceTypeFilter != null && resourceTypeFilter.length() > 0) {
            resourceTypeFilterRE += resourceTypeFilter + "%";
        }
        String throwedByFilterRE = "%";
        if (throwedByFilter != null && throwedByFilter.length() > 0) {
            throwedByFilterRE += throwedByFilter + "%";
        }
        TypedQuery<Long> query;
        if (after > 0) {
            query = em.createNamedQuery("countEventsAfterDate", Long.class).setParameter("eventTypeFilter", eventTypeFilterRE).setParameter("fromObjectFilter", fromResourceFilterRE)
                    .setParameter("objectTypeFilter", resourceTypeFilterRE).setParameter("throwedByFilter", throwedByFilterRE).setParameter("after", new Date(after));

        } else {
            query = em.createNamedQuery("countEvents", Long.class).setParameter("eventTypeFilter", eventTypeFilterRE).setParameter("fromObjectFilter", fromResourceFilterRE)
                    .setParameter("objectTypeFilter", resourceTypeFilterRE).setParameter("throwedByFilter", throwedByFilterRE);
        }
        return query.getSingleResult();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public long systemCountAllEvents() throws EventServiceException {
        LOGGER.log(Level.FINE, "#SYSTEM# counting all events");
        TypedQuery<Long> query = em.createNamedQuery("countAllEvents", Long.class);
        return query.getSingleResult();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<? extends OrtolangEvent> systemListAllEventsForKey(String key) throws EventServiceException {
        LOGGER.log(Level.FINE, "#SYSTEM# listing all events for key " + key);
        List<Event> events = em.createNamedQuery("listAllEventsFromObject", Event.class).setParameter("fromObject", key).setLockMode(LockModeType.NONE).getResultList();
        return events;
    }

    /* Service Methods */

    @Override
    public String getServiceName() {
        return EventService.SERVICE_NAME;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String>();
        try {
            infos.put(INFO_FEEDS_ALL, Long.toString(registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(EventService.SERVICE_NAME, EventFeed.OBJECT_TYPE), null)));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_FEEDS_ALL, e);
        }
        try {
            infos.put(INFO_EVENTS_ALL, Long.toString(systemCountAllEvents()));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_EVENTS_ALL, e);
        }
        return infos;
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        for (int i = 0; i < OBJECT_PERMISSIONS_LIST.length; i++) {
            if (OBJECT_PERMISSIONS_LIST[i][0].equals(type)) {
                return OBJECT_PERMISSIONS_LIST[i][1].split(",");
            }
        }
        throw new OrtolangException("Unable to find object permissions list for object type : " + type);
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);

            if (!identifier.getService().equals(EventService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            if (identifier.getType().equals(EventFeed.OBJECT_TYPE)) {
                return readEventFeed(key);
            }

            throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
        } catch (RegistryServiceException | KeyNotFoundException | EventServiceException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    // TODO implement get size
    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        return null;
    }
    
    private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws EventServiceException {
        if (!identifier.getService().equals(getServiceName())) {
            throw new EventServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
        }

        if (!identifier.getType().equals(objectType)) {
            throw new EventServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
        }
    }
    
    @Override
    public void dump(String key, XMLStreamWriter writer, Set<String> deps, Set<String> streams) throws OrtolangException {
        //TODO Implement that
    }

    @Override
    public void restore() throws OrtolangException {
        throw new OrtolangException("NOT IMPLEMENTED");
    }

}
