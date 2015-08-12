package fr.ortolang.diffusion.ftp.filesystem;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

public class OrtolangFileSystemFactory implements FileSystemFactory {
	
    private static final Logger LOGGER = Logger.getLogger(OrtolangFileSystemFactory.class.getName());
    
	public OrtolangFileSystemFactory() {
	}

	@Override
	public FileSystemView createFileSystemView(User user) throws FtpException {
	    synchronized (user) {
	        LOGGER.log(Level.FINE, "creating new filesystem view for user: " + user.getName());
	        OrtolangFileSystemView fsview = new OrtolangFileSystemView(user);
	        return fsview;
	    }
	}

}
