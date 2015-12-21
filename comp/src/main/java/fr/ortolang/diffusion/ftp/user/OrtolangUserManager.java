package fr.ortolang.diffusion.ftp.user;

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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.ftp.FtpService;
import fr.ortolang.diffusion.ftp.FtpServiceException;
import fr.ortolang.diffusion.membership.MembershipService;

public class OrtolangUserManager implements UserManager {
    
    private static final Logger LOGGER = Logger.getLogger(OrtolangUserManager.class.getName());

    private static FtpService ftp;

    private static FtpService getFtpService() throws OrtolangException {
        if (ftp == null) {
            ftp = (FtpService) OrtolangServiceLocator.findService(FtpService.SERVICE_NAME);
        }
        return ftp;
    }

    @Override
    public User authenticate(Authentication auth) throws AuthenticationFailedException {
        if (auth instanceof UsernamePasswordAuthentication) {
            LOGGER.log(Level.FINE, "checking totp validity for username: " + ((UsernamePasswordAuthentication) auth).getUsername());
            UsernamePasswordAuthentication upauth = (UsernamePasswordAuthentication) auth;
            String username = upauth.getUsername();
            String totp = upauth.getPassword();
            if (username == null || username.length() == 0) {
                throw new AuthenticationFailedException("authentication failed, username is null or empty");
            }
            if (totp == null || totp.length() == 0) {
                throw new AuthenticationFailedException("authentication failed, password is null or empty");
            }
            try {
                boolean validTotp = getFtpService().checkUserAuthentication(username, totp);
                if ( validTotp ) {
                    LOGGER.log(Level.INFO, "authentication success totp is valid, retreiving profile secret to enforce EJB authentication");
                    String secret = getFtpService().getInternalAuthenticationPassword(username);
                    return getUserByName(username, secret);
                } else {
                    LOGGER.log(Level.INFO, "authentication failed totp is not valid");
                    throw new AuthenticationFailedException("authentication failed");
                }
            } catch (OrtolangException | FtpServiceException e) {
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
        try {
            return getFtpService().checkUserExistence(username);
        } catch (FtpServiceException | OrtolangException e) {
            throw new FtpException("unable to check if user exists", e);
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
