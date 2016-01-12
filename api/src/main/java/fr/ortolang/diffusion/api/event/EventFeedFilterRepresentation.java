package fr.ortolang.diffusion.api.event;

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