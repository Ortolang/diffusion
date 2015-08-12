package fr.ortolang.diffusion.ftp.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.TagElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;

public class OrtolangWorkspaceFile implements FtpFile {

    private static final Logger LOGGER = Logger.getLogger(OrtolangWorkspaceFile.class.getName());
    
    private Workspace workspace;
	private String owner;
	private long lastModified;
	private User user;
	private String alias;
	
	public OrtolangWorkspaceFile(OrtolangFileSystemView fsview, User user, String alias) throws FtpException {
	    LOGGER.log(Level.FINE, "loading workspace");
	    this.user = user;
	    this.alias = alias;
	    try {
	        LoginContext lc = null;
            if ( !user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
                lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                lc.login();
            }
            try {
                String wskey = fsview.getCoreService().resolveWorkspaceAlias(alias);
                workspace = fsview.getCoreService().readWorkspace(wskey);
                owner = fsview.getSecurityService().getOwner(wskey);
                lastModified = fsview.getRegistryService().getLastModificationDate(wskey);
            } catch ( Exception e ) {
                LOGGER.log(Level.SEVERE, "error while trying to load workspace with alias " + alias, e);
                throw new FtpException("error while trying to load workspace with alias " + alias, e);
            }
            if ( lc != null ) {
                lc.logout();
            }
        } catch (LoginException e) {
            LOGGER.log(Level.SEVERE, "unable to login with user: " + user.getName(), e);
            throw new FtpException("unable to login with user: " + user.getName(), e);
        }
	}

	@Override
	public String getName() {
		return workspace.getAlias();
	}

	@Override
	public String getOwnerName() {
		return owner;
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
		return "/" + workspace.getAlias();
	}

	@Override
	public String getGroupName() {
	    return workspace.getMembers();
	}

	@Override
	public long getLastModified() {
	    return lastModified;
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
	    List<FtpFile> content = new ArrayList<FtpFile>();
	    for ( TagElement tag : workspace.getTags() ) {
	        try {
	            content.add(new OrtolangBaseFile(PathBuilder.newInstance().path(alias).path(tag.getName()), tag.getName(), true, true));
	        } catch ( InvalidPathException e ) {
	            LOGGER.log(Level.SEVERE, "unable to create base file for tag: " + tag.getName());
	        }
        }
	    if ( !user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
	        for ( SnapshotElement snapshot : workspace.getSnapshots() ) {
	            try {
	                content.add(new OrtolangBaseFile(PathBuilder.newInstance().path(alias).path(snapshot.getName()), snapshot.getName(), true, true));
	            } catch ( InvalidPathException e ) {
	                LOGGER.log(Level.SEVERE, "unable to create base file for snapshot: " + snapshot.getName());
	            }
	        }
	        try {
                content.add(new OrtolangBaseFile(PathBuilder.newInstance().path(alias).path(Workspace.HEAD), Workspace.HEAD, true, true));
	        } catch ( InvalidPathException e ) {
                LOGGER.log(Level.SEVERE, "unable to create base file for snapshot: " + Workspace.HEAD);
            }
	    }
	    return content;
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
		throw new IOException("unable to create input stream for workspace");
	}

	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		throw new IOException("unable to create output stream for workspace");
	}

}
