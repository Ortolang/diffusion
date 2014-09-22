package fr.ortolang.diffusion.api.ssh.session;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.server.session.ServerSession;

public class SSHSession extends ServerSession {

	private static Logger logger = Logger.getLogger(SSHSession.class.getName());
	private String login;
	private String password;

	public SSHSession(SshServer server, IoSession ioSession) throws Exception {
		super(server, ioSession);
		logger.log(Level.FINE, "ssh server session created");
	}

	public void setLogin(String login) {
		this.login = login;
	}
	
	public String getLogin() {
		return login;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return password;
	}

}
