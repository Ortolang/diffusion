package fr.ortolang.diffusion.ssh;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.ssh.authenticator.JaasPasswordAuthenticator;
import fr.ortolang.diffusion.ssh.command.SSHCommandFactory;
import fr.ortolang.diffusion.ssh.session.SSHSessionFactory;
import fr.ortolang.diffusion.ssh.shell.SSHShellFactory;

/**
 * Implementation of the SSH Service.<br/>
 * <br/>
 * The implementation starts an SSH Server in order to listen for clients and commands. Authentication of this SSH server module relies on the system authentication using a JAAS
 * connector. <br/>
 * Implementation is based on a EJB 3.0 Singleton Session Bean. Because internal visibility only, this bean does not implement Remote interface but only Local one.<br/>
 * <br/>
 * Bean security is configured for WildFly 8 and rely on JAAS to ensure Authentication and Authorization of user.<br/>
 * <br/>
 * 
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 21 September 2009
 */
@Startup
@Local(SSHService.class)
@Singleton(name = SSHService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class SSHServiceBean implements SSHService {

	private static Logger logger = Logger.getLogger(SSHServiceBean.class.getName());

	private static SSHCommandFactory commandFactory;
	private static SSHShellFactory shellFactory;
	private static SshServer sshd = null;

	public SSHServiceBean() {
		logger.log(Level.FINE, "new SSHServiceBean instance created");
	}

	@PostConstruct
	public void init() throws Exception {
		if (commandFactory == null) {
			commandFactory = new SSHCommandFactory();
			logger.log(Level.FINE, "sshd command factory created");
		}

		if (shellFactory == null) {
			shellFactory = new SSHShellFactory();
			logger.log(Level.FINE, "sshd shell factory created");
		}

		if (sshd == null) {
			try {
				sshd = SshServer.setUpDefaultServer();
				sshd.setSessionFactory(new SSHSessionFactory());
				sshd.setCommandFactory(commandFactory);
				sshd.setShellFactory(shellFactory);
				sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(OrtolangConfig.getInstance().getProperty("home") + "/hostkey.ser"));
				sshd.setHost(OrtolangConfig.getInstance().getProperty("transport.ssh.host"));
				sshd.setPort(Integer.parseInt(OrtolangConfig.getInstance().getProperty("transport.ssh.port")));

				JaasPasswordAuthenticator authenticator = new JaasPasswordAuthenticator();
				sshd.setPasswordAuthenticator(authenticator);
				sshd.start();
				logger.log(Level.FINE, "sshd started on host " + sshd.getHost() + ":" + sshd.getPort());
			} catch (Exception e) {
				logger.log(Level.SEVERE, "unable to start sshd", e);
			}
		}

		try {
			this.importShellCommands("fr.ortolang.diffusion.ssh.shell.command");
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable to import shell commands");
		}
	}

	@PreDestroy
	public void destroy() throws Exception {
		logger.log(Level.FINE, "stopping sshd");
		sshd.stop();
		logger.log(Level.FINE, "ssd stopped");
	}

	@Override
	public void registerCommand(String name, String classname) throws SSHServiceException {
		commandFactory.registerCommand(name, classname);
	}

	@Override
	public void importShellCommands(String packageName) throws SSHServiceException {
		shellFactory.importCommands(packageName);
	}

}
