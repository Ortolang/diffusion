package fr.ortolang.diffusion.api.event;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.event.entity.EventFeed;

@XmlRootElement(name = "event-feed")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventFeedRepresentation {

    @XmlAttribute(name = "key")
    private String key;
    private String name;
    private String description;
    private List<OrtolangEvent> events;

    public EventFeedRepresentation() {
        events = Collections.emptyList();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<OrtolangEvent> getEvents() {
        return events;
    }

    public void setEvents(List<OrtolangEvent> events) {
        this.events = events;
    }

    public static EventFeedRepresentation fromEventFeed(EventFeed feed) {
        EventFeedRepresentation representation = new EventFeedRepresentation();
        representation.setKey(feed.getKey());
        representation.setName(feed.getName());
        representation.setDescription(feed.getDescription());
        representation.setEvents(feed.getEvents());
        return representation;
    }

}