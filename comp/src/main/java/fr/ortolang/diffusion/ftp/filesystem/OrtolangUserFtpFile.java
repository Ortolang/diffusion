package fr.ortolang.diffusion.ftp.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;

public class OrtolangUserFtpFile implements FtpFile {
	
	private String username;
	
	public OrtolangUserFtpFile(String username) {
		this.username = username;
	}

	@Override
	public String getName() {
		return "/";
	}

	@Override
	public String getOwnerName() {
		return username;
	}

	@Override
	public boolean doesExist() {
		return true;
	}

	@Override
	public boolean delete() {
		return false;
	}

	@Override
	public String getAbsolutePath() {
		return "/";
	}

	@Override
	public String getGroupName() {
		return username;
	}

	@Override
	public long getLastModified() {
		return 0;
	}

	@Override
	public int getLinkCount() {
		return 0;
	}

	@Override
	public long getSize() {
		return 0;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public boolean isReadable() {
		return true;
	}

	@Override
	public boolean isRemovable() {
		return false;
	}

	@Override
	public boolean isWritable() {
		return false;
	}

	@Override
	public List<FtpFile> listFiles() {
		// TODO List files for user's project alias.
		return null;
	}

	@Override
	public boolean mkdir() {
		return false;
	}

	@Override
	public boolean move(FtpFile destination) {
		return false;
	}

	@Override
	public boolean setLastModified(long lastmodified) {
		return false;
	}
	
	@Override
	public InputStream createInputStream(long offset) throws IOException {
		throw new IOException("unable to create input stream for root folder");
	}

	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		throw new IOException("unable to create output stream for root folder");
	}

	

}
