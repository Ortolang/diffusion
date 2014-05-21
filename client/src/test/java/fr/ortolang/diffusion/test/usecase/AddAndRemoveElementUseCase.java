package fr.ortolang.diffusion.test.usecase;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
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

public class AddAndRemoveElementUseCase {
	
	private static final String SAMPLE_FILE = "/mnt/space/jerome/Data/sample.txt";
	
	private static Logger logger = Logger.getLogger(AddAndRemoveElementUseCase.class.getName());
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
		logger.log(Level.INFO, "Starting Add and Remove Elements Scenario");

		WebTarget profiles = base.path("/membership/profiles");
		WebTarget collections = base.path("/core/collections");
		WebTarget objects = base.path("/core/objects");
		
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

		logger.log(Level.INFO, "Creating collection");
		Form newCollectionForm = new Form().param("name", "test collection " + System.currentTimeMillis()).param("description", "A collection to perform add and remove tests");
		Response newCollectionResponse = collections.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(newCollectionForm, MediaType.APPLICATION_FORM_URLENCODED));
		if (newCollectionResponse.getStatus() != Status.CREATED.getStatusCode()) {
			logger.log(Level.WARNING, "Unexpected response code while trying to create collection : " + newCollectionResponse.getStatus() + " - " + newCollectionResponse.getEntity());
			fail("Unable to create project");
		}
		String collectionKey = newCollectionResponse.getLocation().getPath().substring(newCollectionResponse.getLocation().getPath().lastIndexOf("/"));
		logger.log(Level.INFO, "Created collection key : " + collectionKey);
		
		Set<String> elements = new HashSet<String> ();
		for (int i = 0; i < 2500; i++) {
			logger.log(Level.FINE, "Creating sample DataObject to populate collection");
			File thefile = Paths.get(SAMPLE_FILE).toFile();
			FileDataBodyPart filePart = new FileDataBodyPart("file", thefile);
			MultiPart newObjectForm = new FormDataMultiPart().field("name", thefile.getName().toString())
					.field("description", "A data object corresponding to the sample file " + thefile.getName()).bodyPart(filePart);
			Response newObjectResponse = objects.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(newObjectForm, newObjectForm.getMediaType()));
			if (newObjectResponse.getStatus() != Status.CREATED.getStatusCode()) {
				logger.log(Level.WARNING, "Unexpected response code while trying to create dataobject : " + newObjectResponse.getStatus());
				fail("Unable to create dataobject");
			}
			String objectKey = newObjectResponse.getLocation().getPath().substring(newObjectResponse.getLocation().getPath().lastIndexOf("/"));
			logger.log(Level.FINE, "Created data object key : " + objectKey);
			elements.add(objectKey);
			
			logger.log(Level.FINE, "Adding the created dataobject as a collection element");
			Response addElementResponse = collections.path(collectionKey).path("elements").path(objectKey).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.entity(objectKey, MediaType.TEXT_PLAIN));
			if (addElementResponse.getStatus() != Status.NO_CONTENT.getStatusCode()) {
				logger.log(Level.WARNING, "Unexpected response code while trying to add element to collection : " + addElementResponse.getStatus());
				fail("Unable to add element to collection");
			}
		}
		
		for (String element : elements) {
			logger.log(Level.FINE, "Removing dataobject from collection");
			Response removeElementResponse = collections.path(collectionKey).path("elements").path(element).request(MediaType.APPLICATION_JSON_TYPE).delete();
			if (removeElementResponse.getStatus() != Status.NO_CONTENT.getStatusCode()) {
				logger.log(Level.WARNING, "Unexpected response code while trying to remove element from collection : " + removeElementResponse.getStatus());
				fail("Unable to remove element from collection");
			}
		}
		
		
	}


}
