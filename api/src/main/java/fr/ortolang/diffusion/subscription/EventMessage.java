package fr.ortolang.diffusion.subscription;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 * *
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

import fr.ortolang.diffusion.event.entity.Event;

import java.util.Date;
import java.util.Map;

public class EventMessage {

    private String fromObject;

    private String throwedBy;

    private String objectType;

    private Date date;

    private String type;

    private Map<String, String> arguments;

    public EventMessage() {
    }

    public String getFromObject() {
        return fromObject;
    }

    public void setFromObject(String fromObject) {
        this.fromObject = fromObject;
    }

    public String getThrowedBy() {
        return throwedBy;
    }

    public void setThrowedBy(String throwedBy) {
        this.throwedBy = throwedBy;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, String> arguments) {
        this.arguments = arguments;
    }

    public EventMessage fromEvent(Event event) {
        setFromObject(event.getFromObject());
        setThrowedBy(event.getThrowedBy());
        setObjectType(event.getObjectType());
        setDate(event.getDate());
        setType(event.getType());
        setArguments(event.getArguments());
        return this;
    }

    @Override
    public String toString() {
        return "EventMessage{" +
                "fromObject='" + fromObject + '\'' +
                ", throwedBy='" + throwedBy + '\'' +
                ", objectType='" + objectType + '\'' +
                ", date=" + date +
                ", type='" + type + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}
