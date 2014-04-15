package fr.ortolang.diffusion.test.bench;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.print.attribute.URISyntax;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SampleOneCollectionBench {

	private static final String SERVER_ADDRESS = "localhost";
	private static final String SERVER_PORT = "8080";
	private static final String APPLICATION_NAME = "diffusion";
	private static final String APPLICATION_REST_PREFIX = "rest";
	private static final String USERID = "user1";
	private static final String PASSWORD = "tagada";
	private static final String COLLECTION_ROOT_FOLDER = "/mnt/space/jerome/Data/SAMPLE1";

	private static Logger logger = Logger.getLogger(SampleOneCollectionBench.class.getName());
	private static Client client;
	private static WebTarget base;

	@BeforeClass
	public static void init() {
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(USERID, PASSWORD);
		client = ClientBuilder.newClient();
		client.register(feature);
		base = client.target("http://" + SERVER_ADDRESS + ":" + SERVER_PORT + "/" + APPLICATION_NAME + "/" + APPLICATION_REST_PREFIX);
	}

	@AfterClass
	public static void shutdown() {
		client.close();
	}

	/**
	 * Benchmark the creation time of a collection
	 * @throws IOException 
	 */
	@Test
	public void bench() throws IOException {
		logger.log(Level.INFO, "Starting Sample One benchmark");
		WebTarget connectedProfile = base.path("/membership/profiles/connected");
		Response response1 = connectedProfile.request(MediaType.APPLICATION_JSON_TYPE).get();
		if ( response1.getStatus() == Status.OK.getStatusCode() ) {
			String connectedIdentifier = response1.readEntity(String.class);
			logger.log(Level.INFO, "Connected Identifier : " + connectedIdentifier);
			if ( !connectedIdentifier.equals(USERID) ) {
				fail("Connected Identifier is not the good one, should be : " + USERID);
			}
		} else {
			logger.log(Level.INFO, "Unexpected response code while getting connected identifier : " + response1.getStatus() );
			fail("Unable to get Connected Identifier");
		}
		
		WebTarget profile = base.path("/membership/profiles/" + USERID);
		Response response2 = profile.request(MediaType.APPLICATION_JSON_TYPE).get();
		if ( response2.getStatus() == Status.NOT_FOUND.getStatusCode() ) {
			logger.log(Level.INFO, "Profile does NOT exists, creating it");
			WebTarget profileCreate = base.path("/membership/profiles");
			Form newprofile = new Form().param("fullname", USERID.toUpperCase()).param("email", USERID + "@ortolang.fr");
			Response response3 = profileCreate.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(newprofile, MediaType.APPLICATION_FORM_URLENCODED));
			if ( response3.getStatus() != Status.NO_CONTENT.getStatusCode() ) {
				logger.log(Level.INFO, "Unexpected response code while trying to create profile : " + response3.getStatus() );
				fail("Unable to create profile");
			}
		} else if ( response2.getStatus() != Status.OK.getStatusCode() ) {
			logger.log(Level.INFO, "Unexpected response code while getting profile : " + response2.getStatus() );
			fail("Unable to get profile");
		}
		
		Path root = Paths.get(COLLECTION_ROOT_FOLDER);
		Map<Path, String> collections = new HashMap<Path, String> ();
		Files.walkFileTree(root, new CollectionBuilder(collections));
		
	}
	
	class CollectionBuilder implements FileVisitor<Path> {
		
		private Map<Path, String> collections;
		
		public CollectionBuilder(Map<Path, String> collections) {
			logger.log(Level.INFO, "New CollectionBuilder");
			this.collections = collections;
		}
		
		public Map<Path, String> getCollections() {
			return collections;
		}
		
		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			logger.log(Level.INFO, "Creating collection for path : " + dir);
			WebTarget collectionsTarget = base.path("/core/collections");
			Form newcollection = new Form().param("name", dir.getFileName().toString()).param("description", "A collection corresponding to provided folder " + dir.getFileName());
			Response response5 = collectionsTarget.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(newcollection, MediaType.APPLICATION_FORM_URLENCODED));
			if ( response5.getStatus() != Status.CREATED.getStatusCode() ) {
				logger.log(Level.INFO, "Unexpected response code while trying to create collection : " + response5.getStatus() );
				fail("Unable to create collection");
			}
			String key = response5.getLocation().getPath().substring(response5.getLocation().getPath().lastIndexOf("/"));
			collections.put(dir, key);
			logger.log(Level.INFO, "Created collection key : " + key);
			if ( collections.containsKey(dir.getParent()) ) {
				logger.log(Level.INFO, "Adding the created collection as a member of its parent : " + dir.getParent());
				String parent = collections.get(dir.getParent());
				Response response6 = collectionsTarget.path(parent).path("elements").path(key).request().accept(MediaType.MEDIA_TYPE_WILDCARD).put(null);
				if ( response6.getStatus() != Status.NO_CONTENT.getStatusCode() ) {
					logger.log(Level.INFO, "Unexpected response code while trying to add element to collection : " + response6.getStatus() );
					fail("Unable to add element to collection");
				}
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			logger.log(Level.INFO, "Creating DataObject for file : " + file);
			WebTarget objectsTarget = base.path("/core/dataobjects");
			WebTarget collectionsTarget = base.path("/core/collections");
			//TODO Change this to include file data as multipart form data
			Form newobject = new Form().param("name", file.getFileName().toString()).param("description", "A data object corresponding to provided file " + file.getFileName());
			Response response7 = objectsTarget.request(MediaType.MULTIPART_FORM_DATA).accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(newobject, MediaType.MULTIPART_FORM_DATA));
			if ( response7.getStatus() != Status.CREATED.getStatusCode() ) {
				logger.log(Level.INFO, "Unexpected response code while trying to create dataobject : " + response7.getStatus() );
				fail("Unable to create dataobject");
			}
			String key = response7.getLocation().getPath().substring(response7.getLocation().getPath().lastIndexOf("/"));
			logger.log(Level.INFO, "Created data object key : " + key);
			if ( collections.containsKey(file.getParent()) ) {
				logger.log(Level.INFO, "Adding the created dataobject as a member of its parent : " + file.getParent());
				String parent = collections.get(file.getParent());
				Response response8 = collectionsTarget.path(parent).path("elements").path(key).request().accept(MediaType.MEDIA_TYPE_WILDCARD).put(null);
				if ( response8.getStatus() != Status.NO_CONTENT.getStatusCode() ) {
					logger.log(Level.INFO, "Unexpected response code while trying to add element to collection : " + response8.getStatus() );
					fail("Unable to add element to collection");
				}
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			logger.log(Level.WARNING, "Unable to create DataObject for file : " + file, exc);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}
		
	}
	
	
}
