package fr.ortolang.diffusion.api.ssh.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.sshd.common.file.SshFile;
import org.xnio.channels.UnsupportedOptionException;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class ProfileSshFile implements SshFile {
	
	private static Logger logger = Logger.getLogger(ProfileSshFile.class.getName());
	
	private DiffusionFileSystemView view;
	private List<String> workspaces;
	private boolean exists;
	private boolean readable;
	
	protected ProfileSshFile(DiffusionFileSystemView view) throws OrtolangException {
		logger.log(Level.INFO, "ProfileDiffusionSshFile created");
		this.view = view; 
		this.load();
	}
	
	private void load() throws OrtolangException {
		logger.log(Level.INFO, "loading profile : " + view.getConnectedUser());
		try {
			LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(view.getSession().getLogin(), view.getSession().getPassword());
			lc.login();
			workspaces = view.getCore().findWorkspacesForProfile(view.getConnectedUser());
			exists = true;
			readable = true;
			lc.logout();
		} catch (CoreServiceException | OrtolangException | LoginException e) {
			logger.log(Level.SEVERE, "error while trying to load profile for user " + view.getConnectedUser(), e);
			throw new OrtolangException("error while trying to load profile for user " + view.getConnectedUser(), e);
		} catch (KeyNotFoundException e) {
			logger.log(Level.WARNING, "unable to read profile for user " + view.getConnectedUser() + ", key does not exists", e);
			exists = false;
		} catch (AccessDeniedException e) {
			logger.log(Level.FINE, "unable to read profile for user " + view.getConnectedUser() + ", access denied", e);
			readable = false;
		} 
	}
	
	@Override
	public String getName() {
		logger.log(Level.INFO, "retreive name: " + view.getConnectedUser());
		return view.getConnectedUser();
	}
	
	@Override
	public String getAbsolutePath() {
		logger.log(Level.INFO, "retreive absolute path: ");
		return "/";
	}

	@Override
	public String getOwner() {
		logger.log(Level.INFO, "retreive owner : " + view.getConnectedUser());
		return view.getConnectedUser();
	}

	@Override
	public boolean isDirectory() {
		logger.log(Level.INFO, "check if isDirectory: true");
		return true;
	}

	@Override
	public boolean isFile() {
		logger.log(Level.INFO, "check if isFile: false");
		return false;
	}

	@Override
	public boolean doesExist() {
		logger.log(Level.INFO, "check if doesExists: " + exists);
		return exists;
	}

	@Override
	public boolean isReadable() {
		logger.log(Level.INFO, "check if isReadable: " + readable);
		return readable;
	}

	@Override
	public boolean isWritable() {
		logger.log(Level.INFO, "check if isWritable: false");
		return false;
	}

	@Override
	public boolean isExecutable() {
		logger.log(Level.INFO, "check if isExecutable: true"); 
		return true;
	}

	@Override
	public boolean isRemovable() {
		logger.log(Level.INFO, "check if isRemovable: false"); 
		return false;
	}

	@Override
	public SshFile getParentFile() {
		logger.log(Level.INFO, "retreive parent file: this"); 
		return this;
	}
	
	@Override
	public long getLastModified() {
		logger.log(Level.INFO, "retreive last modified: 0");
		return 0;
	}

	@Override
	public boolean setLastModified(long time) {
		logger.log(Level.INFO, "set last modified: " + time);
		return false;
	}

	@Override
	public long getSize() {
		logger.log(Level.INFO, "get size: 0");
		return 0;
	}

	@Override
	public boolean mkdir() {
		logger.log(Level.INFO, "mkdir called");
		return false;
	}

	@Override
	public boolean delete() {
		logger.log(Level.INFO, "delete called");
		return false;
	}

	@Override
	public boolean create() throws IOException {
		logger.log(Level.INFO, "create called");
		return false;
	}

	@Override
	public void truncate() throws IOException {
		logger.log(Level.INFO, "truncate called");
	}

	@Override
	public boolean move(SshFile destination) {
		logger.log(Level.INFO, "move called to : " + destination.getAbsolutePath());
		return false;
	}

	@Override
	public List<SshFile> listSshFiles() {
		logger.log(Level.INFO, "listing user workspaces");
		List<SshFile> children = new ArrayList<SshFile>();
		for ( String workspace : workspaces ) {
			children.add(view.getFile(this, workspace));
		}
		return children;
	}

	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		logger.log(Level.INFO, "create ouput stream called with offset : " + offset);
		throw new UnsupportedOptionException();
	}

	@Override
	public InputStream createInputStream(long offset) throws IOException {
		logger.log(Level.INFO, "create input stream called with offset : " + offset);
		throw new UnsupportedOptionException();
	}

	@Override
	public void handleClose() throws IOException {
		logger.log(Level.INFO, "handle close called");
		//Noop
	}
	
	@Override
	public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
		logger.log(Level.INFO, "trying to get attributes ");
		Map<Attribute, Object> map = new HashMap<Attribute, Object>();
        map.put(Attribute.Size, getSize());
        map.put(Attribute.IsDirectory, isDirectory());
        map.put(Attribute.IsRegularFile, isFile());
        map.put(Attribute.IsSymbolicLink, false);
        map.put(Attribute.LastModifiedTime, getLastModified());
        map.put(Attribute.LastAccessTime, getLastModified());
        map.put(Attribute.Owner, view.getConnectedUser());
        map.put(Attribute.Group, view.getConnectedUser());
        EnumSet<Permission> p = EnumSet.noneOf(Permission.class);
        if (isReadable()) {
            p.add(Permission.UserRead);
            //p.add(Permission.GroupRead);
            //p.add(Permission.OthersRead);
        }
        if (isWritable()) {
            p.add(Permission.UserWrite);
            //p.add(Permission.GroupWrite);
            //p.add(Permission.OthersWrite);
        }
        if (isExecutable()) {
            p.add(Permission.UserExecute);
            //p.add(Permission.GroupExecute);
            //p.add(Permission.OthersExecute);
        }
        map.put(Attribute.Permissions, p);
        return map;
	}

	@Override
	public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
		logger.log(Level.INFO, "trying to set attributes ");
		if ( !attributes.isEmpty() ) {
			throw new UnsupportedOptionException();
		}
	}

	@Override
	public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
		logger.log(Level.INFO, "trying to get attribute : " + attribute.name());
		return getAttributes(followLinks).get(attribute);
	}

	@Override
	public void setAttribute(Attribute attribute, Object value) throws IOException {
		logger.log(Level.INFO, "trying to set attribute : " + attribute.name());
		Map<Attribute, Object> map = new HashMap<Attribute, Object> ();
		map.put(attribute, value);
		setAttributes(map);
	}

	@Override
	public String readSymbolicLink() throws IOException {
		logger.log(Level.INFO, "trying to read symlink ");
		throw new UnsupportedOptionException();
	}

	@Override
	public void createSymbolicLink(SshFile destination) throws IOException {
		logger.log(Level.INFO, "trying to create symlink for destination : " + destination.getAbsolutePath());
		throw new UnsupportedOptionException();
	}

}
