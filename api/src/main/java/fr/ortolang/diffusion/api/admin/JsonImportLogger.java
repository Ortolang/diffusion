package fr.ortolang.diffusion.api.admin;

import java.io.IOException;
import java.io.OutputStream;

import fr.ortolang.diffusion.OrtolangImportExportLogger;

public class JsonImportLogger implements OrtolangImportExportLogger {
    
    private OutputStream out;
    private boolean firstline = true;
    
    public JsonImportLogger() {
    }
    
    public void setOutputStream (OutputStream out) {
        this.out = out;
    }
    
    public void start() {
        try {
            out.write("[\r\n".getBytes());
        } catch (IOException e) { //
        }
    }
    
    public void finish() throws IOException {
        try {
            out.write("\r\n]".getBytes());
            out.close();
        } catch (IOException e) { //
        }   
    }

    @Override
    public void log(LogType type, String message) {
        StringBuilder builder = new StringBuilder();
        try {
            if ( !firstline ) {
                builder.append(",\r\n");
            }
            firstline = false;
            builder.append("\t{\"type\":\"").append(type.name()).append("\",\"message\":\"").append(message).append("\"}");
            out.write(builder.toString().getBytes());
        } catch ( IOException e ) { //
        }
    }

}
