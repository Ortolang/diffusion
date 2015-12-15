package fr.ortolang.diffusion.rendering;

import java.nio.file.Path;
import java.util.Locale;

public interface RenderEngine {
    
    public String getId();
    
    public String getName(Locale locale);
    
    public String getDescription(Locale locale);
   
    public boolean canRender(String mimetype);
    
    public boolean render(Path input, Path output);

}
