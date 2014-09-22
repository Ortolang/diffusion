package fr.ortolang.diffusion.bootstrap;

public interface BootstrapService {
	
	public static final String VERSION = "1.0";
    public static final String WORKSPACE_KEY = "system";
    public static final String SERVICE_NAME = "bootstrap";
    
    public void bootstrap() throws BootstrapServiceException;

}
