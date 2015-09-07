package fr.ortolang.diffusion.ftp;

import fr.ortolang.diffusion.OrtolangService;

public interface FtpService extends OrtolangService {
    
    public static final String SERVICE_NAME = "ftp";
	
	public void suspend();
	
	public void resume();
	
	public boolean checkUserExistence(String username) throws FtpServiceException;
	
	public boolean checkUserAuthentication(String username, String password) throws FtpServiceException;
	
	public String getInternalAuthenticationPassword(String username) throws FtpServiceException;

}
