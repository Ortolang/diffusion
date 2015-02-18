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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.api.ssh.session.SSHSession;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class DiffusionFileSystemView implements FileSystemView {
	
	private static Logger logger = Logger.getLogger(DiffusionFileSystemView.class.getName());
	
	private SSHSession session;
	private CoreService core;
	private BrowserService browser;
	private BinaryStoreService binary;
	
	public DiffusionFileSystemView(SSHSession session) {
		logger.log(Level.INFO, "DiffusionFileSystemView created for user: " + session.getUsername());
		this.session = session;
	}

	@Override
	public SshFile getFile(String file) {
		logger.log(Level.FINE, "Getting file: " + file);
		try {
			return loadFile(PathBuilder.fromPath(file));
		} catch (InvalidPathException | OrtolangException e) {
			//TODO how is it possible to send errors to client ??
			logger.log(Level.WARNING, "Unable to get file: " + file, e);
			return null;
		}
	}

	@Override
	public SshFile getFile(SshFile baseDir, String file) {
		logger.log(Level.FINE, "Getting file with base dir: " + baseDir.getAbsolutePath() + " and file: " + file);
		try {
			return loadFile(PathBuilder.fromPath(baseDir.getAbsolutePath()).path(file));
		} catch (InvalidPathException | OrtolangException e) {
			//TODO how is it possible to send errors to client ??
			logger.log(Level.WARNING, "Unable to get file: " + file, e);
			return null;
		}
	}

	@Override
	public FileSystemView getNormalizedView() {
		logger.log(Level.FINE, "Getting normalized view");
		return this;
	}
	
	protected SshFile loadFile(PathBuilder path) throws OrtolangException {
		logger.log(Level.FINE, "Loading file: " + path.build());
       	SshFile file = null;
		if ( path.isRoot() ) {
			logger.log(Level.FINE, "RootLevel, loading ProfileSshFile");
			file = new ProfileSshFile(this);
		} else if ( path.depth() == 1 ) {
			logger.log(Level.FINE, "WorkspaceLevel, loading WorkspaceSshFile with name: " + path.part());
			file = new WorkspaceSshFile(this, path);
		} else {
			logger.log(Level.FINE, "CoreLevel, loading CoreSshFile with path: " + path.build());
			file = new CoreSshFile(this, path);
		}
		return file;
	}
	
	public String getConnectedUser() {
		return session.getUsername();
	}
	
	public SSHSession getSession() {
		return session;
	}
	
	public CoreService getCore() throws OrtolangException {
		if ( core == null ) {
			core = (CoreService) OrtolangServiceLocator.findService("core");
		}
		return core;
	}
	
	public BinaryStoreService getBinaryStore() throws OrtolangException {
		if ( binary == null ) {
			binary = (BinaryStoreService) OrtolangServiceLocator.lookup("binary-store");
		}
		return binary;
	}

	public BrowserService getBrowser() throws OrtolangException {
		if ( browser == null ) {
			browser = (BrowserService) OrtolangServiceLocator.findService("browser");
		}
		return browser;
	}
	
}
