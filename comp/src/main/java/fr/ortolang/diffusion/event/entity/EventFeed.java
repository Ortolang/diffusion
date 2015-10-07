package fr.ortolang.diffusion.event.entity;

import java.util.ArrayList;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.persistence.Version;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.event.EventService;

@Entity
@SuppressWarnings ("serial")
public class EventFeed extends OrtolangObject {

    public static final String OBJECT_TYPE = "eventfeed";
    public static final int MAX_SIZE = 100;
    public static final int DEFAULT_SIZE = 20;

    @Id
    private String id;
    @Version
    private long version;
    @Transient
    private String key;
    private String name;
    private String description;
    private int size;
    @ElementCollection
    private ArrayList<EventFeedFilter> filters;
    @Transient
    private ArrayList<OrtolangEvent> events;
    
    public EventFeed() {
        filters = new ArrayList<EventFeedFilter>();
        events = new ArrayList<OrtolangEvent>();
        size = DEFAULT_SIZE;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public ArrayList<EventFeedFilter> getFilters() {
        return filters;
    }

    public void setFilters(ArrayList<EventFeedFilter> filters) {
        this.filters = filters;
    }
    
    public void addFilter(EventFeedFilter filter) {
        this.filters.add(filter);
    }
    
    public void removeFilter(EventFeedFilter filter) {
        this.filters.remove(filter);
    }

    public ArrayList<OrtolangEvent> getEvents() {
        return events;
    }

    public void setEvents(ArrayList<OrtolangEvent> events) {
        this.events = events;
    }
    
    public void pushEvent(OrtolangEvent event) {
        this.events.add(event);
    }
    
    @Override
    public OrtolangObjectIdentifier getObjectIdentifier() {
        return new OrtolangObjectIdentifier(EventService.SERVICE_NAME, EventFeed.OBJECT_TYPE, getId());
    }

    @Override
    public String getObjectName() {
        return getName();
    }

    @Override
    public String getObjectKey() {
        return getKey();
    }

}