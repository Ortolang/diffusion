package fr.ortolang.diffusion.ssh.session;

import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.session.SessionFactory;


/**
 * A factory for creating SSHServerSession instead of sshd internal server session.
 *
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 24 September 2009
 */
public class SSHSessionFactory extends SessionFactory {
    protected ServerFactoryManager server;

    public void setServer(ServerFactoryManager server) {
        this.server = server;
    }

    protected AbstractSession createSession(IoSession ioSession)
        throws Exception {
        return new SSHSession(server, ioSession);
    }
}
