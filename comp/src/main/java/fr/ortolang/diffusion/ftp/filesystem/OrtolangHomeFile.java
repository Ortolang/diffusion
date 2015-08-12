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

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class OrtolangHomeFile implements FtpFile {

    private static final Logger LOGGER = Logger.getLogger(OrtolangHomeFile.class.getName());
    
    private User user;
    private List<FtpFile> content;

    public OrtolangHomeFile(OrtolangFileSystemView fsview, User user) throws FtpException {
        LOGGER.log(Level.FINE, "creating new ortolang home file");
        this.user = user;
        List<FtpFile> content = new ArrayList<FtpFile>();
        if ( !user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
            try {
                LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                lc.login();
                try {
                    List<String> aliases = fsview.getCoreService().findWorkspacesAliasForProfile(user.getName());
                    for ( String alias : aliases ) {
                        try {
                            content.add(new OrtolangBaseFile(PathBuilder.newInstance().path(alias), alias, true, true));
                        } catch ( InvalidPathException e ) {
                            LOGGER.log(Level.SEVERE, "error while building base file view for alias " + alias + " and user " + user.getName(), e);
                        }
                    }
                } catch (OrtolangException | CoreServiceException | KeyNotFoundException | AccessDeniedException e) {
                    LOGGER.log(Level.SEVERE, "error while trying to list workspaces for user " + user.getName(), e);
                    throw new FtpException("error while trying to list workspaces for user " + user.getName(), e);
                }
                if ( lc != null ) {
                    lc.logout();
                }
            } catch ( LoginException e ) {
                LOGGER.log(Level.SEVERE, "unable to login with user: " + user.getName(), e);
                throw new FtpException("unable to login with user: " + user.getName(), e);
            }
        } 
    }
    
    @Override
    public String getName() {
        return "/";
    }

    @Override
    public String getOwnerName() {
        return user.getName();
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
        return user.getName();
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
        throw new IOException("unable to create input stream for home");
    }

    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        throw new IOException("unable to create output stream for home");
    }

}
