package fr.ortolang.diffusion.api.rest.runtime;

import java.text.DateFormat;
import java.text.ParseException;

public class ProcessVariableRepresentation {
	
	public static final String STRING_TYPE = "string";
	public static final String INTEGER_TYPE = "integer";
	public static final String SHORT_TYPE = "short";
	public static final String LONG_TYPE = "long";
	public static final String DOUBLE_TYPE = "double";
	public static final String BOOLEAN_TYPE = "boolean";
	public static final String DATE_TYPE = "date";
	
	
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

}
