package fr.ortolang.diffusion.ftp.user;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authentication.UsernamePasswordLoginContextFactory;

public class OrtolangUserManager implements UserManager {
    
    private static final Logger LOGGER = Logger.getLogger(OrtolangUserManager.class.getName());

    private static RegistryService registry;
    private static MembershipService membership;

    private static RegistryService getRegistryService() throws OrtolangException {
        if (registry == null) {
            registry = (RegistryService) OrtolangServiceLocator.findService(RegistryService.SERVICE_NAME);
        }
        return registry;
    }

    private static MembershipService getMembershipService() throws OrtolangException {
        if (membership == null) {
            membership = (MembershipService) OrtolangServiceLocator.findService(MembershipService.SERVICE_NAME);
        }
        return membership;
    }

    @Override
    public User authenticate(Authentication auth) throws AuthenticationFailedException {
        if (auth instanceof UsernamePasswordAuthentication) {
            LOGGER.log(Level.FINE, "checking authentication for username/password");
            UsernamePasswordAuthentication upauth = (UsernamePasswordAuthentication) auth;
            String username = upauth.getUsername();
            String password = upauth.getPassword();
            if (username == null) {
                throw new AuthenticationFailedException("authentication failed");
            }
            if (password == null) {
                throw new AuthenticationFailedException("authentication failed");
            }
            try {
                LoginContext lc = UsernamePasswordLoginContextFactory.createLoginContext(username, password);
                lc.login();
                LOGGER.log(Level.FINE, "try a call to membreship service to validate credentials");
                String expected = getMembershipService().getProfileKeyForIdentifier(username);
                String connected = getMembershipService().getProfileKeyForConnectedIdentifier(); 
                lc.logout();
                LOGGER.log(Level.FINE, "expected: " + expected);
                LOGGER.log(Level.FINE, "connected: " + connected);
                if (connected.equals(username)) {
                    LOGGER.log(Level.INFO, "authentication success");
                    return getUserByName(username, password);
                } else {
                    LOGGER.log(Level.INFO, "authentication failed: connected profile [" + connected + "] is NOT the expected one [" + expected + "]");
                    throw new AuthenticationFailedException("authentication failed");
                }
            } catch (OrtolangException | LoginException e) {
                LOGGER.log(Level.SEVERE, "unable to perform authentication");
                throw new AuthenticationFailedException("unable to perform authentication", e);
            }
        } else if (auth instanceof AnonymousAuthentication) {
            LOGGER.log(Level.FINE, "authenticate as " + MembershipService.UNAUTHENTIFIED_IDENTIFIER);
            return getUserByName(MembershipService.UNAUTHENTIFIED_IDENTIFIER);
        } else {
            LOGGER.log(Level.FINE, "unsupported authentication class");
            throw new IllegalArgumentException("authentication not supported by this user manager");
        }
    }

    @Override
    public User getUserByName(String username) {
        return getUserByName(username, "");
    }
    
    private User getUserByName(String username, String password) {
        BaseUser user = new BaseUser();
        user.setName(username);
        user.setEnabled(true);
        user.setPassword(password);
        user.setHomeDirectory("/");
        List<Authority> authorities = new ArrayList<Authority>();
        authorities.add(new ConcurrentLoginPermission(0, 0));
        authorities.add(new TransferRatePermission(0, 0));
        user.setAuthorities(authorities);
        user.setMaxIdleTime(600);
        return user;
    }

    @Override
    public boolean doesExist(String username) throws FtpException {
        LOGGER.log(Level.FINE, "checking existence of username: " + username);
        OrtolangObjectIdentifier identifier = new OrtolangObjectIdentifier(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, username);
        try {
            getRegistryService().lookup(identifier);
            return true;
        } catch (IdentifierNotRegisteredException e) {
            return false;
        } catch (OrtolangException | RegistryServiceException e) {
            throw new FtpException("unable to check if user exists in registry", e);
        }

    }

    @Override
    public boolean isAdmin(String username) throws FtpException {
        LOGGER.log(Level.FINE, "checking is admin for username: " + username);
        return username.equals(MembershipService.SUPERUSER_IDENTIFIER);
    }

    @Override
    public String getAdminName() throws FtpException {
        return MembershipService.SUPERUSER_IDENTIFIER;
    }

    @Override
    public void delete(String username) throws FtpException {
        throw new FtpException("unable to delete ortolang user");
    }

    @Override
    public String[] getAllUserNames() throws FtpException {
        throw new FtpException("unable to list all ortolang users");
    }

    @Override
    public void save(User username) throws FtpException {
        throw new FtpException("unable to save ortolang user");
    }

}
