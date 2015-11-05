package fr.ortolang.diffusion.ftp.filesystem;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.core.AliasNotFoundException;
import fr.ortolang.diffusion.core.CollectionNotEmptyException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathAlreadyExistsException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.PathNotFoundException;
import fr.ortolang.diffusion.core.WorkspaceReadOnlyException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

public class OrtolangCoreFile implements FtpFile {

    private static final Logger LOGGER = Logger.getLogger(OrtolangCoreFile.class.getName());

    private OrtolangFileSystemView fsview;
    private User user;
    private String alias;
    private String root;
    private PathBuilder path;
    private OrtolangObject object;
    private String wskey;
    private String key;
    private String name;
    private String owner;
    private String group;
    private long lastModified;
    private long size;

    public OrtolangCoreFile(OrtolangFileSystemView fsview, User user, String alias, String snapshot, String name) throws FtpException {
        this(fsview, user, alias, snapshot, name, PathBuilder.newInstance());
    }

    public OrtolangCoreFile(OrtolangFileSystemView fsview, User user, String alias, String root, String name, PathBuilder path) throws FtpException {
        LOGGER.log(Level.FINE, "creating object: alias=" + alias + ", root=" + root + ", name=" + name + ", path=" + path.build());
        this.fsview = fsview;
        this.user = user;
        this.alias = alias;
        this.root = root;
        this.name = name;
        this.path = path;
        this.owner = "user";
        this.group = alias + "-members";
        this.lastModified = 0;
        this.size = 0;
        try {
            LoginContext lc = null;
            if (!user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                lc.login();
            }
            try {
                wskey = fsview.getCoreService().resolveWorkspaceAlias(alias);
                key = fsview.getCoreService().resolveWorkspacePath(wskey, root, path.build());
                object = fsview.getCoreService().findObject(key);
                owner = fsview.getSecurityService().getOwner(key);
                lastModified = fsview.getRegistryService().getLastModificationDate(key);
                LOGGER.log(Level.FINE, "object loaded: key= " + key + ", identifier=" + object.getObjectIdentifier());
                if (object instanceof DataObject) {
                    size = ((DataObject) object).getSize();
                }
            } catch (OrtolangException | CoreServiceException | KeyNotFoundException | SecurityServiceException | RegistryServiceException e) {
                LOGGER.log(Level.SEVERE, "unexpected error while trying to load core object", e);
                throw new FtpException("unexpected error while trying to load core object", e);
            } catch (AccessDeniedException e) {
                throw new FtpException("access is denied to this core object", e);
            } catch (AliasNotFoundException e) {
                throw new FtpException("alias not found in workspaces", e);
            } catch (InvalidPathException e) {
                throw new FtpException("invalid path: " + path.build(), e);
            } catch (PathNotFoundException e) {
                LOGGER.log(Level.FINE, "path does not exists, building empty object");
            }
            if (lc != null) {
                lc.logout();
            }
        } catch (LoginException e) {
            LOGGER.log(Level.SEVERE, "unable to login with user: " + user.getName(), e);
            throw new FtpException("unable to login with user: " + user.getName(), e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOwnerName() {
        return owner;
    }

    @Override
    public boolean doesExist() {
        return object != null;
    }

    @Override
    public String getAbsolutePath() {
        try {
            return PathBuilder.newInstance().path(alias).path(root).path(path).build();
        } catch (InvalidPathException e) {
            LOGGER.log(Level.SEVERE, "unable to generate absolute path", e);
            return null;
        }
    }

    @Override
    public String getGroupName() {
        return group;
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
        return size;
    }

    @Override
    public boolean isDirectory() {
        return (doesExist() && object.getObjectIdentifier().getType().equals(Collection.OBJECT_TYPE));
    }

    @Override
    public boolean isFile() {
        return (doesExist() && object.getObjectIdentifier().getType().equals(DataObject.OBJECT_TYPE));
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isReadable() {
        if (doesExist()) {
            try {
                LoginContext lc = null;
                if (!user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                    lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                    lc.login();
                }
                try {
                    fsview.getSecurityService().checkPermission(key, "download");
                } catch (AccessDeniedException | SecurityServiceException | KeyNotFoundException | OrtolangException e) {
                    return false;
                }
                if (lc != null) {
                    lc.logout();
                }
            } catch (LoginException e) {
                LOGGER.log(Level.SEVERE, "unable to login with user: " + user.getName(), e);
            }
        }
        return true;
    }

    @Override
    public boolean isRemovable() {
        if (root.equals(Workspace.HEAD) && doesExist()) {
            try {
                LoginContext lc = null;
                if (!user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                    lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                    lc.login();
                }
                try {
                    fsview.getSecurityService().checkPermission(key, "delete");
                } catch (AccessDeniedException | SecurityServiceException | KeyNotFoundException | OrtolangException e) {
                    return false;
                }
                if (lc != null) {
                    lc.logout();
                }
            } catch (LoginException e) {
                LOGGER.log(Level.SEVERE, "unable to login with user: " + user.getName(), e);
            }
        }
        return false;
    }

    @Override
    public boolean isWritable() {
        if (root.equals(Workspace.HEAD)) {
            return true;
        }
        return false;
    }

    @Override
    public List<FtpFile> listFiles() {
        LOGGER.log(Level.FINE, "listing files");
        if (doesExist() && object.getObjectIdentifier().getType().equals(Collection.OBJECT_TYPE)) {
            List<FtpFile> content = new ArrayList<FtpFile>();
            for (CollectionElement element : ((Collection) object).getElements()) {
                try {
                    content.add(new OrtolangBaseFile(PathBuilder.newInstance().path(alias).path(root).path(path).path(element.getName()), element.getName(), element.getType().equals(
                            Collection.OBJECT_TYPE), true, true, true, element.getModification(), element.getSize()));
                } catch (InvalidPathException e) {
                    LOGGER.log(Level.SEVERE, "unable to create base file for element: " + element.getName());
                }
            }
            return content;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean mkdir() {
        LOGGER.log(Level.FINE, "making directory");
        if (root.equals(Workspace.HEAD) && !doesExist()) {
            try {
                LoginContext lc = null;
                if (!user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                    lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                    lc.login();
                }
                try {
                    fsview.getCoreService().createCollection(wskey, path.build());
                } catch (AccessDeniedException | KeyNotFoundException | OrtolangException | CoreServiceException | InvalidPathException | PathNotFoundException | PathAlreadyExistsException | WorkspaceReadOnlyException e) {
                    return false;
                }
                if (lc != null) {
                    lc.logout();
                }
                return true;
            } catch (LoginException e) {
                LOGGER.log(Level.SEVERE, "unable to login with user: " + user.getName(), e);
            }
        }
        return false;
    }

    @Override
    public boolean move(FtpFile destination) {
        LOGGER.log(Level.FINE, "moving file");
        if (root.equals(Workspace.HEAD) && !doesExist()) {
            try {
                LoginContext lc = null;
                if (!user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                    lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                    lc.login();
                }
                try {
                    PathBuilder dest = PathBuilder.fromPath(destination.getAbsolutePath());
                    if (dest.depth() < 2 || !dest.buildParts()[0].equals(alias) || !dest.buildParts()[1].equals(Workspace.HEAD)) {
                        throw new InvalidPathException("destination must be in the same workspace head.");
                    }
                    if (isDirectory()) {
                        fsview.getCoreService().moveCollection(wskey, path.build(), dest.relativize(2).build());
                    } else {
                        fsview.getCoreService().moveDataObject(wskey, path.build(), dest.relativize(2).build());
                    }
                } catch (AccessDeniedException | KeyNotFoundException | OrtolangException | CoreServiceException | InvalidPathException | PathNotFoundException | PathAlreadyExistsException | WorkspaceReadOnlyException e) {
                    return false;
                }
                if (lc != null) {
                    lc.logout();
                }
                return true;
            } catch (LoginException e) {
                LOGGER.log(Level.SEVERE, "unable to login with user: " + user.getName(), e);
            }
        }
        return false;
    }

    @Override
    public boolean delete() {
        LOGGER.log(Level.FINE, "deleting file");
        if (doesExist()) {
            try {
                LoginContext lc = null;
                if (!user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                    lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                    lc.login();
                }
                try {
                    if (object.getObjectIdentifier().getType().equals(Collection.OBJECT_TYPE)) {
                        fsview.getCoreService().deleteCollection(wskey, path.build());
                    }
                    if (object.getObjectIdentifier().getType().equals(DataObject.OBJECT_TYPE)) {
                        fsview.getCoreService().deleteDataObject(wskey, path.build());
                    }
                    key = null;
                    object = null;
                    return true;
                } catch (InvalidPathException | AccessDeniedException | OrtolangException | CoreServiceException | KeyNotFoundException | CollectionNotEmptyException | PathNotFoundException | WorkspaceReadOnlyException e) {
                    LOGGER.log(Level.SEVERE, "unable to delete object", e);
                }
                if (lc != null) {
                    lc.logout();
                }
            } catch (LoginException e) {
                LOGGER.log(Level.SEVERE, "unable to login with user: " + user.getName(), e);
            }
        }
        return false;
    }

    @Override
    public boolean setLastModified(long lastmodified) {
        LOGGER.log(Level.FINE, "setting last modified");
        return false;
    }

    @Override
    public InputStream createInputStream(long offset) throws IOException {
        LOGGER.log(Level.FINE, "getting input stream");
        if (offset != 0) {
            throw new IOException("random access is not supported");
        }
        if (doesExist() && isFile()) {
            try {
                return fsview.getBinaryStore().get(((DataObject) object).getStream());
            } catch (DataNotFoundException | BinaryStoreServiceException | OrtolangException e) {
                throw new IOException("unable to access binary data", e);
            }
        } else {
            throw new IOException("object does not exists or is not a file");
        }
    }

    @Override
    public OutputStream createOutputStream(long offset) throws IOException {
        LOGGER.log(Level.FINE, "getting output stream");
        if (root.equals(Workspace.HEAD)) {
            LOGGER.log(Level.FINE, "snapshot is " + Workspace.HEAD + ", ready to create temporary file for upload");
            Path upload = Files.createTempFile("ftp", ".up");
            OutputStream os = Files.newOutputStream(upload, StandardOpenOption.WRITE);
            LOGGER.log(Level.FINE, "output stream created on temporary file: " + upload);
            BufferedOutputStream bof = new BufferedOutputStream(os) {
                @Override
                public void close() throws IOException {
                    LOGGER.log(Level.FINE, "closing outputstream, time to update dataobject !!");
                    flush();
                    super.close();
                    try {
                        LoginContext lc = null;
                        if (!user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                            lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                            lc.login();
                        }
                        try {
                            String hash = fsview.getBinaryStore().put(Files.newInputStream(upload));
                            LOGGER.log(Level.FINE, "uploaded file content inserted in binary store");
                            if ( doesExist() ) {
                                fsview.getCoreService().updateDataObject(wskey, path.build(), hash);
                            } else {
                                fsview.getCoreService().createDataObject(wskey, path.build(), hash);
                            }
                        } catch (AccessDeniedException | KeyNotFoundException | OrtolangException | CoreServiceException | InvalidPathException | BinaryStoreServiceException | DataCollisionException | PathNotFoundException | PathAlreadyExistsException | WorkspaceReadOnlyException e) {
                            throw new IOException("error during propagating files data to underlying object", e);
                        }
                        if (lc != null) {
                            lc.logout();
                        }
                    } catch (LoginException e) {
                        LOGGER.log(Level.SEVERE, "unable to login with user: " + user.getName(), e);
                    }
                    Files.delete(upload);
                }
            };
            return bof;
        } else {
            LOGGER.log(Level.FINE, "snapshot is NOT " + Workspace.HEAD + ", aborting upload");
            throw new IOException("writing is only allowed in head");
        }
    }

}
