package fr.ortolang.diffusion.event.entity;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
@SuppressWarnings("serial")
public class EventFeedFilter implements Serializable {

    private String id;
    private String throwedByRE;
    private String fromObjectRE;
    private String eventTypeRE;
    private String objectTypeRE;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventTypeRE() {
        return eventTypeRE;
    }

    public void setEventTypeRE(String eventTypeRE) {
        this.eventTypeRE = eventTypeRE;
    }

    public String getFromObjectRE() {
        return fromObjectRE;
    }

    public void setFromObjectRE(String fromObjectRE) {
        this.fromObjectRE = fromObjectRE;
    }

    public String getObjectTypeRE() {
        return objectTypeRE;
    }

    public void setObjectTypeRE(String objectTypeRE) {
        this.objectTypeRE = objectTypeRE;
    }

    public String getThrowedByRE() {
        return throwedByRE;
    }

    public void setThrowedByRE(String throwedByRE) {
        this.throwedByRE = throwedByRE;
    }

    public boolean match(Event e) {
        return e.getFromObject().matches(fromObjectRE) && e.getType().matches(eventTypeRE) && e.getObjectType().matches(objectTypeRE) && e.getThrowedBy().matches(throwedByRE);
    }

}