package fr.ortolang.diffusion.ftp.filesystem;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.AliasNotFoundException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.PathNotFoundException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class OrtolangFileSystemView implements FileSystemView {
	
    private static final Logger LOGGER = Logger.getLogger(OrtolangFileSystemView.class.getName());
    
    private static CoreService core;
    private static RegistryService registry;
    private static SecurityService security;
    private static BinaryStoreService store;
    
    private User user;
	private PathBuilder current;
	
	public OrtolangFileSystemView(User user) {
	    LOGGER.log(Level.FINE, "creating new filesystem view for user: " + user.getName());
		this.user = user;
		current = PathBuilder.newInstance();
	}

	protected CoreService getCoreService() throws OrtolangException {
        if (core == null) {
            core = (CoreService) OrtolangServiceLocator.findService(CoreService.SERVICE_NAME);
        }
        return core;
    }

	protected RegistryService getRegistryService() throws OrtolangException {
        if (registry == null) {
            registry = (RegistryService) OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME, RegistryService.class);
        }
        return registry;
    }

	protected SecurityService getSecurityService() throws OrtolangException {
        if (security == null) {
            security = (SecurityService) OrtolangServiceLocator.findService(SecurityService.SERVICE_NAME);
        }
        return security;
    }

	protected BinaryStoreService getBinaryStore() throws OrtolangException {
        if (store == null) {
            store = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME, BinaryStoreService.class);
        }
        return store;
    }
    
    @Override
	public FtpFile getHomeDirectory() throws FtpException {
	    LOGGER.log(Level.FINE, "getting home directory");
		OrtolangHomeFile home = new OrtolangHomeFile(this, user);
		return home;
	}

	@Override
	public FtpFile getWorkingDirectory() throws FtpException {
	    LOGGER.log(Level.FINE, "getting working directory");
	    LOGGER.log(Level.FINE, "current path: " + current.build());
		if ( current.depth() == 0 ) {
		    LOGGER.log(Level.FINE, "depth is 0 : building the home file");
		    return new OrtolangHomeFile(this, user);
		}
		if ( current.depth() == 1 ) {
		    LOGGER.log(Level.FINE, "depth is 1 : building the workspace file");
		    return new OrtolangWorkspaceFile(this, user, current.part());
		}
		String[] parts = current.buildParts();
		try {
		    LOGGER.log(Level.FINE, "depth is >= 2 : building an object file");
            return new OrtolangCoreFile(this, user, parts[0], parts[1], current.part(), current.clone().relativize(2));
        } catch (InvalidPathException e) {
            throw new FtpException("invalid path", e);
        }
	}

	@Override
	public FtpFile getFile(String path) throws FtpException {
	    LOGGER.log(Level.FINE, "getting file for path: " + path);
	    try {
	        String newpath = "";
	        if ( path.startsWith("~") ) {
	            newpath = path.replace("~", "");
	        } else if ( path.startsWith(PathBuilder.PATH_SEPARATOR) ) {
	            newpath = path;
	        } else {
	            newpath = current.build() + PathBuilder.PATH_SEPARATOR + path;
	        }
	        PathBuilder builder = PathBuilder.fromPath(newpath);
	        if ( builder.depth() == 0 ) {
                return new OrtolangHomeFile(this, user);
            }
            if ( builder.depth() == 1 ) {
                return new OrtolangWorkspaceFile(this, user, builder.part());
            }
            String[] parts = builder.buildParts();
            return new OrtolangCoreFile(this, user, parts[0], parts[1], builder.part(), builder.clone().relativize(2));
        } catch (InvalidPathException e) {
            throw new FtpException("invalid path", e);
        }
	}

	@Override
	public boolean changeWorkingDirectory(String path) throws FtpException {
	    LOGGER.log(Level.FINE, "changing working directory to path: " + path);
	    try {
	        String newpath = "";
            if ( path.startsWith("~") ) {
                newpath = path.replace("~", "");
            } else if ( path.startsWith(PathBuilder.PATH_SEPARATOR) ) {
                newpath = path;
            } else {
                newpath = current.build() + PathBuilder.PATH_SEPARATOR + path;
            }
            PathBuilder builder = PathBuilder.fromPath(newpath);
            if ( builder.depth() <= 2 ) {
                current = builder;
                LOGGER.log(Level.FINE, "working directory set to: " + current.build());
                return true;
            } else {
                try {
                    LoginContext lc = null;
                    if ( !user.getName().equals(MembershipService.UNAUTHENTIFIED_IDENTIFIER) ) {
                        lc = UsernamePasswordLoginContextFactory.createLoginContext(user.getName(), user.getPassword());
                        lc.login();
                    }
                    boolean changed = false;
                    try {
                        String[] parts = builder.buildParts();
                        String wskey = getCoreService().resolveWorkspaceAlias(parts[0]);
                        String key = getCoreService().resolveWorkspacePath(wskey, parts[1], builder.clone().relativize(2).build());
                        OrtolangObjectIdentifier identifier = getRegistryService().lookup(key);
                        if ( identifier.getService().equals(CoreService.SERVICE_NAME) && identifier.getType().equals(Collection.OBJECT_TYPE) ) {
                            current = builder;
                            LOGGER.log(Level.FINE, "working directory set to: " + current.build());
                            changed = true;
                        }
                    } catch ( PathNotFoundException | InvalidPathException | AccessDeniedException | AliasNotFoundException e ) {
                        LOGGER.log(Level.FINE, "unable to change directory", e);
                        return false;
                    } catch (OrtolangException | CoreServiceException | RegistryServiceException | KeyNotFoundException e) {
                        LOGGER.log(Level.SEVERE, "unexpected error while trying to change directory", e);
                        return false;
                    } 
                    if ( lc != null ) {
                        lc.logout();
                    }
                    return changed;
                } catch (LoginException e) {
                    LOGGER.log(Level.SEVERE, "workgin directory unchanged : unable to login with user: " + user.getName(), e);
                    return false;
                } 
            }
        } catch (InvalidPathException e) {
            LOGGER.log(Level.SEVERE, "working directory unchanged : invalid path: " + path, e);
            return false;
        }
	}

	@Override
	public boolean isRandomAccessible() throws FtpException {
		return false;
	}
	
	@Override
	public void dispose() {
	    LOGGER.log(Level.FINE, "disposing filesystem view");

	}
	
}
