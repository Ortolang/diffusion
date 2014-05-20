package fr.ortolang.diffusion.test.usecase;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

import fr.ortolang.diffusion.test.bench.BenchSuite;
import fr.ortolang.diffusion.test.bench.CookieFilter;

public class WorkAndPublishUseCase {
	
	private static final String PROJECT_ROOT_FOLDER = "/mnt/space/jerome/Data/PROJECT";

	private static Logger logger = Logger.getLogger(WorkAndPublishUseCase.class.getName());
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

	@Test
	public void scenario() throws IOException {
		logger.log(Level.INFO, "Starting Work And Publish Scenario");

		WebTarget profiles = base.path("/membership/profiles");
		WebTarget projects = base.path("/collaboration/projects");
		WebTarget collections = base.path("/core/collections");
		WebTarget objects = base.path("/core/objects");
		WebTarget metadatas = base.path("/core/metadatas");

		Response connectedProfileResponse = profiles.path("connected").request(MediaType.APPLICATION_JSON_TYPE).get();
		if (connectedProfileResponse.getStatus() == Status.OK.getStatusCode()) {
			String connectedProfileObject = connectedProfileResponse.readEntity(String.class);
			JsonObject jsonRootCollectionObject = Json.createReader(new StringReader(connectedProfileObject)).readObject();
			String connectedProfileKey = jsonRootCollectionObject.getJsonString("key").getString();
			if ( !connectedProfileKey.equals(BenchSuite.USERID)) {
				fail("Connected Profile seems to be bad, found wrong identifier key in the profile object : " + connectedProfileKey);
			}
		} else {
			logger.log(Level.WARNING, "Unexpected response code while getting connected identifier : " + connectedProfileResponse.getStatus());
			fail("Unable to get Connected Identifier");
		}

		logger.log(Level.INFO, "Creating project");
		Form newProjectForm = new Form().param("name", "benchmark project " + System.currentTimeMillis()).param("type", "corpus");
		Response newProjectResponse = projects.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(newProjectForm, MediaType.APPLICATION_FORM_URLENCODED));
		if (newProjectResponse.getStatus() != Status.CREATED.getStatusCode()) {
			logger.log(Level.WARNING, "Unexpected response code while trying to create project : " + newProjectResponse.getStatus());
			fail("Unable to create project");
		}
		String projectKey = newProjectResponse.getLocation().getPath().substring(newProjectResponse.getLocation().getPath().lastIndexOf("/")+1);
		logger.log(Level.INFO, "Created project key : " + projectKey);
		
		logger.log(Level.INFO, "Getting project root collection");
		Response getProjectRootResponse = projects.path(projectKey).path("root").request(MediaType.APPLICATION_JSON_TYPE).get();
		String projectRootKey = "";
		if (getProjectRootResponse.getStatus() == Status.OK.getStatusCode()) {
			String rootCollectionObject = getProjectRootResponse.readEntity(String.class);
			JsonObject jsonRootCollectionObject = Json.createReader(new StringReader(rootCollectionObject)).readObject();
			projectRootKey = jsonRootCollectionObject.getJsonString("key").getString();
			logger.log(Level.INFO, "Root collection retreived : " + projectRootKey);
		} else {
			logger.log(Level.WARNING, "Unexpected response code while trying to read project root : " + getProjectRootResponse.getStatus());
			fail("Unable to get project root collection key");
		}
		logger.log(Level.INFO, "Root collection key : " + projectRootKey);
		
		Path projectFolder = Paths.get(PROJECT_ROOT_FOLDER);
		try ( DirectoryStream<Path> stream = Files.newDirectoryStream(projectFolder) ) {
			for ( Path file : stream ) {
				File thefile = file.toFile();
				if ( thefile.isFile() ) {
					logger.log(Level.FINE, "Creating DataObject for path : " + file);
					FileDataBodyPart filePart = new FileDataBodyPart("file", thefile);
					MultiPart multipart = new FormDataMultiPart()
				    .field("name", file.getFileName().toString())
				    .field("description", "A data object corresponding to provided file " + file.getFileName())
				    .bodyPart(filePart);
					Response createObjectResponse = objects.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(multipart, multipart.getMediaType()));
					if ( createObjectResponse.getStatus() != Status.CREATED.getStatusCode() ) {
						logger.log(Level.WARNING, "Unexpected response code while trying to create dataobject : " + createObjectResponse.getStatus() );
						fail("Unable to create dataobject");
					}
					String objectKey = createObjectResponse.getLocation().getPath().substring(createObjectResponse.getLocation().getPath().lastIndexOf("/"));
					logger.log(Level.FINE, "Created data object key : " + objectKey);
					
					logger.log(Level.FINE, "Adding the created dataobject as a member of root collection : " + projectRootKey);
					Response addToCollectionResponse = collections.path(projectRootKey).path("elements").path(objectKey).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(objectKey, MediaType.TEXT_PLAIN));
					if ( addToCollectionResponse.getStatus() != Status.NO_CONTENT.getStatusCode() ) {
						logger.log(Level.WARNING, "Unexpected response code while trying to add element to collection : " + addToCollectionResponse.getStatus() );
						fail("Unable to add element to collection");
					}
				}
			}
		}
		
		logger.log(Level.INFO, "Creating release");
		Form newReleaseForm = new Form().param("name", "v1.0").param("type", "release");
		Response newReleaseResponse = projects.path(projectKey).path("versions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(newReleaseForm, MediaType.APPLICATION_FORM_URLENCODED));
		if (newReleaseResponse.getStatus() != Status.CREATED.getStatusCode()) {
			logger.log(Level.WARNING, "Unexpected response code while trying to release project : " + newReleaseResponse.getStatus());
			logger.log(Level.WARNING, "entity: " + newReleaseResponse.readEntity(String.class)); 
			fail("Unable to release project");
		}
		String releaseKey = newReleaseResponse.getLocation().getPath().substring(newReleaseResponse.getLocation().getPath().lastIndexOf("/")+1);
		logger.log(Level.INFO, "Created release key (should be same than old root collection): " + releaseKey);
		
		
		
	}

}
