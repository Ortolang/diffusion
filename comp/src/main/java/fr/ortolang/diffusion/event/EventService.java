package fr.ortolang.diffusion.event;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.event.entity.Event;
import fr.ortolang.diffusion.event.entity.EventFeed;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface EventService extends OrtolangService {

    public static final String SERVICE_NAME = "event";
    
    public static final String INFO_FEEDS_ALL = "feeds.all";
    public static final String INFO_EVENTS_ALL = "events.all";
    
    public void createEventFeed(String key, String name, String description) throws EventServiceException, AccessDeniedException, KeyAlreadyExistsException;

    public void addEventFeedFilter(String key, String eventTypeRE, String fromResourceRE, String resourceTypeRE, String throwedByRE) throws EventServiceException, AccessDeniedException, KeyNotFoundException;

    public void removeEventFeedFilter(String key, String id) throws EventServiceException, AccessDeniedException, KeyNotFoundException;

    public EventFeed readEventFeed(String key) throws EventServiceException, AccessDeniedException, KeyNotFoundException; 
    
    public void updateEventFeed(String key, String name, String description) throws EventServiceException, AccessDeniedException, KeyNotFoundException;

    public void deleteEventFeed(String key) throws EventServiceException, AccessDeniedException, KeyNotFoundException;
    
    public void persistEvent(Event event) throws EventServiceException;

}
