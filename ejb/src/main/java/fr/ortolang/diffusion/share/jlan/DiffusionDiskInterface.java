package fr.ortolang.diffusion.share.jlan;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.core.DeviceContext;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileOpenParams;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.SearchContext;
import org.alfresco.jlan.server.filesys.TreeConnection;
import org.springframework.extensions.config.ConfigElement;

public class DiffusionDiskInterface implements DiskInterface {

	@Override
	public DeviceContext createContext(String shareName, ConfigElement args) throws DeviceContextException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createDirectory(SrvSession sess, TreeConnection tree, FileOpenParams params) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void deleteDirectory(SrvSession sess, TreeConnection tree, String dir) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void treeOpened(SrvSession sess, TreeConnection tree) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void treeClosed(SrvSession sess, TreeConnection tree) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public NetworkFile createFile(SrvSession sess, TreeConnection tree, FileOpenParams params) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int fileExists(SrvSession sess, TreeConnection tree, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void flushFile(SrvSession sess, TreeConnection tree, NetworkFile file) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FileInfo getFileInformation(SrvSession sess, TreeConnection tree, String name) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int readFile(SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf, int bufPos, int siz, long filePos) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void renameFile(SrvSession sess, TreeConnection tree, String oldName, String newName) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long seekFile(SrvSession sess, TreeConnection tree, NetworkFile file, long pos, int typ) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFileInformation(SrvSession sess, TreeConnection tree, String name, FileInfo info) throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void truncateFile(SrvSession sess, TreeConnection tree, NetworkFile file, long siz) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int writeFile(SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf, int bufoff, int siz, long fileoff) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void closeFile(SrvSession sess, TreeConnection tree, NetworkFile param) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteFile(SrvSession sess, TreeConnection tree, String name) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isReadOnly(SrvSession sess, DeviceContext ctx) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public NetworkFile openFile(SrvSession sess, TreeConnection tree, FileOpenParams params) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SearchContext startSearch(SrvSession sess, TreeConnection tree, String searchPath, int attrib) throws FileNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

}
