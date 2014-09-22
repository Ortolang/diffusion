package fr.ortolang.diffusion.api.ssh.shell;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;

public class SSHShellFactory implements Factory<Command> {
    
	private static Log logger = LogFactory.getLog(SSHShellFactory.class);
    private List<String> commands;
    private ThreadFactory threadFactory;
    
    public SSHShellFactory(ThreadFactory threadFactory) {
    	commands = new Vector<String>();
    	this.threadFactory = threadFactory;
    }

    @Override
    public SSHShell create() {
        logger.debug("creating a new shell");
        return new SSHShell(commands, threadFactory);
    }
    
    public void importCommands(String packageName) throws SSHShellException {
        if (commands.contains(packageName)) {
            throw new SSHShellException("A package with name " + packageName + " is already imported");
        }
        commands.add(packageName);
    }
}
