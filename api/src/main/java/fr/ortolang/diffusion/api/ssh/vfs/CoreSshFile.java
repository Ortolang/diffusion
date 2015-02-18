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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.sshd.common.file.SshFile;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

public class CoreSshFile implements SshFile {
	
	private static Logger logger = Logger.getLogger(CoreSshFile.class.getName());
	
	private DiffusionFileSystemView view;
	private PathBuilder absolutePath;
	private String workspace;
	private PathBuilder path;
	private boolean exists;
	private boolean readable;
	private boolean writable;
	private boolean executable;
	private boolean removable;
	private String type;
	private String key;
	private String name;
	private long size;
	private Set<CollectionElement> elements;
	private long lastModified;
	private Path temp = null;
	
	protected CoreSshFile(DiffusionFileSystemView view, PathBuilder path) throws OrtolangException {
		logger.log(Level.INFO, "CoreSshFile created for path: " + path.build());
		this.view = view;
		this.path = path;
		this.load();
	}
	
	private void load() throws OrtolangException {
		logger.log(Level.INFO, "loading element : " + path.part());
		try {
			LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(view.getSession().getLogin(), view.getSession().getPassword());
			lc.login();
			absolutePath = path.clone();
			workspace = path.buildParts()[0];
			path.relativize(workspace);
			readable = true;
			writable = true;
			removable = false;
			executable = false;
			key = view.getCore().resolveWorkspacePath(workspace, "", path.build());
			type = view.getBrowser().lookup(key).getType();
			lastModified = view.getBrowser().getInfos(key).getLastModificationDate();
			exists = true;
			if ( type.equals(Collection.OBJECT_TYPE) ) {
				Collection collection = view.getCore().readCollection(key);
				name = collection.getName();
				elements = collection.getElements();
				size = elements.size();
				executable = true;
				removable = true;
				if ( collection.isRoot() ) {
					removable = false;
				}
			}
			if ( type.equals(DataObject.OBJECT_TYPE) ) {
				DataObject object = view.getCore().readDataObject(key);
				name = object.getName();
				size = object.getSize();
				elements = Collections.emptySet();
				removable = true;
			}
			lc.logout();
		} catch (AccessDeniedException | BrowserServiceException | LoginException | OrtolangException | CoreServiceException | KeyNotFoundException e) {
			logger.log(Level.SEVERE, "error while trying to load element for path " + path, e);
			throw new OrtolangException("unable to load core object associated with path: " + path.build(), e);
		} catch (InvalidPathException e) {
			exists = false;
		}
	}

	@Override
	public String getName() {
		logger.log(Level.FINE, "retreive name: " + name);
		return name;
	}
	
	@Override
	public String getAbsolutePath() {
		logger.log(Level.FINE, "retreive absolute path: " + absolutePath.build());
		return absolutePath.build();
	}

	@Override
	public String getOwner() {
		logger.log(Level.FINE, "retreive owner : " + view.getConnectedUser());
		return view.getConnectedUser();
	}

	@Override
	public boolean isDirectory() {
		logger.log(Level.FINE, "check if isDirectory: " + type.equals(Collection.OBJECT_TYPE));
		return type.equals(Collection.OBJECT_TYPE);
	}

	@Override
	public boolean isFile() {
		logger.log(Level.FINE, "check if isFile: " + type.equals(DataObject.OBJECT_TYPE));
		return type.equals(DataObject.OBJECT_TYPE);
	}

	@Override
	public boolean doesExist() {
		logger.log(Level.FINE, "check if doesExists: " + exists);
		return exists;
	}

	@Override
	public boolean isReadable() {
		logger.log(Level.FINE, "check if isReadable: " + readable);
		return readable;
	}

	@Override
	public boolean isWritable() {
		logger.log(Level.FINE, "check if isWritable: " + writable);
		return writable;
	}

	@Override
	public boolean isExecutable() {
		logger.log(Level.FINE, "check if isExecutable: " + executable); 
		return executable;
	}

	@Override
	public boolean isRemovable() {
		logger.log(Level.FINE, "check if isRemovable: " + removable);
		return removable;
	}

	@Override
	public SshFile getParentFile() {
		logger.log(Level.FINE, "retreive parent file calling the view to create it ");
		return view.getFile(path.clone().parent().build());
	}
	
	@Override
	public long getLastModified() {
		logger.log(Level.FINE, "retreive last modified: " + lastModified);
		return lastModified;
	}

	@Override
	public boolean setLastModified(long time) {
		//logger.log(Level.INFO, "set last modified: " + time);
		return false;
	}

	@Override
	public long getSize() {
		logger.log(Level.FINE, "get size: " + size);
		return size;
	}

	@Override
	public boolean mkdir() {
		logger.log(Level.FINE, "mkdir called");
		if ( !exists ) {
			try {
				LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(view.getSession().getLogin(), view.getSession().getPassword());
				lc.login();
				view.getCore().createCollection(workspace, path.build(), "pas de description");
				//TODO maybe reload informations...
				lc.logout();
				return true;
			} catch ( LoginException | CoreServiceException | KeyNotFoundException | InvalidPathException | AccessDeniedException | OrtolangException e ) {
				logger.log(Level.SEVERE, "error while trying to make directory for path: " + path, e); 
			}
		}
		return false;
	}

	@Override
	public boolean delete() {
		logger.log(Level.FINE, "delete called");
		if ( exists ) {
			try {
				LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(view.getSession().getLogin(), view.getSession().getPassword());
				lc.login();
				if ( type.equals(Collection.OBJECT_TYPE) ) {
					view.getCore().deleteCollection(workspace, path.build());
				}
				if ( type.equals(DataObject.OBJECT_TYPE) ) {
					view.getCore().deleteDataObject(workspace, path.build());
				}
				//TODO maybe reload informations...
				lc.logout();
				return true;
			} catch ( LoginException | CoreServiceException | KeyNotFoundException | InvalidPathException | AccessDeniedException | OrtolangException e ) {
				logger.log(Level.SEVERE, "error while trying to delete path: " + path, e); 
			}
		}
		return false;
	}

	@Override
	public boolean create() throws IOException {
		logger.log(Level.FINE, "create called");
		if ( !exists ) {
			try {
				LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(view.getSession().getLogin(), view.getSession().getPassword());
				lc.login();
				view.getCore().createDataObject(workspace, path.build(), "pas de description", "");
				//TODO maybe reload informations...
				lc.logout();
				return true;
			} catch ( LoginException | CoreServiceException | KeyNotFoundException | InvalidPathException | AccessDeniedException | OrtolangException e ) {
				logger.log(Level.SEVERE, "error while trying to make directory for path: " + path, e); 
			}
		}
		return false;
	}

	@Override
	public void truncate() throws IOException {
		logger.log(Level.FINE, "truncate called");
		if ( exists ) {
			try {
				LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(view.getSession().getLogin(), view.getSession().getPassword());
				lc.login();
				view.getCore().updateDataObject(workspace, path.build(), "pas de description", "");
				//TODO maybe reload informations...
				lc.logout();
			} catch ( LoginException | CoreServiceException | KeyNotFoundException | InvalidPathException | AccessDeniedException | OrtolangException e ) {
				logger.log(Level.SEVERE, "error while trying to make directory for path: " + path, e); 
			}
		}
	}

	@Override
	public boolean move(SshFile destination) {
		logger.log(Level.FINE, "move called to : " + destination.getAbsolutePath());
		//TODO
		return false;
	}

	@Override
	public List<SshFile> listSshFiles() {
		logger.log(Level.FINE, "listing element children depending on type of element");
		List<SshFile> children = new ArrayList<SshFile>();
		for ( CollectionElement element : elements ) {
			if (element.getType().equals(Collection.OBJECT_TYPE) || element.getType().equals(DataObject.OBJECT_TYPE)) {
				children.add(view.getFile(this, element.getName()));
			}
		}
		return children;
	}
	
	@Override
	public OutputStream createOutputStream(long offset) throws IOException {
		logger.log(Level.FINE, "create ouput stream called with offset : " + offset);
		if ( !isWritable() ) {
			throw new IOException("No write permission for DataObject");
		}
		//TODO create an output stream in a temporary file but needs to create the data object at the end of this creation (maybe handle close called)...
		temp = Files.createTempFile("ssh-upload-", ".tmp");
		return Files.newOutputStream(temp);
	}

	@Override
	public InputStream createInputStream(long offset) throws IOException {
		logger.log(Level.FINE, "create input stream called with offset : " + offset);
		if ( !exists ) {
			throw new IOException("DataObject does not exists");
		}
		if ( !isReadable() ) {
			throw new IOException("No read permission for DataObject");
		}
		try {
			LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(view.getSession().getLogin(), view.getSession().getPassword());
			lc.login();
			String hash = view.getCore().readDataObject(key).getStream();
			InputStream input = new ByteArrayInputStream("".getBytes());  
			if ( hash.length() > 0 ) {
				input = view.getBinaryStore().get(hash);
			} 
			lc.logout();
			return input;
		} catch ( LoginException | CoreServiceException | KeyNotFoundException | AccessDeniedException | OrtolangException | BinaryStoreServiceException | DataNotFoundException e ) {
			logger.log(Level.SEVERE, "error while trying to make directory for path: " + path, e); 
		}
		return null;
	}

	@Override
	public void handleClose() throws IOException {
		logger.log(Level.FINE, "handle close called");
		if ( temp != null ) {
			logger.log(Level.INFO, "we have a temp file uploaded, maybe update data object according to this !!");
			try {
				LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(view.getSession().getLogin(), view.getSession().getPassword());
				lc.login();
				String hash = view.getBinaryStore().put(Files.newInputStream(temp));
				view.getCore().updateDataObject(workspace, path.build(), "pas de description", hash);
				lc.logout();
			} catch ( LoginException | CoreServiceException | KeyNotFoundException | InvalidPathException | AccessDeniedException | OrtolangException | BinaryStoreServiceException | DataCollisionException e ) {
				logger.log(Level.SEVERE, "error while trying to update data with uploaded content", e); 
			}
			Files.delete(temp);
			temp = null;
		}
	}
	
	@Override
	public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
		logger.log(Level.FINE, "trying to get attributes ");
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
		logger.log(Level.FINE, "trying to set attributes ");
		if ( !attributes.isEmpty() ) {
			//throw new UnsupportedOptionException();
		}
	}

	@Override
	public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
		logger.log(Level.FINE, "trying to get attribute : " + attribute.name());
		return getAttributes(followLinks).get(attribute);
	}

	@Override
	public void setAttribute(Attribute attribute, Object value) throws IOException {
		logger.log(Level.FINE, "trying to set attribute : " + attribute.name());
		Map<Attribute, Object> map = new HashMap<Attribute, Object> ();
		map.put(attribute, value);
		setAttributes(map);
	}

	@Override
	public String readSymbolicLink() throws IOException {
		logger.log(Level.FINE, "trying to read symlink ");
		throw new IOException();
	}

	@Override
	public void createSymbolicLink(SshFile destination) throws IOException {
		logger.log(Level.FINE, "trying to create symlink for destination : " + destination.getAbsolutePath());
		throw new IOException();
	}

}
