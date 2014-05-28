package fr.ortolang.diffusion.test.bench;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
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

	private static final String[] SAMPLE_PROJECT_IDS = {"project10", "project20", "project50", "project100", "project200", "project500", "project1000", "project2000"};
	private static final String SAMPLE_PROJECT_BASE_PATH = "/mnt/space/jerome/Data/dataset";
	private static final String OUTPUT_FILE = "/mnt/space/jerome/Data/project-bench.csv";
	private static final int ITERATIONS = 3;
	private static final int CPT = 0;
	private static final int MIN = 1;
	private static final int SUM = 2;
	private static final int MAX = 3;

	private static Logger logger = Logger.getLogger(ProjectBench.class.getName());
	private static Client client;
	private static WebTarget base;

	private long[] createStats = new long[] { 0, 0, 0, 0 };
	private long[] releaseStats = new long[] { 0, 0, 0, 0 };

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
	 * Benchmark the creation, releasing and publishing time of a project
	 * 
	 * @throws IOException
	 */
	@Test
	public void bench() throws IOException {
		logger.log(Level.INFO, "Starting Project benchmark");

		try (OutputStream os = Files.newOutputStream(Paths.get(OUTPUT_FILE))) {

			os.write("size,create min,create avg,create max,release min,release avg,release max\r\n".getBytes());

			WebTarget profiles = base.path("/membership/profiles");
			WebTarget projects = base.path("/collaboration/projects");
			WebTarget collections = base.path("/core/collections");
			WebTarget objects = base.path("/core/objects");
			WebTarget metadatas = base.path("/core/metadatas");

			Response connectedProfileResponse = profiles.path("connected").request().accept(MediaType.APPLICATION_JSON_TYPE).get();
			if (connectedProfileResponse.getStatus() == Status.OK.getStatusCode()) {
				String connectedProfileObject = connectedProfileResponse.readEntity(String.class);
				JsonObject jsonConnectedProfileObject = Json.createReader(new StringReader(connectedProfileObject)).readObject();
				String connectedProfileKey = jsonConnectedProfileObject.getJsonString("key").getString();
				if (!connectedProfileKey.equals(BenchSuite.USERID)) {
					fail("Connected Profile seems to be bad, found wrong identifier key in the profile object : " + connectedProfileKey);
				}
			} else {
				logger.log(Level.WARNING, "Unexpected response code while getting connected identifier : " + connectedProfileResponse.getStatus());
				fail("Unable to get Connected Identifier");
			}

			for ( int k=0; k<SAMPLE_PROJECT_IDS.length; k++) {
				int projectSize = 0;
				createStats = new long[] { 0, 0, 0, 0 };
				releaseStats = new long[] { 0, 0, 0, 0 };
				
				for (int i = 0; i < ITERATIONS; i++) {
					projectSize = 0;
					long start = System.currentTimeMillis();

					logger.log(Level.INFO, "Creating project " + SAMPLE_PROJECT_IDS[k] + " ITERATION " + i);
					Form newprojectForm = new Form().param("name", SAMPLE_PROJECT_IDS[k] + System.currentTimeMillis()).param("type", "benchmark");
					Response newprojectResponse = projects.request().accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(newprojectForm, MediaType.APPLICATION_FORM_URLENCODED));
					if (newprojectResponse.getStatus() != Status.CREATED.getStatusCode()) {
						logger.log(Level.WARNING, "Unexpected response code while trying to create project : " + newprojectResponse.getStatus());
						fail("Unable to create project");
					}
					String projectkey = newprojectResponse.getLocation().getPath().substring(newprojectResponse.getLocation().getPath().lastIndexOf("/") + 1);
					logger.log(Level.INFO, "Created project key : " + projectkey);

					logger.log(Level.FINE, "Reading project root collection");
					Response rootcollectionResponse = projects.path(projectkey).path("root").request().accept(MediaType.MEDIA_TYPE_WILDCARD).get();
					String rootCollectionKey = "";
					if (rootcollectionResponse.getStatus() == Status.OK.getStatusCode()) {
						String rootcollectionObject = rootcollectionResponse.readEntity(String.class);
						JsonObject jsonRootCollectionObject = Json.createReader(new StringReader(rootcollectionObject)).readObject();
						rootCollectionKey = jsonRootCollectionObject.getJsonString("key").getString();
					} else {
						logger.log(Level.WARNING, "Unexpected response code while trying to read project root collection : " + rootcollectionResponse.getStatus());
						fail("Unable to read root collection for project");
					}
					logger.log(Level.FINE, "Project root collection key : " + rootCollectionKey);
					
					logger.log(Level.FINE, "Creating project metadatas");
					File projectMetadataFile = Paths.get(SAMPLE_PROJECT_BASE_PATH, SAMPLE_PROJECT_IDS[i], SAMPLE_PROJECT_IDS[i] + "-md.xml").toFile();
					FileDataBodyPart projectMetadataFilePart = new FileDataBodyPart("file", projectMetadataFile);
					MultiPart projectMetadataForm = new FormDataMultiPart().field("name", "Metadatas of project " + SAMPLE_PROJECT_IDS[i])
							.field("target", rootCollectionKey).bodyPart(projectMetadataFilePart);
					Response projectMetadataResponse = metadatas.request().accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(projectMetadataForm, projectMetadataForm.getMediaType()));
					if (projectMetadataResponse.getStatus() != Status.CREATED.getStatusCode()) {
						logger.log(Level.WARNING, "Unexpected response code while trying to create project metadatas : " + projectMetadataResponse.getStatus());
						fail("Unable to create project metadatas");
					}
					String projectMetadataKey = projectMetadataResponse.getLocation().getPath().substring(projectMetadataResponse.getLocation().getPath().lastIndexOf("/") + 1);
					logger.log(Level.FINE, "Created project metadata key : " + projectMetadataKey);
					
					File[] projectFiles = Paths.get(SAMPLE_PROJECT_BASE_PATH, SAMPLE_PROJECT_IDS[k], "objects").toFile().listFiles();
					for ( int j=0; j<projectFiles.length; j++ ) {
						logger.log(Level.FINE, "Creating project dataobject");
						File thefile = projectFiles[j];
						FileDataBodyPart filePart = new FileDataBodyPart("file", thefile);
						MultiPart newobjectForm = new FormDataMultiPart().field("name", thefile.getName().toString())
								.field("description", "A dataobject corresponding to the sample file " + thefile.getName()).bodyPart(filePart);
						Response newobjectResponse = objects.request().accept(MediaType.APPLICATION_JSON).post(Entity.entity(newobjectForm, newobjectForm.getMediaType()));
						if (newobjectResponse.getStatus() != Status.CREATED.getStatusCode()) {
							logger.log(Level.WARNING, "Unexpected response code while trying to create dataobject : " + newobjectResponse.getStatus());
							fail("Unable to create dataobject");
						}
						String objectkey = newobjectResponse.getLocation().getPath().substring(newobjectResponse.getLocation().getPath().lastIndexOf("/")+1);
						logger.log(Level.FINE, "Created data object key : " + objectkey);
						
						logger.log(Level.FINE, "Adding the created dataobject as a collection element");
						Response addobjectResponse = collections.path(rootCollectionKey).path("elements").path(objectkey).request().accept(MediaType.APPLICATION_JSON)
								.put(Entity.entity(objectkey, MediaType.TEXT_PLAIN));
						if (addobjectResponse.getStatus() != Status.NO_CONTENT.getStatusCode()) {
							logger.log(Level.WARNING, "Unexpected response code while trying to add element to collection : " + addobjectResponse.getStatus());
							fail("Unable to add element to collection");
						}
						logger.log(Level.FINE, "DataObject added to collection");
						
						File metadata = Paths.get(SAMPLE_PROJECT_BASE_PATH, SAMPLE_PROJECT_IDS[k], "metadatas", thefile.getName() + ".xml").toFile();
						if ( metadata.exists() ) {
							logger.log(Level.FINE, "Creating metadataobject");
							FileDataBodyPart metafilePart = new FileDataBodyPart("file", metadata);
							MultiPart newmetadataForm = new FormDataMultiPart().field("name", thefile.getName().toString())
									.field("target", objectkey).bodyPart(filePart);
							Response newmetadataResponse = metadatas.request().accept(MediaType.APPLICATION_JSON).post(Entity.entity(newmetadataForm, newmetadataForm.getMediaType()));
							if (newmetadataResponse.getStatus() != Status.CREATED.getStatusCode()) {
								logger.log(Level.WARNING, "Unexpected response code while trying to create metadataobject : " + newmetadataResponse.getStatus());
								fail("Unable to create metadataobject");
							}
							String metadataobjectkey = newmetadataResponse.getLocation().getPath().substring(newmetadataResponse.getLocation().getPath().lastIndexOf("/")+1);
							logger.log(Level.FINE, "Created metadata object key : " + metadataobjectkey);
						}
						projectSize++;
					}
					long stop = System.currentTimeMillis();
					long time = stop - start;
					createStats[CPT]++;
					if (createStats[MIN] <= 0 || time < createStats[MIN]) {
						createStats[MIN] = time;
					}
					createStats[SUM] += time;
					if (time > createStats[MAX]) {
						createStats[MAX] = time;
					}
					
					start = System.currentTimeMillis();
					logger.log(Level.FINE, "Releasing project");
					Form releaseProjectForm = new Form().param("name", "V" + i + ".0").param("type", "RELEASE");
					Response newReleaseResponse = projects.path(projectkey).path("versions").request().accept(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(releaseProjectForm, MediaType.APPLICATION_FORM_URLENCODED));
					if (newReleaseResponse.getStatus() != Status.CREATED.getStatusCode()) {
						logger.log(Level.WARNING, "Unexpected response code while trying to release project : " + newReleaseResponse.getStatus());
						fail("Unable to release project");
					}
					logger.log(Level.INFO, "Project released");
					stop = System.currentTimeMillis();
					time = stop - start;
					releaseStats[CPT]++;
					if (releaseStats[MIN] <= 0 || time < releaseStats[MIN]) {
						releaseStats[MIN] = time;
					}
					releaseStats[SUM] += time;
					if (time > releaseStats[MAX]) {
						releaseStats[MAX] = time;
					}
				}
			
				StringBuffer buffer = new StringBuffer();
				buffer.append(projectSize).append(",");
				buffer.append(createStats[MIN]).append(",");
				buffer.append((createStats[SUM] / createStats[CPT])).append(",");
				buffer.append(createStats[MAX]).append(",");
				buffer.append(releaseStats[MIN]).append(",");
				buffer.append((releaseStats[SUM] / releaseStats[CPT])).append(",");
				buffer.append(releaseStats[MAX]).append("\r\n");
				os.write(buffer.toString().getBytes());
			}
		}

	}

}
