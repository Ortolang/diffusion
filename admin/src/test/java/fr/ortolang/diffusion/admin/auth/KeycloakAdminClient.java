package fr.ortolang.diffusion.admin.auth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.keycloak.common.util.KeycloakUriBuilder;

public class KeycloakAdminClient {
	
	private static final Logger LOGGER = Logger.getLogger(KeycloakAdminClient.class.getName());
	private static final String USERNAME = "root";
	private static final String PASSWORD = "tagada54";
	private static final String REALM = "ortolang";
	private static final String CLIENT_ID = "import";
	private static final String BASE_URL = "https://localhost:8443";
	

	static class TypedList extends ArrayList<RoleRepresentation> {
	}

	public static class Failure extends Exception {
		private int status;

		public Failure(int status) {
			this.status = status;
		}

		public int getStatus() {
			return status;
		}
	}

	public static String getContent(HttpEntity entity) throws IOException {
		if (entity == null)
			return null;
		InputStream is = entity.getContent();
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int c;
			while ((c = is.read()) != -1) {
				os.write(c);
			}
			byte[] bytes = os.toByteArray();
			String data = new String(bytes);
			return data;
		} finally {
			try {
				is.close();
			} catch (IOException ignored) {

			}
		}

	}

	public static AccessTokenResponse getToken() throws IOException {

		HttpClient client = new HttpClientBuilder().disableTrustManager().build();

		try {
			HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(BASE_URL + "/auth").path(ServiceUrlConstants.TOKEN_PATH).build(REALM));
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			formparams.add(new BasicNameValuePair("username", USERNAME));
			formparams.add(new BasicNameValuePair("password", PASSWORD));
			formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
			formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, CLIENT_ID));
			UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
			post.setEntity(form);

			HttpResponse response = client.execute(post);
			int status = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			if (status != 200) {
				String json = getContent(entity);
				throw new IOException("Bad status: " + status + " response: " + json);
			}
			if (entity == null) {
				throw new IOException("No Entity");
			}
			String json = getContent(entity);
			return JsonSerialization.readValue(json, AccessTokenResponse.class);
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static void logout(AccessTokenResponse res) throws IOException {

		HttpClient client = new HttpClientBuilder().disableTrustManager().build();

		try {
			HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(BASE_URL + "/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).build(REALM));
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, res.getRefreshToken()));
			formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, CLIENT_ID));
			UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
			post.setEntity(form);
			HttpResponse response = client.execute(post);
			boolean status = response.getStatusLine().getStatusCode() != 204;
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				return;
			}
			InputStream is = entity.getContent();
			if (is != null)
				is.close();
			if (status) {
				throw new RuntimeException("failed to logout");
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static List<RoleRepresentation> getRealmRoles(AccessTokenResponse res) throws Failure {

		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			HttpGet get = new HttpGet(BASE_URL + "/auth/admin/realms/" + REALM + "/roles");
			get.addHeader("Authorization", "Bearer " + res.getToken());
			try {
				HttpResponse response = client.execute(get);
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				try {
					return JsonSerialization.readValue(is, TypedList.class);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}
	
	public static void createUser(AccessTokenResponse res, UserRepresentation user) throws Failure {

		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			HttpPost post = new HttpPost(BASE_URL + "/auth/admin/realms/" + REALM + "/users");
			post.addHeader("Authorization", "Bearer " + res.getToken());
			post.addHeader("Content-type", "application/json");
			try {
				post.setEntity(new StringEntity(JsonSerialization.writeValueAsString(user), "UTF-8"));
				HttpResponse response = client.execute(post);
				if (response.getStatusLine().getStatusCode() != 201) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}
	
	public static UserRepresentation getUser(AccessTokenResponse res, String username) throws Failure {

		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			try {
				HttpGet get = new HttpGet(BASE_URL + "/auth/admin/realms/" + REALM + "/users/" + URLEncoder.encode(username, "UTF-8").replaceAll("\\+", "%20"));
				get.addHeader("Authorization", "Bearer " + res.getToken());
				get.addHeader("Content-type", "application/json");
				HttpResponse response = client.execute(get);
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				try {
					return JsonSerialization.readValue(is, UserRepresentation.class);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static UserRepresentation[] getUsers(AccessTokenResponse res) throws Failure {
		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			try {
				HttpGet get = new HttpGet(BASE_URL + "/auth/admin/realms/" + REALM + "/users/");
				get.addHeader("Authorization", "Bearer " + res.getToken());
				get.addHeader("Content-type", "application/json");
				HttpResponse response = client.execute(get);
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				try {
					return JsonSerialization.readValue(is, UserRepresentation[].class);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static void updateUser(AccessTokenResponse res, UserRepresentation userRepresentation) throws Failure {
		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			try {
				HttpPut put = new HttpPut(BASE_URL + "/auth/admin/realms/" + REALM + "/users/" + userRepresentation.getId());
				put.addHeader("Authorization", "Bearer " + res.getToken());
				put.addHeader("Content-type", "application/json");
				put.setEntity(new StringEntity(JsonSerialization.writeValueAsString(userRepresentation), "UTF-8"));
				HttpResponse response = client.execute(put);
				if (response.getStatusLine().getStatusCode() != 204) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static RoleRepresentation[] getUserRoleMapping(AccessTokenResponse res, UserRepresentation userRepresentation) throws Failure {
		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			try {
				HttpGet get = new HttpGet(BASE_URL + "/auth/admin/realms/" + REALM + "/users/" + userRepresentation.getId() + "/role-mappings/realm");
				get.addHeader("Authorization", "Bearer " + res.getToken());
				HttpResponse response = client.execute(get);
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				try {
					return JsonSerialization.readValue(is, RoleRepresentation[].class);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static RoleRepresentation[] getRealmRoleMapping(AccessTokenResponse res, UserRepresentation userRepresentation) throws Failure {
		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			try {
				HttpGet get = new HttpGet(BASE_URL + "/auth/admin/realms/" + REALM + "/users/" + userRepresentation.getId() + "/role-mappings/realm/available");
				get.addHeader("Authorization", "Bearer " + res.getToken());
				HttpResponse response = client.execute(get);
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
				HttpEntity entity = response.getEntity();
				InputStream is = entity.getContent();
				try {
					return JsonSerialization.readValue(is, RoleRepresentation[].class);
				} finally {
					is.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}

	public static void addUserRoleMappings(AccessTokenResponse res, UserRepresentation userRepresentation, RoleRepresentation[] roleRepresentations) throws Failure {
		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			try {
				HttpPost post = new HttpPost(BASE_URL + "/auth/admin/realms/" + REALM + "/users/" + userRepresentation.getId() + "/role-mappings/realm");
				post.addHeader("Authorization", "Bearer " + res.getToken());
				post.addHeader("Content-type", "application/json");
				post.setEntity(new StringEntity(JsonSerialization.writeValueAsString(roleRepresentations), "UTF-8"));
				HttpResponse response = client.execute(post);
				if (response.getStatusLine().getStatusCode() != 204) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}
	
	public static void deleteUser(AccessTokenResponse res, String username) throws Failure {

		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			try {
				HttpDelete delete = new HttpDelete(BASE_URL + "/auth/admin/realms/" + REALM + "/users/" + URLEncoder.encode(username, "UTF-8").replaceAll("\\+", "%20"));
				delete.addHeader("Authorization", "Bearer " + res.getToken());
				delete.addHeader("Content-type", "application/json");
				HttpResponse response = client.execute(delete);
				if (response.getStatusLine().getStatusCode() != 204) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}
	
	public static void resetUserPassword(AccessTokenResponse res, String username, CredentialRepresentation cred) throws Failure {

		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			try {
				HttpPut put = new HttpPut(BASE_URL + "/auth/admin/realms/ortolang/" + REALM + "/" + URLEncoder.encode(username, "UTF-8").replaceAll("\\+", "%20") + "/reset-password");
				put.addHeader("Authorization", "Bearer " + res.getToken());
				put.addHeader("Content-type", "application/json");
				put.setEntity(new StringEntity(JsonSerialization.writeValueAsString(cred), "UTF-8"));
				HttpResponse response = client.execute(put);
				if (response.getStatusLine().getStatusCode() != 204) {
					throw new Failure(response.getStatusLine().getStatusCode());
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			client.getConnectionManager().shutdown();
		}
	}
	
	public static String getAuthUrlBase() {
		return "http://localhost:8080";
	}

}
