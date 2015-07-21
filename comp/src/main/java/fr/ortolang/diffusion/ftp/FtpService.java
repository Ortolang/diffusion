package fr.ortolang.diffusion.ftp;

public interface FtpService {
	
	public static final String SERVICE_NAME = "ftp";
	
	public void suspend();
	
	public void resume();

}
