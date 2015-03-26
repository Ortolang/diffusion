package fr.ortolang.diffusion.api.ssh;

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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.api.ssh.auth.DiffusionPublicKeyAuthenticator;
import fr.ortolang.diffusion.api.ssh.session.SSHSessionFactory;
import fr.ortolang.diffusion.api.ssh.shell.SSHShellFactory;
import fr.ortolang.diffusion.api.ssh.vfs.DiffusionFileSystemFactory;
import fr.ortolang.diffusion.api.ssh.vfs.DiffusionSftpSubsystem;

@SuppressWarnings("serial")
public class SSHServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(SSHServlet.class.getName());

	@Resource
	private static ManagedThreadFactory threadFactory;
	@Resource
	private static ManagedScheduledExecutorService executor;

	private static SshServer sshd = null;
	private static boolean started = false;

	@Override
	public void init(ServletConfig config) throws ServletException {
		try {
			LOGGER.log(Level.INFO, "starting ssh service...");
			if (sshd == null) {
				sshd = SshServer.setUpDefaultServer();
				sshd.getProperties().put(SshServer.WELCOME_BANNER, "Welcome to Ortolang SSHD\n");
	
				sshd.setSessionFactory(new SSHSessionFactory(sshd));
				sshd.setFileSystemFactory(new DiffusionFileSystemFactory());
	
				sshd.setScheduledExecutorService(executor, true);
	
				sshd.setCommandFactory(new ScpCommandFactory());
	
				List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
				namedFactoryList.add(new DiffusionSftpSubsystem.Factory(threadFactory));
				sshd.setSubsystemFactories(namedFactoryList);
	
				sshd.setShellFactory(new SSHShellFactory(threadFactory));
	
				sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(OrtolangConfig.getInstance().getHome() + "/hostkey.ser"));
				sshd.setHost(OrtolangConfig.getInstance().getProperty("transport.ssh.host"));
				sshd.setPort(Integer.parseInt(OrtolangConfig.getInstance().getProperty("transport.ssh.port")));
	
//				DiffusionPasswordAuthenticator authenticator = new DiffusionPasswordAuthenticator();
//				sshd.setPasswordAuthenticator(authenticator);
				DiffusionPublicKeyAuthenticator pkauthenticator = new DiffusionPublicKeyAuthenticator();
				sshd.setPublickeyAuthenticator(pkauthenticator);
				sshd.start();
			}
			started = true;
			LOGGER.log(Level.INFO, "ssh service started: " + sshd.getVersion());
		} catch ( IOException e ) {
			LOGGER.log(Level.SEVERE, "unable to start ssh service: " + e.getMessage(), e);
		}
	}

	@Override
	public void destroy() {
		try {
			LOGGER.log(Level.INFO, "stopping ssh service...");
			if ( started ) {
				sshd.stop(true);
				LOGGER.log(Level.INFO, "ssh service stopped");
			} else  {
				LOGGER.log(Level.INFO, "ssh service not running, nothing to stop");
			}
		} catch ( InterruptedException e ) {
			LOGGER.log(Level.WARNING, "unable to stop ssh service: " + e.getMessage(), e);
		}
	}

}
