package fr.ortolang.diffusion.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.ortolang.diffusion.store.binary.BinaryStoreVolumeMapper;
import fr.ortolang.diffusion.store.binary.VolumeNotFoundException;

public class WorkspaceLog {
    
    public String key;
    public String alias;
    public boolean deleted = false;
    public List<LogEvent> events = new ArrayList<LogEvent> ();
    public Map<String, List<LogEvent>> trackedObjects = new HashMap<String, List<LogEvent>> ();
    
    public void analyseEvents() {
        for ( LogEvent event : events ) {
            if ( event.type.equals("core.workspace.delete") ) {
                deleted = true;
            }
        }
        if ( !deleted ) {
            //find objects to track...
            for ( LogEvent event : events ) {
                if ( event.type.equals("core.object.update") ) {
                    String path = event.args.get("path");
                    String okey = event.args.get("key");
                    String key = event.args.get("okey");
                    if ( okey != null && key != null && key.equals(okey) ) {
                        //UPDATE WITHOUT CLONE DO NOT TRACK THIS
                    } else {
                        if ( !trackedObjects.containsKey(path) ) {
                            List<LogEvent> oevents = new ArrayList<LogEvent> ();
                            trackedObjects.put(path, oevents);
                        }
                    }
                }
            }
            //find complete history for tracked objects 
            for ( LogEvent event : events ) {
                if ( event.type.equals("core.workspace.snapshot") ) {
                    for ( List<LogEvent> history : trackedObjects.values() ) {
                        history.add(event);
                    }
                }
                String path = event.args.get("path");
                if ( path != null && event.type.matches("core.object.*") ) {
                    if ( trackedObjects.containsKey(path) ) {
                        trackedObjects.get(path).add(event);
                    }
                }
            }
            //order tracked object history
            for ( List<LogEvent> history : trackedObjects.values() ) {
                Collections.sort(history);
            }
        }
    }
    
    @Override
    public String toString() {
        Collections.sort(events);
        StringBuilder output = new StringBuilder();
        output.append("Workspace - ").append(key).append(" - ").append(alias).append("\r\n");
        for (LogEvent event : events) {
            if ( event.type.matches("core.workspace.*") || event.type.matches("core.object.update") ) {
                output.append("\t ").append(event).append("\r\n");
            }
        }
        return output.toString();
    }
    
    public String history() {
        StringBuilder output = new StringBuilder();
        output.append("Workspace - ").append(key).append(" - ").append(alias).append("\r\n");
        for ( Entry<String, List<LogEvent>> entry : trackedObjects.entrySet() ) {
            output.append("\t Path: ").append(entry.getKey()).append("\r\n");
            Collections.sort(entry.getValue());
            for ( LogEvent event : entry.getValue() ) {
                output.append("\t\t ").append(event.date).append(" - ").append(event.key).append(" - ").append(event.from).append(" - ").append(event.type).append(" - ").append(event.args).append("\r\n");
            }
        }
        return output.toString();
    }

    public String stats() {
        StringBuilder output = new StringBuilder();
        output.append("Workspace - ").append(key).append(" - ").append(alias).append("\r\n");
        for ( Entry<String, List<LogEvent>> entry : trackedObjects.entrySet() ) {
            output.append("\t Path: ").append(entry.getKey()).append("\r\n");
            Collections.sort(entry.getValue());
            for ( LogEvent event : entry.getValue() ) {
                output.append("\t\t ").append(event.date).append(" - ").append(event.key).append(" - ").append(event.from).append(" - ").append(event.type).append(" - ").append(event.args).append("\r\n");
            }
        }
        return output.toString();
    }
    
    public String sql() throws IOException, VolumeNotFoundException {
        StringBuilder output = new StringBuilder();
        for ( Entry<String, List<LogEvent>> entry : trackedObjects.entrySet() ) {
            Collections.sort(entry.getValue());
            for ( LogEvent event : entry.getValue() ) {
                if ( event.type.equals("core.object.create") || event.type.equals("core.object.update") ) {
                    String hash = event.args.get("hash");
                    String digit = hash.substring(0,2);
                    Path datafile = Paths.get("/home/jerome/prod/binary-store", BinaryStoreVolumeMapper.getVolume(digit), digit, hash);
                    long size = Files.size(datafile);
                    output.append("UPDATE dataobject SET stream='").append(hash).append("', size='").append(size).append("' WHERE id IN (SELECT substring(re.identifier from 14) FROM registryentry re WHERE re.key='").append(event.args.get("key")).append("');\r\n");
                }
            }
        }
        return output.toString();
    }
    
}
