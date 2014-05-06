package fr.ortolang.diffusion.test.bench;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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

public class ProjectBench {

	private static final String SAMPLE_FILE = "/mnt/space/jerome/Data/sample.txt";
	private static final String OUTPUT_FILE = "/mnt/space/jerome/Data/project-bench.csv";
	private static final int CREATE_SIZE = 500;
	private static final int CREATE_ITERATIONS = 20;
	private static final int READ_ITERATIONS = 5;
	private static final int CLONE_ITERATIONS = 5;
	private static final int CPT = 0;
	private static final int MIN = 1;
	private static final int SUM = 2;
	private static final int MAX = 3;

	private static Logger logger = Logger.getLogger(SampleOneCollectionBench.class.getName());
	private static Client client;
	private static WebTarget base;

	private long[] createDataObjectStats = new long[] { 0, 0, 0, 0 };
	private long[] addElementStats = new long[] { 0, 0, 0, 0 };
	private long[] readCollectionStats = new long[] { 0, 0, 0, 0 };
	private long[] snapshotProjectStats = new long[] { 0, 0, 0, 0 };

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
		logger.log(Level.INFO, "Starting Collection benchmark");

		try (OutputStream os = Files.newOutputStream(Paths.get(OUTPUT_FILE))) {
			
			os.write("size,create min, create avg, create max, insert min, insert avg, insert max, read min, read avg, read max, clone min, clone avg, clone max\r\n".getBytes());

			WebTarget connectedProfile = base.path("/membership/profiles/connected");
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

			WebTarget profile = base.path("/membership/profiles/" + BenchSuite.USERID);
			Response response2 = profile.request(MediaType.APPLICATION_JSON_TYPE).get();
			if (response2.getStatus() == Status.NOT_FOUND.getStatusCode()) {
				logger.log(Level.FINE, "Profile does NOT exists, creating it");
				WebTarget profileCreate = base.path("/membership/profiles");
				Form newprofile = new Form().param("fullname", BenchSuite.USERID.toUpperCase()).param("email", BenchSuite.USERID + "@ortolang.fr");
				Response response3 = profileCreate.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD)
						.post(Entity.entity(newprofile, MediaType.APPLICATION_FORM_URLENCODED));
				if (response3.getStatus() != Status.NO_CONTENT.getStatusCode()) {
					logger.log(Level.WARNING, "Unexpected response code while trying to create profile : " + response3.getStatus());
					fail("Unable to create profile");
				}
			} else if (response2.getStatus() != Status.OK.getStatusCode()) {
				logger.log(Level.WARNING, "Unexpected response code while getting profile : " + response2.getStatus());
				fail("Unable to get profile");
			}

			WebTarget projectsTarget = base.path("/collaboration/projects");
			WebTarget collectionsTarget = base.path("/core/collections");
			WebTarget objectsTarget = base.path("/core/objects");

			logger.log(Level.INFO, "Creating project");
			Form newproject = new Form().param("name", "benchmark project " + System.currentTimeMillis()).param("type", "benchmark");
			Response response5 = projectsTarget.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD)
					.post(Entity.entity(newproject, MediaType.APPLICATION_FORM_URLENCODED));
			if (response5.getStatus() != Status.CREATED.getStatusCode()) {
				logger.log(Level.WARNING, "Unexpected response code while trying to create project : " + response5.getStatus());
				fail("Unable to create project");
			}
			String projectkey = response5.getLocation().getPath().substring(response5.getLocation().getPath().lastIndexOf("/"));
			logger.log(Level.INFO, "Created project key : " + projectkey);
			
//			for (int j = 1; j <= CREATE_ITERATIONS; j++) {
//				for (int i = 0; i < CREATE_SIZE; i++) {
//					logger.log(Level.FINE, "Creating sample DataObject to populate collection");
//					File thefile = Paths.get(SAMPLE_FILE).toFile();
//					FileDataBodyPart filePart = new FileDataBodyPart("file", thefile);
//					MultiPart multipart = new FormDataMultiPart().field("name", thefile.getName().toString())
//							.field("description", "A data object corresponding to the sample file " + thefile.getName()).bodyPart(filePart);
//
//					long start = System.currentTimeMillis();
//					Response response7 = objectsTarget.request().accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(multipart, multipart.getMediaType()));
//					long stop = System.currentTimeMillis();
//					if (response7.getStatus() != Status.CREATED.getStatusCode()) {
//						logger.log(Level.WARNING, "Unexpected response code while trying to create dataobject : " + response7.getStatus());
//						fail("Unable to create dataobject");
//					}
//					String filekey = response7.getLocation().getPath().substring(response7.getLocation().getPath().lastIndexOf("/"));
//					logger.log(Level.FINE, "Created data object key : " + filekey);
//					long time = stop - start;
//					createDataObjectStats[CPT]++;
//					if (createDataObjectStats[MIN] <= 0 || time < createDataObjectStats[MIN]) {
//						createDataObjectStats[MIN] = time;
//					}
//					createDataObjectStats[SUM] += time;
//					if (time > createDataObjectStats[MAX]) {
//						createDataObjectStats[MAX] = time;
//					}
//
//					logger.log(Level.FINE, "Adding the created dataobject as a collection element");
//					start = System.currentTimeMillis();
//					Response response8 = collectionsTarget.path(collectionkey).path("elements").path(filekey).request().accept(MediaType.MEDIA_TYPE_WILDCARD)
//							.put(Entity.entity(filekey, MediaType.TEXT_PLAIN));
//					stop = System.currentTimeMillis();
//					if (response8.getStatus() != Status.NO_CONTENT.getStatusCode()) {
//						logger.log(Level.WARNING, "Unexpected response code while trying to add element to collection : " + response8.getStatus());
//						fail("Unable to add element to collection");
//					}
//					time = stop - start;
//					addElementStats[CPT]++;
//					if (addElementStats[MIN] <= 0 || time < addElementStats[MIN]) {
//						addElementStats[MIN] = time;
//					}
//					addElementStats[SUM] += time;
//					if (time > addElementStats[MAX]) {
//						addElementStats[MAX] = time;
//					}
//				}
//				for (int i = 0; i < READ_ITERATIONS; i++) {
//					logger.log(Level.FINE, "Reading collection");
//					long start = System.currentTimeMillis();
//					Response response9 = collectionsTarget.path(collectionkey).request().accept(MediaType.MEDIA_TYPE_WILDCARD).get();
//					long stop = System.currentTimeMillis();
//					if (response9.getStatus() != Status.OK.getStatusCode()) {
//						logger.log(Level.WARNING, "Unexpected response code while trying to read collection : " + response9.getStatus());
//						fail("Unable to read collection");
//					}
//					long time = stop - start;
//					
//					readCollectionStats[CPT]++;
//					if (readCollectionStats[MIN] <= 0 || time < readCollectionStats[MIN]) {
//						readCollectionStats[MIN] = time;
//					}
//					readCollectionStats[SUM] += time;
//					if (time > readCollectionStats[MAX]) {
//						readCollectionStats[MAX] = time;
//					}
//				}
//				
//				StringBuffer buffer = new StringBuffer();
//				buffer.append(j*CREATE_SIZE).append(",");
//				buffer.append(createDataObjectStats[MIN]).append(",");
//				buffer.append((createDataObjectStats[SUM] / createDataObjectStats[CPT])).append(",");
//				buffer.append(createDataObjectStats[MAX]).append(",");
//				buffer.append(addElementStats[MIN]).append(",");
//				buffer.append((addElementStats[SUM] / addElementStats[CPT])).append(",");
//				buffer.append(addElementStats[MAX]).append(",");
//				buffer.append(readCollectionStats[MIN]).append(",");
//				buffer.append((readCollectionStats[SUM] / readCollectionStats[CPT])).append(",");
//				buffer.append(readCollectionStats[MAX]).append("\r\n");
//				os.write(buffer.toString().getBytes());
//				createDataObjectStats = new long[] { 0, 0, 0, 0 };
//				addElementStats = new long[] { 0, 0, 0, 0 };
//				readCollectionStats = new long[] { 0, 0, 0, 0 };
//			}

		}

	}

}