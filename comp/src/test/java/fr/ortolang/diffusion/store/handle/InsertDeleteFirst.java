package fr.ortolang.diffusion.store.handle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringTokenizer;

public class InsertDeleteFirst {
    
    public static void main(String args[]) {
        Path input = Paths.get("/home/jerome/ortolang_sldr_handles_new.sql");
        Path output = Paths.get("/home/jerome/ortolang_sldr_handles_new_with_delete.sql");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int nbreads = 0;
        try (InputStream fis = Files.newInputStream(input)) {
            while ( (nbreads=fis.read(buf)) != -1 ) {
                baos.write(buf, 0, nbreads);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        String content = baos.toString();
        StringTokenizer lines = new StringTokenizer(content, "\r\n");
        
        try ( OutputStream os = Files.newOutputStream(output)) {
            while ( lines.hasMoreTokens() ) {
                String line = lines.nextToken();
                if ( line.startsWith("INSERT") ) {
                    String[] parts = line.split(",");
                    String handle = parts[0].substring(parts[0].indexOf("(")+1);
                    String idx = parts[1].trim();
                    String sql = "DELETE FROM handles WHERE handle=" + handle + " AND idx = " + idx + ";\r\n";
                    os.write(sql.getBytes());
                } 
                os.write(line.getBytes());
                os.write("\r\n".getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

}
