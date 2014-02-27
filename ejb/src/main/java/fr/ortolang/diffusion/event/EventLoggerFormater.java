package fr.ortolang.diffusion.event;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import fr.ortolang.diffusion.event.entity.Event;

public class EventLoggerFormater {
	
	private static HashMap<String, SimpleDateFormat> sdf = new HashMap<String, SimpleDateFormat> (); 
	private static String fieldSeparator = ",";
	
	private static SimpleDateFormat getEventDateFormatter() {
		String key = Thread.currentThread().getId() + "";
		if ( !sdf.containsKey(key) ) {
			sdf.put(key, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS ZZ"));
		}
		return sdf.get(key);
	}
	
	public static String formatEvent(Event e) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[" + getEventDateFormatter().format(e.getDate()) + "]");
		buffer.append(fieldSeparator);
		buffer.append(e.getFromObject());
		buffer.append(fieldSeparator);
		buffer.append(e.getObjectType());
		buffer.append(fieldSeparator);
		buffer.append(e.getType());
		buffer.append(fieldSeparator);
		buffer.append(e.getThrowedBy());
		buffer.append(fieldSeparator);
		buffer.append(e.getArguments());
		buffer.append(fieldSeparator);
		return buffer.toString();
	}

}