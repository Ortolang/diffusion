package fr.ortolang.diffusion.security.authentication;


public interface AuthenticationService {
	
	public static final String SERVICE_NAME = "authentication";
    
    public String getConnectedIdentifier();

}
