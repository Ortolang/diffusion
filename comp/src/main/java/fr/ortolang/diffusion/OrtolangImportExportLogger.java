package fr.ortolang.diffusion;

public interface OrtolangImportExportLogger {
    
    public void log(LogType type, String message);
    
    public enum LogType {
         APPEND, MESSAGE, NOTICE, ERROR;
    }

}
