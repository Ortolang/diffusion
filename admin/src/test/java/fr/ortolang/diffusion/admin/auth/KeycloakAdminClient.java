package fr.ortolang.diffusion.admin.auth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;

public class KeycloakAdminClient {

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
			HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(getAuthUrlBase() + "/auth").path(ServiceUrlConstants.TOKEN_SERVICE_DIRECT_GRANT_PATH).build("ortolang"));
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			formparams.add(new BasicNameValuePair("username", "root"));
			formparams.add(new BasicNameValuePair("password", "tagada54"));
			formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
			formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "admin-client"));
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
			HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(getAuthUrlBase() + "/auth").path(ServiceUrlConstants.TOKEN_SERVICE_LOGOUT_PATH).build("ortolang"));
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();
			formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, res.getRefreshToken()));
			formparams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, "admin-client"));
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
			HttpGet get = new HttpGet(getAuthUrlBase() + "/auth/admin/realms/ortolang/roles");
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
			HttpPost post = new HttpPost(getAuthUrlBase() + "/auth/admin/realms/ortolang/users");
			post.addHeader("Authorization", "Bearer " + res.getToken());
			post.addHeader("Content-type", "application/json");
			try {
				post.setEntity(new StringEntity(JsonSerialization.writeValueAsString(user)));
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
	
	public static void setUserRoles(AccessTokenResponse res, String username, List<RoleRepresentation> roles) throws Failure {

		HttpClient client = new HttpClientBuilder().disableTrustManager().build();
		try {
			HttpPost post = new HttpPost(getAuthUrlBase() + "/auth/admin/realms/ortolang/users/" + username + "/role-mappings/realm");
			post.addHeader("Authorization", "Bearer " + res.getToken());
			post.addHeader("Content-type", "application/json");
			try {
				post.setEntity(new StringEntity(JsonSerialization.writeValueAsString(roles)));
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

	public static String getAuthUrlBase() {
		return "http://localhost:8080";
	}

}
