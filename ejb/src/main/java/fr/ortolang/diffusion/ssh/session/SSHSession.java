package fr.ortolang.diffusion.ssh.session;

import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.session.ServerSession;
import org.jboss.logmanager.Level;


/**
 * A specific server session to store the JAAS login context. This allow to logout before session close.
 *
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @date 24 September 2009
 */
public class SSHSession extends ServerSession {
    private static Logger logger = Logger.getLogger(SSHSession.class.getName());
    private LoginContext lc;

    public SSHSession(ServerFactoryManager server, IoSession ioSession)
        throws Exception {
        super(server, ioSession);
    }

    public void setLoginContext(LoginContext lc) {
        this.lc = lc;
    }

    @Override
    public CloseFuture close(boolean immediately) {
        logger.log(Level.FINE, "closing ssh server session");
        try {
            lc.logout();
        } catch (LoginException e) {
            logger.log(Level.WARNING, "unable to logout ", e);
        }
        return super.close(immediately);
    }
}
