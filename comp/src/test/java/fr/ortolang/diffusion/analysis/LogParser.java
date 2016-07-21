package fr.ortolang.diffusion.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import fr.ortolang.diffusion.store.binary.VolumeNotFoundException;

public class LogParser {
    
    private static Map<String, WorkspaceLog> workspaces = new HashMap<String, WorkspaceLog> ();
    private static Map<String, WorkspaceLog> updatedworkspaces = new HashMap<String, WorkspaceLog> ();

    public static void main(String[] args) throws IOException, VolumeNotFoundException {
        String fileName = args[0];
        try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
            stream.forEach(LogParser::parseLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
//        for (WorkspaceLog ws : workspaces.values()) {
//            System.out.println(ws);
//        }
        
        System.out.println();
        System.out.println("###################################");
        System.out.println("Workspaces analysed : " + workspaces.size());
        System.out.println("###################################");
        System.out.println();
        
        for (WorkspaceLog ws : workspaces.values()) {
            for ( LogEvent event : ws.events ) {
                if ( event.type.equals("core.object.update") ) {
                    if ( !updatedworkspaces.containsKey(ws.key) ) {
                        updatedworkspaces.put(ws.key,  ws);
                    }
                    break;
                }
            }
        }
        
//        for (WorkspaceLog ws : updatedworkspaces.values()) {
//            System.out.println(ws);
//        }
//        
        System.out.println();
        System.out.println("###################################");
        System.out.println("Workspaces with updated objects : " + updatedworkspaces.size());
        System.out.println("###################################");
        System.out.println();
        
        //Pour chaque ws, trouver les objets updaté, et recréer l'historique de ces objets par le path et avec les snapshots)
        
        for (WorkspaceLog ws : updatedworkspaces.values()) {
            ws.analyseEvents();
            if ( !ws.deleted ) {
                //System.out.println(ws.alias);
                System.out.println(ws.history());
            }
        }
        
        System.out.println();
        System.out.println("###################################");
        System.out.println("Fix script");
        System.out.println("###################################");
        System.out.println();
        
        for (WorkspaceLog ws : updatedworkspaces.values()) {
            if ( !ws.deleted ) {
                System.out.println(ws.sql());
            }
        }
    }
    
    public static void parseLine (String line) {
        try {
            LogEvent event = LogEvent.fromLine(line);
            if ( event.from.matches("workspace") ) {
                if ( !workspaces.containsKey(event.key) ) {
                    WorkspaceLog ws = new WorkspaceLog();
                    ws.key = event.key;
                    workspaces.put(ws.key, ws);
                }
                if ( workspaces.get(event.key).alias == null && event.args.get("ws-alias") != null ) {
                    workspaces.get(event.key).alias = event.args.get("ws-alias");
                }
                workspaces.get(event.key).events.add(event);
            }
        } catch ( Exception e ) {
            System.out.println("unable to parse line : " + line + " " + e.getMessage());
        }
    }
    
}
