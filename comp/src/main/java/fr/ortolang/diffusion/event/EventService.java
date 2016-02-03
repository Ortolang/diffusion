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

import java.util.Date;
import java.util.List;

import fr.ortolang.diffusion.OrtolangEvent;
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
    
    public List<OrtolangEvent> browseEventFeed(String key, int offset, int limit) throws EventServiceException, AccessDeniedException, KeyNotFoundException;
    
    public List<OrtolangEvent> browseEventFeedSinceDate(String key, Date from) throws EventServiceException, AccessDeniedException, KeyNotFoundException;
    
    public List<OrtolangEvent> browseEventFeedSinceEvent(String key, long id) throws EventServiceException, AccessDeniedException, KeyNotFoundException;
    
    public void updateEventFeed(String key, String name, String description) throws EventServiceException, AccessDeniedException, KeyNotFoundException;

    public void deleteEventFeed(String key) throws EventServiceException, AccessDeniedException, KeyNotFoundException;
    
    public void persistEvent(Event event) throws EventServiceException;

}
