package fr.ortolang.diffusion.api.ssh.vfs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;

import fr.ortolang.diffusion.api.ssh.session.SSHSession;

public class DiffusionFileSystemFactory implements FileSystemFactory {
	
	private static Logger logger = Logger.getLogger(DiffusionFileSystemFactory.class.getName());
	
    @Override
	public FileSystemView createFileSystemView(Session session) throws IOException {
    	logger.log(Level.INFO, "Creating new DiffusionFileSystemView for session : " + session.getUsername());
    	return new DiffusionFileSystemView((SSHSession) session);
	}

}
