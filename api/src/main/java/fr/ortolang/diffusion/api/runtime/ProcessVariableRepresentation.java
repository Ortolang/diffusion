package fr.ortolang.diffusion.api.runtime;

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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

public class ProcessVariableRepresentation {
	
	public static final String STRING_TYPE = "string";
	public static final String INTEGER_TYPE = "integer";
	public static final String SHORT_TYPE = "short";
	public static final String LONG_TYPE = "long";
	public static final String DOUBLE_TYPE = "double";
	public static final String BOOLEAN_TYPE = "boolean";
	public static final String DATE_TYPE = "date";
	public static final String OBJECT_TYPE = "object";
	
	
	public static final DateFormat df = DateFormat.getDateTimeInstance();

	private String name;
	private String value;
	private String type;
	
	public ProcessVariableRepresentation() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public Object getTypedValue() {
		switch ( type ) {
		case STRING_TYPE : return value;
		case INTEGER_TYPE : return Integer.parseInt(value);
		case SHORT_TYPE : return Short.parseShort(value);
		case LONG_TYPE : return Long.parseLong(value);
		case DOUBLE_TYPE : return Double.parseDouble(value);
		case BOOLEAN_TYPE : return Boolean.parseBoolean(value);
		case DATE_TYPE : try {
				return df.parse(value);
			} catch (ParseException e) {
				return null;
			}
		default: return value;
		}
	}
	
	public static ProcessVariableRepresentation fromObject(String name, Object value) {
	    ProcessVariableRepresentation instance = new ProcessVariableRepresentation();
	    instance.setName(name);
	    if ( value instanceof String ) {
	        instance.setType(STRING_TYPE);
	        instance.setValue((String)value);
	    } else if ( value instanceof Integer ) {
	        instance.setType(INTEGER_TYPE);
            instance.setValue(((Integer)value).toString());
	    } else if ( value instanceof Short ) {
            instance.setType(SHORT_TYPE);
            instance.setValue(((Short)value).toString());
        } else if ( value instanceof Long ) {
            instance.setType(LONG_TYPE);
            instance.setValue(((Long)value).toString());
        } else if ( value instanceof Double ) {
            instance.setType(DOUBLE_TYPE);
            instance.setValue(((Double)value).toString());
        } else if ( value instanceof Boolean ) {
            instance.setType(BOOLEAN_TYPE);
            instance.setValue(((Boolean)value).toString());
        } else if ( value instanceof Date ) {
            instance.setType(DATE_TYPE);
            instance.setValue(df.format((Date)value));
        } else {
	        instance.setType(OBJECT_TYPE);
            instance.setValue(value.toString());
	    }
	    return instance;
	}

}
