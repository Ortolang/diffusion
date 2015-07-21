package fr.ortolang.diffusion.ftp.filesystem;

import java.util.HashMap;
import java.util.Map;

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

public class OrtolangFileSystemFactory implements FileSystemFactory {
	
	private Map<String, FileSystemView> cache;
	
	public OrtolangFileSystemFactory() {
		cache = new HashMap<String, FileSystemView> ();
	}

	@Override
	public FileSystemView createFileSystemView(User user) throws FtpException {
		if ( !cache.containsKey(user.getName()) ) {
			cache.put(user.getName(), new OrtolangFileSystemView(user));
		}
		return cache.get(user.getName());
	}

}
