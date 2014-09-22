package fr.ortolang.diffusion.api.ssh.vfs;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.sftp.SftpSubsystem;

public class DiffusionSftpSubsystem extends SftpSubsystem {
	
	protected final Logger logger = Logger.getLogger(DiffusionSftpSubsystem.class.getName());
	private ThreadFactory factory;
	
    public static class Factory implements NamedFactory<Command> {

    	private ThreadFactory threadFactory;
    	
        public Factory(ThreadFactory threadFactory) {
        	this.threadFactory = threadFactory;
        }

        public Command create() {
        	DiffusionSftpSubsystem dsftp = new DiffusionSftpSubsystem();
        	dsftp.setThreadFactory(threadFactory);
        	return dsftp;
        }

        public String getName() {
            return "sftp";
        }
    }
    
    public void setThreadFactory(ThreadFactory factory) {
    	this.factory = factory;
    }
    
    @Override
    public void start(Environment env) throws IOException {
    	logger.log(Level.INFO, "Creating new thread for diffusion sftp subsystem");
    	Thread thread = factory.newThread(this);
    	thread.start();
    }

}
