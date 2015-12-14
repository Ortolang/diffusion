package fr.ortolang.diffusion.event;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import javax.persistence.PersistenceContext;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.event.entity.EventFeed;
import fr.ortolang.diffusion.event.entity.EventFeedFilter;
import fr.ortolang.diffusion.form.FormService;
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
            
            int cpt = 0;
            int offset = 0;
            int limit = 1000;
            boolean endOfEvents = false;
            while ( cpt < feed.getSize() || endOfEvents ) {
                List<Event> events = em.createNamedQuery("listAllEventsByDate", Event.class).setFirstResult(offset).setMaxResults(limit).getResultList();
                if ( events.size() < limit ) {
                    LOGGER.log(Level.FINEST, "listAllEventsByDate returned only " + events.size() + " events, seem that end is reached.");
                    endOfEvents = true;
                } else {
                    LOGGER.log(Level.FINEST, "listAllEventsByDate returned " + limit + " results, setting offset to next segment.");
                    offset += limit;
                }
                for ( Event event : events ) {
                    for ( EventFeedFilter filter : feed.getFilters() ) {
                        if ( filter.match(event) ) {
                            try {
                                if ( event.getFromObject() != null && event.getFromObject().length() > 0 ) {
                                    authorisation.checkPermission(event.getFromObject(), subjects, "read");
                                }
                                feed.pushEvent(event);
                                cpt++;
                            } catch ( AccessDeniedException e ) {
                                LOGGER.log(Level.FINEST, "no permission to read event with id: " + event.getId());
                            }
                            break;
                        }
                    }
                    if ( cpt >= feed.getSize() ) {
                        break;
                    }
                }
            }

            return feed;
        } catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
            throw new EventServiceException("unable to read the event feed with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateEventFeed(String key, String name, String description) throws EventServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "creating event feed for key [" + key + "] and name [" + name + "]");
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
            notification.throwEvent(key, caller, EventFeed.OBJECT_TYPE, OrtolangEvent.buildEventType(FormService.SERVICE_NAME, EventFeed.OBJECT_TYPE, "delete"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
            ctx.setRollbackOnly();
            throw new EventServiceException("unable to delete event feed with key [" + key + "]", e);
        }
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addEventFeedFilter(String key, String eventTypeRE, String fromObjectRE, String objectTypeRE, String throwedByRE) throws EventServiceException, AccessDeniedException,
            KeyNotFoundException {
        LOGGER.log(Level.FINE, "adding flter in event feed with key [" + key + "]");
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
        LOGGER.log(Level.FINE, "removing flter in event feed with key [" + key + "]");
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
            for ( EventFeedFilter filter : ef.getFilters() ) {
                if ( filter.getId().equals(id) ) {
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

    /* Service Methods */

    @Override
    public String getServiceName() {
        return EventService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        //TODO return infos
        return Collections.emptyMap();
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
        } catch (RegistryServiceException | KeyNotFoundException | AccessDeniedException | EventServiceException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    //TODO implement get size
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

}
