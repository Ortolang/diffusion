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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import fr.ortolang.diffusion.OrtolangEvent;

@Entity
@SuppressWarnings("serial")
@Table(indexes={@Index(columnList="date", name="eventDateIndex"), @Index(columnList="fromObject", name="eventFromIndex")})
@NamedQueries({
        @NamedQuery(name = "countAllEvents", query = "SELECT count(e) FROM Event e"),
        @NamedQuery(name = "listAllEvents", query = "SELECT e FROM Event e ORDER BY e.date DESC"),
        @NamedQuery(name = "listAllEventsFromId", query = "SELECT e FROM Event e WHERE e.id > :id ORDER BY e.date DESC"),
        @NamedQuery(name = "listAllEventsFromDate", query = "SELECT e FROM Event e WHERE e.date > :date ORDER BY e.date DESC"),
        @NamedQuery(name = "listAllEventsThrowedBy", query = "SELECT e FROM Event e WHERE e.throwedBy = :throwedBy ORDER BY e.date DESC"),
        @NamedQuery(name = "listAllEventsFromObject", query = "SELECT e FROM Event e WHERE e.fromObject = :fromObject ORDER BY e.date DESC"),
        @NamedQuery(name = "findEvents", query = "SELECT e FROM Event e WHERE e.fromObject LIKE :fromObjectFilter AND e.throwedBy LIKE :throwedByFilter AND e.type LIKE :eventTypeFilter AND e.objectType LIKE :objectTypeFilter ORDER BY e.date DESC"),
        @NamedQuery(name = "findEventsAfterDate", query = "SELECT e FROM Event e WHERE e.fromObject LIKE :fromObjectFilter AND e.throwedBy LIKE :throwedByFilter AND e.type LIKE :eventTypeFilter AND e.objectType LIKE :objectTypeFilter AND e.date > :after ORDER BY e.date DESC"),
        @NamedQuery(name = "findEventsAfterId", query = "SELECT e FROM Event e WHERE e.fromObject LIKE :fromObjectFilter AND e.throwedBy LIKE :throwedByFilter AND e.type LIKE :eventTypeFilter AND e.objectType LIKE :objectTypeFilter AND e.id > :after ORDER BY e.date DESC")
        })
public class Event extends OrtolangEvent implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(Event.class.getName());

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String fromObject;
    private String throwedBy;
    private String objectType;
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;
    private String type;
    @Transient
    private Map<String, String> args;
    @Column(columnDefinition="TEXT")
    private String serializedArgs;

    public Event() {
        args = Collections.emptyMap();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getFromObject() {
        return fromObject;
    }

    @Override
    public void setFromObject(String fromObject) {
        this.fromObject = fromObject;
    }

    @Override
    public String getThrowedBy() {
        return throwedBy;
    }

    @Override
    public void setThrowedBy(String throwedBy) {
        this.throwedBy = throwedBy;
    }

    @Override
    public String getObjectType() {
        return objectType;
    }

    @Override
    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Map<String, String> getArguments() {
        return args;
    }

    @Override
    public void setArguments(Map<String, String> args) {
        this.args = args;
        try {
            this.serializedArgs = serializeArgs(args);
            LOGGER.log(Level.FINEST, "arguments serialized for event");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "unable to serialize event arguments");
        }
    }

    @PostLoad
    private void onPostLoad() {
        if (serializedArgs != null && serializedArgs.length() > 0 ) {
            try {
                this.args = deserializeArgs(serializedArgs);
                LOGGER.log(Level.FINEST, "arguments deserialized for event");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "unable to deserialize event arguments");
            }
        }
    }

    @PostUpdate
    private void onPostUpdate() {
        if (serializedArgs != null && serializedArgs.length() > 0 ) {
            try {
                this.args = deserializeArgs(serializedArgs);
                LOGGER.log(Level.FINEST, "arguments deserialized for event");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "unable to deserialize event arguments");
            }
        }
    }

    private static String serializeArgs(Map<String, String> args) throws IOException {
        if (args == null || args.size() == 0 ) {
            return "";
        }
        Properties props = new Properties();
        for ( Entry<String, String> entry : args.entrySet() ) {
            if (entry.getValue() != null) {
                props.setProperty(entry.getKey(), entry.getValue());
            }
        }
        StringWriter out = new StringWriter();
        props.store(out, null);
        return out.toString();
    }

    private static Map<String, String> deserializeArgs(String serializedArgs) throws IOException {
        if (serializedArgs != null && serializedArgs.length() > 0 ) {
            Properties props = new Properties();
            props.load(new StringReader(serializedArgs));
            Map<String, String> map = new HashMap<String, String>();
            for (String key : props.stringPropertyNames()) {
                map.put(key, props.getProperty(key));
            }
            return map;
        }
        return Collections.emptyMap();
    }


    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", fromObject='" + fromObject + '\'' +
                ", throwedBy='" + throwedBy + '\'' +
                ", objectType='" + objectType + '\'' +
                ", date=" + date +
                ", type='" + type + '\'' +
                ", arguments=" + args +
                '}';
    }
}
