package fr.ortolang.diffusion.admin.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;


public class ImportKeycloakUsers {
	
	private static final Logger LOGGER = Logger.getLogger(ImportKeycloakUsers.class.getName());

//	@Test
//	public void getRoles() throws IOException {
//		java.util.List<RoleRepresentation> list = null;
//	    try {
//	        AccessTokenResponse res = KeycloakAdminClient.getToken();
//	        list = KeycloakAdminClient.getRealmRoles(res);
//	        KeycloakAdminClient.logout(res);
//	    } catch (KeycloakAdminClient.Failure failure) {
//	    	LOGGER.log(Level.SEVERE, "There was a failure processing request.  You either didn't configure Keycloak properly");
//	    	LOGGER.log(Level.SEVERE, "Status from database service invocation was: " + failure.getStatus());
//	        return;
//	    }
//	    for (RoleRepresentation role : list) {
//	    	LOGGER.log(Level.INFO, role.getName());
//	    }
//	}
	
	@Test
	public void createUser() throws IOException {
		try {
	        AccessTokenResponse res = KeycloakAdminClient.getToken();
	        UserRepresentation user = new UserRepresentation();
	        user.setUsername("toto");
	        user.setFirstName("Toto");
	        user.setLastName("TOTO");
	        user.setEmail("toto@toto.org");
	        user.setEmailVerified(true);
	        user.setEnabled(true);
	        KeycloakAdminClient.createUser(res, user);
	        List<RoleRepresentation> roles = new ArrayList<RoleRepresentation> ();
	        roles.add(new RoleRepresentation("user", ""));	
	        //KeycloakAdminClient.setUserRoles(res, "toto", roles);
	        KeycloakAdminClient.logout(res);
	    } catch (KeycloakAdminClient.Failure failure) {
	    	LOGGER.log(Level.SEVERE, "There was a failure processing request.  You either didn't configure Keycloak properly");
	    	LOGGER.log(Level.SEVERE, "Status from database service invocation was: " + failure.getStatus());
	        return;
	    }
	}
	

}
