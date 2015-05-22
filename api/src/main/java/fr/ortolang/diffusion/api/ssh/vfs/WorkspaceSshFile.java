package fr.ortolang.diffusion.api.ssh.vfs;

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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.sshd.common.file.SshFile;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.AliasNotFoundException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class WorkspaceSshFile implements SshFile {
	
	private static final Logger LOGGER = Logger.getLogger(WorkspaceSshFile.class.getName());
	
	private DiffusionFileSystemView view;
	private PathBuilder path;
	private Workspace ws;
	private Collection head;
	private long lastModified;
	private boolean exists;
	private boolean readable;
	
	protected WorkspaceSshFile(DiffusionFileSystemView view, PathBuilder path) throws OrtolangException {
		LOGGER.log(Level.FINE, "WorkspaceSshFile created for path: " + path.build());
		this.view = view;
		this.path = path;
		this.load();
	}
	
	private void load() throws OrtolangException {
		LOGGER.log(Level.FINE, "loading workspace : " + path.part());
		try {
			LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(view.getSession().getLogin(), view.getSession().getPassword());
			lc.login();
			String wskey = view.getCore().resolveWorkspaceAlias(path.part());
			ws = view.getCore().readWorkspace(wskey);
			head = view.getCore().readCollection(ws.getHead());
			exists = true;
			readable = true;
			lastModified = view.getBrowser().getInfos(ws.getKey()).getLastModificationDate();
			lc.logout();
		} catch (BrowserServiceException | LoginException | OrtolangException | CoreServiceException e) {
			LOGGER.log(Level.SEVERE, "error while trying to load workspace for path " + path, e);
			throw new OrtolangException("error while trying to load workspace for path " + path, e);
		} catch (AliasNotFoundException e) {
			LOGGER.log(Level.WARNING, "unable to read workspace for path " + path + ", alias not found");
			exists = false;
		} catch (KeyNotFoundException e) {
			LOGGER.log(Level.WARNING, "unable to read workspace for path " + path + ", key not found");
			exists = false;
		} catch (AccessDeniedException e) {
			LOGGER.log(Level.FINE, "unable to read workspace for path " + path + ", access denied");
			readable = false;
		} 
	}

	@Override
	public String getName() {
		LOGGER.log(Level.FINE, "retreive name: " + ws.getAlias());
		return ws.getAlias();
	}
	
	@Override
	public String getAbsolutePath() {
		LOGGER.log(Level.FINE, "retreive absolute path: " + path.build());
		return path.build();
	}

	@Override
	public String getOwner() {
		LOGGER.log(Level.FINE, "retreive owner : " + view.getConnectedUser());
		return view.getConnectedUser();
	}

	@Override
	public boolean isDirectory() {
		LOGGER.log(Level.FINE, "check if isDirectory: true");
		return true;
	}

	@Override
	public boolean isFile() {
		LOGGER.log(Level.FINE, "check if isFile: false");
		return false;
	}

	@Override
	public boolean doesExist() {
		LOGGER.log(Level.FINE, "check if doesExists: " + exists );
		return exists;
	}

	@Override
	public boolean isReadable() {
		LOGGER.log(Level.FINE, "check if isReadable: " + readable);
		return readable;
	}

	@Override
	public boolean isWritable() {
		LOGGER.log(Level.FINE, "check if isWritable: false");
		return false;
	}

	@Override
	public boolean isExecutable() {
		LOGGER.log(Level.FINE, "check if isExecutable: true"); 
		return true;
	}

	@Override
	public boolean isRemovable() {
		LOGGER.log(Level.FINE, "check if isRemovable: false"); 
		return false;
	}

	@Override
	public SshFile getParentFile() {
		LOGGER.log(Level.FINE, "retreive parent file calling the view to create it ");
		return view.getFile(path.clone().parent().build());
	}
	
	@Override
	public long getLastModified() {
		LOGGER.log(Level.FINE, "retreive last modified: " + lastModified);
		return lastModified;
	}

	@Override
	public boolean setLastModified(long time) {
		LOGGER.log(Level.FINE, "set last modified: " + time);
		return false;
	}

	@Override
	public long getSize() {
		LOGGER.log(Level.FINE, "get size: 0");
		return 0;
	}

	@Override
	public boolean mkdir() {
		LOGGER.log(Level.FINE, "mkdir called");
		return false;
	}

	@Override
	public boolean delete() {
		LOGGER.log(Level.FINE, "delete called");
		return false;
	}

	@Override
	public boolean create() throws IOException {
		LOGGER.log(Level.FINE, "create called");
		return false;
	}

	@Override
	public void truncate() throws IOException {
		LOGGER.log(Level.FINE, "truncate called");
	}

	@Override
	public boolean move(SshFile destination) {
		LOGGER.log(Level.FINE, "move called to : " + destination.getAbsolutePath());
		return false;
	}

	@Override
	public List<SshFile> listSshFiles() {
		LOGGER.log(Level.FINE, "listing workspace head elements");
		List<SshFile> children = new ArrayList<SshFile>();
		for ( CollectionElement element : head.getElements() ) {
			children.add(view.getFile(this, element.getName()));
		}
		return children;
	}
	
	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		LOGGER.log(Level.FINE, "create ouput stream called with offset : " + offset);
		throw new IOException();
	}

	@Override
	public InputStream createInputStream(long offset) throws IOException {
		LOGGER.log(Level.FINE, "create input stream called with offset : " + offset);
		throw new IOException();
	}

	@Override
	public void handleClose() throws IOException {
		LOGGER.log(Level.FINE, "handle close called");
		//Noop
	}
	
	@Override
	public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
		LOGGER.log(Level.FINE, "trying to get attributes ");
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
            p.add(Permission.GroupRead);
            //p.add(Permission.OthersRead);
        }
        if (isWritable()) {
            p.add(Permission.UserWrite);
            p.add(Permission.GroupWrite);
            //p.add(Permission.OthersWrite);
        }
        if (isExecutable()) {
            p.add(Permission.UserExecute);
            p.add(Permission.GroupExecute);
            //p.add(Permission.OthersExecute);
        }
        map.put(Attribute.Permissions, p);
        return map;
	}

	@Override
	public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
		LOGGER.log(Level.FINE, "trying to set attributes ");
		if ( !attributes.isEmpty() ) {
			throw new IOException();
		}
	}

	@Override
	public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
		LOGGER.log(Level.FINE, "trying to get attribute : " + attribute.name());
		return getAttributes(followLinks).get(attribute);
	}

	@Override
	public void setAttribute(Attribute attribute, Object value) throws IOException {
		LOGGER.log(Level.FINE, "trying to set attribute : " + attribute.name());
		Map<Attribute, Object> map = new HashMap<Attribute, Object> ();
		map.put(attribute, value);
		setAttributes(map);
	}

	@Override
	public String readSymbolicLink() throws IOException {
		LOGGER.log(Level.FINE, "trying to read symlink ");
		throw new IOException();
	}

	@Override
	public void createSymbolicLink(SshFile destination) throws IOException {
		LOGGER.log(Level.FINE, "trying to create symlink for destination : " + destination.getAbsolutePath());
		throw new IOException();
	}

}
