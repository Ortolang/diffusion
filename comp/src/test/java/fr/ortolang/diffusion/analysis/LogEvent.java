package fr.ortolang.diffusion.analysis;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LogEvent implements Comparable<LogEvent> {
    
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS ZZ");
    
    public Date date;
    public String key;
    public String from;
    public String type;
    public String author;
    public Map<String, String> args;
    
    public static LogEvent fromLine(String line) throws ParseException, IOException {
        String[] parts = line.substring(13, line.indexOf("{")).split(",");
        String args = line.substring(line.indexOf("{"));
        LogEvent event = new LogEvent();
        event.setDate(parts[0]);
        event.key = parts[1];
        event.from = parts[2];
        event.type = parts[3];
        event.author = parts[4];
        event.setArguments(args);
        return event;
    }
    
    public void setDate(String date) throws ParseException {
        this.date = sdf.parse(date.substring(1, date.length()-1));
    }
    
    public void setArguments (String serializedArgs) throws IOException {
        if ( serializedArgs != null && serializedArgs.length() > 2 ) {
            serializedArgs = serializedArgs.substring(1, serializedArgs.length()-1);
            Map<String, String> map = new HashMap<String, String>();
            String pkey = "";
            for (String arg : serializedArgs.split(",")) {
                arg = arg.trim();
                if ( arg.contains("=") ) {
                    String key = arg.substring(0, arg.indexOf("="));
                    String value = arg.substring(arg.indexOf("=")+1, arg.length());
                    map.put(key, value);
                    pkey = key;
                } else {
                    String value = map.get(pkey) + arg;
                    map.put(pkey, value);
                }
            }
            this.args = map;
        } else {
            this.args = Collections.emptyMap();
        }
    }

    @Override
    public int compareTo(LogEvent o) {
        return this.date.compareTo(o.date);
    }
    
    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(date).append(" ").append(key).append(" ").append(type);
        if ( type.matches("core.object.*") ) {
            output.append(" sha1:").append(args.get("hash")).append(" path:").append(args.get("path"));
        }
        return output.toString();
    }

}
