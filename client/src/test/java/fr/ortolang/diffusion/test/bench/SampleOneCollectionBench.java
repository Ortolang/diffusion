package fr.ortolang.diffusion.test.bench;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SampleOneCollectionBench {

	private static final String COLLECTION_ROOT_FOLDER = "/mnt/space/jerome/Data/SAMPLE1";
	
	private static Logger logger = Logger.getLogger(SampleOneCollectionBench.class.getName());
	private static Client client;
	private static WebTarget base;
	
	@BeforeClass
	public static void init() {
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(BenchSuite.USERID, BenchSuite.PASSWORD);
		client = ClientBuilder.newClient();
		client.register(feature);
		client.register(MultiPartFeature.class);
		client.register(CookieFilter.class);
		base = client.target("http://" + BenchSuite.SERVER_ADDRESS + ":" + BenchSuite.SERVER_PORT + "/" + BenchSuite.APPLICATION_NAME + "/" + BenchSuite.APPLICATION_REST_PREFIX);
	}

	@AfterClass
	public static void shutdown() {
		client.close();
	}

	/**
	 * Benchmark the creation time of a collection
	 * 
	 * @throws IOException
	 */
	@Test
	public void bench() throws IOException {
		long start = System.currentTimeMillis();
		
		logger.log(Level.INFO, "Starting Sample One benchmark");
		WebTarget connectedProfile = base.path("/membership/profiles");
		Response response1 = connectedProfile.request(MediaType.APPLICATION_JSON_TYPE).get();
		if (response1.getStatus() == Status.OK.getStatusCode()) {
			String connectedIdentifier = response1.readEntity(String.class);
			logger.log(Level.INFO, "Connected Identifier : " + connectedIdentifier);
			if (!connectedIdentifier.equals(BenchSuite.USERID)) {
				fail("Connected Identifier is not the good one, should be : " + BenchSuite.USERID);
			}
		} else {
			logger.log(Level.WARNING, "Unexpected response code while getting connected identifier : " + response1.getStatus());
			fail("Unable to get Connected Identifier");
		}

//		WebTarget profile = base.path("/membership/profiles/" + BenchSuite.USERID);
//		Response response2 = profile.request(MediaType.APPLICATION_JSON_TYPE).get();
//		if (response2.getStatus() == Status.NOT_FOUND.getStatusCode()) {
//			logger.log(Level.FINE, "Profile does NOT exists, creating it");
//			WebTarget profileCreate = base.path("/membership/profiles");
//			Form newprofile = new Form().param("fullname", BenchSuite.USERID.toUpperCase()).param("email", BenchSuite.USERID + "@ortolang.fr");
//			Response response3 = profileCreate.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD)
//					.post(Entity.entity(newprofile, MediaType.APPLICATION_FORM_URLENCODED));
//			if (response3.getStatus() != Status.NO_CONTENT.getStatusCode()) {
//				logger.log(Level.WARNING, "Unexpected response code while trying to create profile : " + response3.getStatus());
//				fail("Unable to create profile");
//			}
//		} else if (response2.getStatus() != Status.OK.getStatusCode()) {
//			logger.log(Level.WARNING, "Unexpected response code while getting profile : " + response2.getStatus());
//			fail("Unable to get profile");
//		}

		Path root = Paths.get(COLLECTION_ROOT_FOLDER);
		Map<Path, String> collections = new HashMap<Path, String>();
		CollectionBuilder builder = new CollectionBuilder(collections);
		Files.walkFileTree(root, builder);

		long stop = System.currentTimeMillis();
		
		logger.log(Level.INFO, "Benchmark Report => collections:" + builder.getNbCollections() + ", dataobjects:" + builder.getNbDataObjects() + ", totalSize:" + builder.getTotalLength() + ", time:" + (stop-start));
	}

	class CollectionBuilder implements FileVisitor<Path> {

		private Map<Path, String> collections;
		private int nbCollections = 0;
		private int nbDataObjects = 0;
		private long volume = 0;

		public CollectionBuilder(Map<Path, String> collections) {
			logger.log(Level.FINE, "New CollectionBuilder");
			this.collections = collections;
		}

		public Map<Path, String> getCollections() {
			return collections;
		}
		
		public long getTotalLength() {
			return volume;
		}
		
		public int getNbCollections() {
			return nbCollections;
		}
		
		public int getNbDataObjects() {
			return nbDataObjects;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			logger.log(Level.FINE, "Creating collection for path : " + dir);
			WebTarget collectionsTarget = base.path("/core/collections");
			Form newcollection = new Form().param("name", dir.getFileName().toString()).param("description", "A collection corresponding to provided folder " + dir.getFileName());
			Response response5 = collectionsTarget.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD)
					.post(Entity.entity(newcollection, MediaType.APPLICATION_FORM_URLENCODED));
			if (response5.getStatus() != Status.CREATED.getStatusCode()) {
				logger.log(Level.WARNING, "Unexpected response code while trying to create collection : " + response5.getStatus());
				fail("Unable to create collection");
			}
			String key = response5.getLocation().getPath().substring(response5.getLocation().getPath().lastIndexOf("/"));
			nbCollections++;
			collections.put(dir, key);
			logger.log(Level.FINE, "Created collection key : " + key);
			if (collections.containsKey(dir.getParent())) {
				logger.log(Level.FINE, "Adding the created collection as a member of its parent : " + dir.getParent());
				String parent = collections.get(dir.getParent());
				Response response6 = collectionsTarget.path(parent).path("elements").path(key).request().accept(MediaType.MEDIA_TYPE_WILDCARD).put(Entity.entity(key, MediaType.TEXT_PLAIN));
				if (response6.getStatus() != Status.NO_CONTENT.getStatusCode()) {
					logger.log(Level.WARNING, "Unexpected response code while trying to add element to collection : " + response6.getStatus());
					fail("Unable to add element to collection");
				}
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			logger.log(Level.FINE, "Creating DataObject for file : " + file);
			WebTarget objectsTarget = base.path("/core/objects");
			WebTarget collectionsTarget = base.path("/core/collections");
			
			File thefile = file.toFile();
			FileDataBodyPart filePart = new FileDataBodyPart("file", thefile);
			MultiPart multipart = new FormDataMultiPart()
		    .field("name", file.getFileName().toString())
		    .field("description", "A data object corresponding to provided file " + file.getFileName())
		    .bodyPart(filePart);
			Response response7 = objectsTarget.request().accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(multipart, multipart.getMediaType()));
			if ( response7.getStatus() != Status.CREATED.getStatusCode() ) {
				logger.log(Level.WARNING, "Unexpected response code while trying to create dataobject : " + response7.getStatus() );
				fail("Unable to create dataobject");
			}
			nbDataObjects++;
			volume += thefile.length();
			String key = response7.getLocation().getPath().substring(response7.getLocation().getPath().lastIndexOf("/"));
			logger.log(Level.FINE, "Created data object key : " + key);
			if ( collections.containsKey(file.getParent()) ) {
				logger.log(Level.FINE, "Adding the created dataobject as a member of its parent : " + file.getParent());
				String parent = collections.get(file.getParent());
				Response response8 = collectionsTarget.path(parent).path("elements").path(key).request().accept(MediaType.MEDIA_TYPE_WILDCARD).put(Entity.entity(key, MediaType.TEXT_PLAIN));
				if ( response8.getStatus() != Status.NO_CONTENT.getStatusCode() ) {
					logger.log(Level.WARNING, "Unexpected response code while trying to add element to collection : " + response8.getStatus() );
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
