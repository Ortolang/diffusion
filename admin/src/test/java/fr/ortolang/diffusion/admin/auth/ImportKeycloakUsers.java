package fr.ortolang.diffusion.admin.auth;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class ImportKeycloakUsers {

	private static final Logger LOGGER = Logger.getLogger(ImportKeycloakUsers.class.getName());

	
	// @Test
	// public void getRoles() throws IOException {
	// java.util.List<RoleRepresentation> list = null;
	// try {
	// AccessTokenResponse res = KeycloakAdminClient.getToken();
	// list = KeycloakAdminClient.getRealmRoles(res);
	// KeycloakAdminClient.logout(res);
	// } catch (KeycloakAdminClient.Failure failure) {
	// LOGGER.log(Level.SEVERE, "There was a failure processing request.  You either didn't configure Keycloak properly");
	// LOGGER.log(Level.SEVERE, "Status from database service invocation was: " + failure.getStatus());
	// return;
	// }
	// for (RoleRepresentation role : list) {
	// LOGGER.log(Level.INFO, role.getName());
	// }
	// }
	
	@Test
	public void importUsers() throws IOException {
		AccessTokenResponse res = KeycloakAdminClient.getToken();
		FileReader in = new FileReader("/media/space/jerome/Data/SLDR/users.csv");
		Iterable<CSVRecord> records = CSVFormat.newFormat(',').withQuote('\"').withHeader().parse(in);

		for (CSVRecord record : records) {
			UserRepresentation user = new UserRepresentation();
			user.setUsername(record.get("pro_login"));
			user.setFirstName(record.get("pro_firstname"));
			user.setLastName(record.get("pro_lastname"));
			if (record.get("pro_emailt").length() > 0) {
				user.setEmail(record.get("pro_emailt"));
			} else {
				user.setEmail(record.get("pro_email"));
			}
			if ( record.get("pro_pwd").length() < 8 ) {
	        	user.setRequiredActions(Arrays.asList("UPDATE_PASSWORD"));
	        }
			user.setEmailVerified(true);
			user.setEnabled(true);
			
			System.out.print("Treating user: " + user.getUsername());
			boolean create = true;
			try {
				UserRepresentation userrep = KeycloakAdminClient.getUser(res, user.getUsername());
				System.out.print(" USER ALREADY EXISTS, NOTHING TO DO ");
				create = false;
				try {
					KeycloakAdminClient.deleteUser(res, user.getUsername());
					System.out.print(" OK");
				} catch ( KeycloakAdminClient.Failure failure) {
					System.out.print(" FAILED " + failure.getStatus());
					create = false;
				}
			} catch (KeycloakAdminClient.Failure failure) {
				if ( failure.getStatus() != 404 ) {
					System.out.print(" LOAD USER FAILED FOR UNKNOWN REASON " + failure.getStatus());
					create = false;
				}
			}
			if ( create ) {
				try {
					System.out.print(" CREATED");
					KeycloakAdminClient.createUser(res, user);
					System.out.print(" OK");
					
					CredentialRepresentation cred = new CredentialRepresentation();
			        cred.setType(CredentialRepresentation.PASSWORD);
			        cred.setValue(record.get("pro_pwd"));
			        cred.setTemporary(false);
			        System.out.print(" PWD");
			        KeycloakAdminClient.resetUserPassword(res, user.getUsername(), cred);
			        System.out.print(" OK");
			        
				} catch (KeycloakAdminClient.Failure failure) {
					System.out.print(" FAILED " + failure.getStatus() + "{" + user.getUsername() + ", " + user.getFirstName() + ", " + user.getLastName() + ", " + user.getEmail() + ", " + record.get("pwd") + "}");
				}
			}
			System.out.println();
		}
		KeycloakAdminClient.logout(res);
	}
	
}
