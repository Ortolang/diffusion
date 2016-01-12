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
