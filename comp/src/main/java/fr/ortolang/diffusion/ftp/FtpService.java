package fr.ortolang.diffusion.ftp;

import fr.ortolang.diffusion.OrtolangService;

public interface FtpService extends OrtolangService {
    
    public static final String SERVICE_NAME = "ftp";
    
    public static final String INFO_SERVER_HOST = "ftp.server.host";
    public static final String INFO_SERVER_PORT = "ftp.server.port";
    public static final String INFO_SERVER_STATE = "ftp.server.state";
    public static final String INFO_LOGIN_FAILURE_DELAY = "ftp.login.failure.delay";
    public static final String INFO_MAX_ANON_LOGIN = "ftp.login.anon.max";
    public static final String INFO_MAX_LOGIN_FAILURES = "ftp.login.failures.max";
    public static final String INFO_MAX_LOGIN = "ftp.login.max";
    public static final String INFO_MAX_THREADS = "ftp.threads.max";
    public static final String INFO_ANON_LOGIN_ENABLES = "ftp.login.anon.enabled";
    
	
	public void suspend();
	
	public void resume();
	
	public boolean checkUserExistence(String username) throws FtpServiceException;
	
	public boolean checkUserAuthentication(String username, String password) throws FtpServiceException;
	
	public String getInternalAuthenticationPassword(String username) throws FtpServiceException;

}
