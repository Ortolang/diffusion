package fr.ortolang.diffusion.ftp;

import javax.ejb.Local;

@Local
public interface FtpService {
	
	public static final String SERVICE_NAME = "ftp";
	
	public void suspend();
	
	public void resume();
	

}
