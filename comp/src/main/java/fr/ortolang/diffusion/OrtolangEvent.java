package fr.ortolang.diffusion;

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

import org.apache.commons.codec.binary.Base64;

import javax.jms.JMSException;
import javax.jms.Message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class OrtolangEvent {

    private static final Logger LOGGER = Logger.getLogger(OrtolangEvent.class.getName());

    public static final String DATE = "eventdate";
    public static final String TYPE = "eventtype";
    public static final String FROM_OBJECT = "fromobject";
    public static final String THROWED_BY = "throwedby";
    public static final String OBJECT_TYPE = "objecttype";
    public static final String ARGUMENTS = "arguments";

    private static Map<String, SimpleDateFormat> sdf = new HashMap<String, SimpleDateFormat> ();

    public static SimpleDateFormat getEventDateFormatter() {
        String key = Thread.currentThread().getId() + "";
        if ( sdf.get(key) == null ) {
            sdf.put(key, new SimpleDateFormat(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.DATE_FORMAT_PATTERN)));
        }
        return sdf.get(key);
    }

    public abstract Date getDate();

    public abstract void setDate(Date date);

    public String getFormattedDate() {
        return getEventDateFormatter().format(getDate());
    }

    public void setFormattedDate(String date) throws OrtolangException {
        try {
            if (date != null) {
                this.setDate(getEventDateFormatter().parse(date));
            }
        } catch ( ParseException e ) {
            throw new OrtolangException("unable to parse date", e);
        }
    }

    public abstract String getType();

    public abstract void setType(String type);

    public abstract String getFromObject();

    public abstract void setFromObject(String from);

    public abstract String getObjectType();

    public abstract void setObjectType(String resource);

    public abstract String getThrowedBy();

    public abstract void setThrowedBy(String throwedby);

    public abstract Map<String, String> getArguments();

    public abstract void setArguments(Map<String, String> arguments);

    @SuppressWarnings("unchecked")
    public void fromJMSMessage(Message message) throws OrtolangException {
        try {
            setFormattedDate(message.getStringProperty(OrtolangEvent.DATE));
            setThrowedBy(message.getStringProperty(OrtolangEvent.THROWED_BY));
            setFromObject(message.getStringProperty(OrtolangEvent.FROM_OBJECT));
            setObjectType(message.getStringProperty(OrtolangEvent.OBJECT_TYPE));
            setType(message.getStringProperty(OrtolangEvent.TYPE));
            String serializedArgs = message.getStringProperty(OrtolangEvent.ARGUMENTS);
            if (serializedArgs != null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(serializedArgs));
                try {
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Map<String, String> args = (Map<String, String>) ois.readObject();
                    setArguments(args);
                } catch (IOException | ClassNotFoundException e) {
                    LOGGER.log(Level.SEVERE, "Unable to deserialize arguments", e);
                }
            }
        } catch ( JMSException e ) {
            throw new OrtolangException("unable to build event from jms message", e);
        }
    }

    public static String buildEventType(String serviceName, String resourceName, String eventName) {
        StringBuilder event = new StringBuilder();
        if ( serviceName != null && serviceName.length() > 0 ) {
            event.append(serviceName).append(".");
        }
        if ( resourceName != null && resourceName.length() > 0 ) {
            event.append(resourceName).append(".");
        }
        event.append(eventName);
        return event.toString();
    }

    public static class ArgumentsBuilder {

        private Map<String, String> args;

        public ArgumentsBuilder() {
            args = new HashMap<>();
        }

        public ArgumentsBuilder(int initialCapacity) {
            args = new HashMap<>(initialCapacity);
        }

        public ArgumentsBuilder(String key, String value) {
            this(1);
            addArgument(key, value);
        }

        public ArgumentsBuilder(Map<String, String> args) {
            this.args = args;
        }

        public ArgumentsBuilder addArgument(String key, String value) {
            if ( value == null ) {
                value = "";
            }
            args.put(key, value);
            return this;
        }

        public Map<String, String> build() {
            return args;
        }
    }
}
