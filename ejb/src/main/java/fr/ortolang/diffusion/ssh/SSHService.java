package fr.ortolang.diffusion.ssh;


/**
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 21 September 2009
 */
public interface SSHService {
	
	public static final String SERVICE_NAME = "ssh";
	
	public void registerCommand(String name, String classname) throws SSHServiceException;
	
	public void importShellCommands(String packageName) throws SSHServiceException;

}
