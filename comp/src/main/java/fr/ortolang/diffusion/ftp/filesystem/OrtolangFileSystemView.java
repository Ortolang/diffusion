package fr.ortolang.diffusion.ftp.filesystem;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

public class OrtolangFileSystemView implements FileSystemView {
	
	private User user;
	private String current;

	public OrtolangFileSystemView(User user) {
		this.user = user;
	}

	@Override
	public FtpFile getHomeDirectory() throws FtpException {
		OrtolangUserFtpFile home = new OrtolangUserFtpFile(user.getName());
		return home;
	}

	@Override
	public FtpFile getWorkingDirectory() throws FtpException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FtpFile getFile(String path) throws FtpException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean changeWorkingDirectory(String path) throws FtpException {
		return false;
	}

	@Override
	public boolean isRandomAccessible() throws FtpException {
		return true;
	}
	
	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

}
