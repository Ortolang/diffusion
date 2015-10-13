package fr.ortolang.diffusion.api.event;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fr.ortolang.diffusion.event.entity.EventFeedFilter;

@XmlRootElement(name = "event-feed-filter")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventFeedFilterRepresentation {

    @XmlAttribute(name = "id")
    private String id;
    private String throwedBy;
    private String sourceKey;
    private String eventType;
    private String objectType;

    public EventFeedFilterRepresentation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getThrowedBy() {
        return throwedBy;
    }

    public void setThrowedBy(String thrower) {
        this.throwedBy = thrower;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String source) {
        this.sourceKey = source;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String type) {
        this.eventType = type;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String object) {
        this.objectType = object;
    }

    public static EventFeedFilterRepresentation fromEventFeedFilter(EventFeedFilter filter) {
        EventFeedFilterRepresentation representation = new EventFeedFilterRepresentation();
        representation.setId(filter.getId());
        representation.setThrowedBy(filter.getThrowedByRE());
        representation.setSourceKey(filter.getFromObjectRE());
        representation.setEventType(filter.getEventTypeRE());
        representation.setObjectType(filter.getObjectTypeRE());
        return representation;
    }

}