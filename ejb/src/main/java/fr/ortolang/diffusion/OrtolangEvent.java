package fr.ortolang.diffusion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import javax.jms.JMSException;
import javax.jms.Message;

public abstract class OrtolangEvent {
	
	public static final String DATE = "eventdate";
	public static final String TYPE = "eventtype";
	public static final String FROM_OBJECT = "fromobject";
	public static final String THROWED_BY = "throwedby";
	public static final String OBJECT_TYPE = "objecttype";
	public static final String ARGUMENTS = "arguments";
	
	private static HashMap<String, SimpleDateFormat> sdf = new HashMap<String, SimpleDateFormat> (); 
	
	public static SimpleDateFormat getEventDateFormatter() {
		String key = Thread.currentThread().getId() + "";
		if ( sdf.get(key) == null ) {
			sdf.put(key, new SimpleDateFormat(OrtolangConfig.getInstance().getProperty("date.format.pattern")));
		}
		return sdf.get(key);
	}
	
	public abstract Date getDate();
	
	public abstract void setDate(Date date);
	
	public String getFormatedDate() {
		return getEventDateFormatter().format(getDate());
	}
	
	public void setFormatedDate(String date) throws OrtolangException {
		try {
			this.setDate(getEventDateFormatter().parse(date));
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
	
	public abstract String getArguments();
	
	public abstract void setArguments (String arguments);
	
	public void fromJMSMessage(Message message) throws OrtolangException {
		try {
			setFormatedDate(message.getStringProperty(OrtolangEvent.DATE));
			setThrowedBy(message.getStringProperty(OrtolangEvent.THROWED_BY));
			setFromObject(message.getStringProperty(OrtolangEvent.FROM_OBJECT));
	        setObjectType(message.getStringProperty(OrtolangEvent.OBJECT_TYPE));
			setType(message.getStringProperty(OrtolangEvent.TYPE));
			setArguments(message.getStringProperty(OrtolangEvent.ARGUMENTS));
		} catch ( JMSException e ) {
			throw new OrtolangException("unable to build event from jms message", e);
		}
	}
	
//	public String toString() {
//		JSONObject jsonObject = JSONObject.fromObject( this );  
//	    return jsonObject.toString();
//	}

}
