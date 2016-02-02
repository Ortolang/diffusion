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
        return (e.getFromObject() == null || e.getFromObject().matches(fromObjectRE)) && e.getType().matches(eventTypeRE) && e.getObjectType().matches(objectTypeRE) && (e.getThrowedBy() == null || e.getThrowedBy().matches(throwedByRE));
    }

}