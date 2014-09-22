package fr.ortolang.diffusion.api.ssh.session;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.session.SessionFactory;

public class SSHSessionFactory extends SessionFactory {
	
    protected SshServer sshserver;

    public SSHSessionFactory(SshServer sshserver) {
    	this.sshserver = sshserver;
    }
    
    protected AbstractSession doCreateSession(IoSession ioSession) throws Exception {
    	return new SSHSession(sshserver, ioSession);
    }
    
}
