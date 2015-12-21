package fr.ortolang.diffusion.event.entity;

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
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
    public static final int DEFAULT_SIZE = 10;

    @Id
    private String id;
    @Version
    private long version;
    @Transient
    private String key;
    private String name;
    private String description;
    private int size;
    @ElementCollection(fetch=FetchType.EAGER)
    private List<EventFeedFilter> filters;
    @Transient
    private List<OrtolangEvent> events;
    
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
        if ( size >= MAX_SIZE ) {
            this.size = MAX_SIZE;
        } else {
            this.size = size;
        }
    }

    public List<EventFeedFilter> getFilters() {
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

    public List<OrtolangEvent> getEvents() {
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