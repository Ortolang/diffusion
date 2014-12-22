package fr.ortolang.diffusion.api.ssh;

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

	private static Logger logger = Logger.getLogger(SSHServlet.class.getName());

	@Resource
	private ManagedThreadFactory threadFactory;
	@Resource
	private ManagedScheduledExecutorService executor;

	private SshServer sshd = null;
	private boolean started = false;

	@Override
	public void init(ServletConfig config) throws ServletException {
		try {
			logger.log(Level.INFO, "starting ssh service...");
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
	
				sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(OrtolangConfig.getInstance().getProperty("home") + "/hostkey.ser"));
				sshd.setHost(OrtolangConfig.getInstance().getProperty("transport.ssh.host"));
				sshd.setPort(Integer.parseInt(OrtolangConfig.getInstance().getProperty("transport.ssh.port")));
	
//				DiffusionPasswordAuthenticator authenticator = new DiffusionPasswordAuthenticator();
//				sshd.setPasswordAuthenticator(authenticator);
				DiffusionPublicKeyAuthenticator pkauthenticator = new DiffusionPublicKeyAuthenticator();
				sshd.setPublickeyAuthenticator(pkauthenticator);
				sshd.start();
			}
			started = true;
			logger.log(Level.INFO, "ssh service started: " + sshd.getVersion());
		} catch ( IOException e ) {
			logger.log(Level.SEVERE, "unable to start ssh service: " + e.getMessage(), e);
		}
	}

	@Override
	public void destroy() {
		try {
			logger.log(Level.INFO, "stopping ssh service...");
			if ( started ) {
				sshd.stop();
				logger.log(Level.INFO, "ssh service stopped");
			} else  {
				logger.log(Level.INFO, "ssh service not running, nothing to stop");
			}
		} catch ( InterruptedException e ) {
			logger.log(Level.WARNING, "unable to stop ssh service: " + e.getMessage(), e);
		}
	}

}
