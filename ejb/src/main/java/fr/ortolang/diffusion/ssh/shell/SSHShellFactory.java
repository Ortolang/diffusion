package fr.ortolang.diffusion.ssh.shell;

import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;

import fr.ortolang.diffusion.ssh.SSHServiceException;


/**
 * A factory for creating SSHShell and allow sshd to handle shell client requests.
 *
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 21 September 2009
 */
public class SSHShellFactory implements Factory<Command> {
    private static Logger logger = Logger.getLogger(SSHShellFactory.class.getName());
    private List<String> commands;
    
    public SSHShellFactory() {
    	commands = new Vector<String>();
    }

    @Override
    public SSHShell create() {
        logger.log(Level.FINE, "creating a new shell");
        return new SSHShell(commands);
    }
    
    public void importCommands(String packageName) throws SSHServiceException {
        if (commands.contains(packageName)) {
            throw new SSHServiceException("A package with name " + packageName + " is already imported");
        }
        commands.add(packageName);
    }
}
