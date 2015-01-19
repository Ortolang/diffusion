package fr.ortolang.diffusion.api.usecase;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.ortolang.diffusion.api.builder.WorkspaceBuilder;
import fr.ortolang.diffusion.api.client.OrtolangRestClient;
import fr.ortolang.diffusion.api.client.OrtolangRestClientException;
import fr.ortolang.diffusion.api.config.ClientConfig;

public class WorkAndPublishUseCase {
	
	private static Logger logger = Logger.getLogger(WorkAndPublishUseCase.class.getName());

	@BeforeClass
	public static void init() {
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(ClientConfig.USERID, ClientConfig.PASSWORD);

	}

	@AfterClass
	public static void shutdown() {
//		client.close();
	}

	@Test
	public void scenario() throws IOException, OrtolangRestClientException {
		logger.log(Level.INFO, "Starting Work And Publish Scenario");
		
		String username = ClientConfig.USERID;
		String password = ClientConfig.PASSWORD;
		String baseUrl = "http://localhost:8080/api/rest";
		String wsKey = "frantext3";
		WorkspaceBuilder frantext = new WorkspaceBuilder(username, password, baseUrl, wsKey, "src/test/resources/samples/bag");
		frantext.run();
		
		// Publish
		OrtolangRestClient user1 = new OrtolangRestClient(username, password, baseUrl);
		
		try {
			user1.checkAuthentication();
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "authentication failed: " + e.getMessage(), e);
		}
		
		JsonObject wsJsonObject = user1.readWorkspace(wsKey);
		String head = wsJsonObject.getJsonString("head").getString();
		
		StringBuffer keys = listKeys(user1, head, new StringBuffer());
		keys.deleteCharAt(keys.length()-1);
		logger.log(Level.INFO, "keys " + keys);
		
		// Publish
		OrtolangRestClient admin = new OrtolangRestClient(ClientConfig.ROOT_ID, ClientConfig.ROOT_PASSWORD, "http://localhost:8080/api/rest");
		
		try {
			admin.checkAuthentication();
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "authentication failed: " + e.getMessage(), e);
		}
		
		Map<String, String> params = new HashMap<String, String> ();
		params.put("keys", keys.toString());
		Map<String, File> attachments = new HashMap<String, File> ();
		admin.createProcess("simple-publication", "Release " + wsKey, params, attachments);
		
//		// Waiting until the process is finished
//		try {
//			logger.log(Level.INFO, "Waiting process");
//			Thread.sleep(10000);
//		} catch (InterruptedException e) {
//			logger.log(Level.SEVERE, "Cannot wait !!");
//			fail("Unable to wait process");
//		}
//		
		//TODO checks process

//		//TODO list projects from triplestore
//		logger.log(Level.INFO, "Listing project publised");
//		String queryListRootCollection = "SELECT ?subj ?title ?description WHERE {?subj <http://www.ortolang.fr/2014/05/diffusion#type> \"collection\" ; <http://purl.org/dc/elements/1.1/title> ?title ; <http://purl.org/dc/elements/1.1/description> ?description }";
//		JsonObject jsonSPARQLResultObject = semanticSearch(queryListRootCollection);
//		JsonObject resultsSPARQL = jsonSPARQLResultObject.getJsonObject("results");
//		JsonArray bindings = resultsSPARQL.getJsonArray("bindings");
//		logger.log(Level.INFO, "bindings : "+bindings);
//		assertTrue("List of project is empty",bindings.size()>0);
//		
//		logger.log(Level.INFO, "Getting triples about root collection");
//		String queryCollectionDetails = "SELECT ?pred ?obj WHERE {<http://localhost:8080/diffusion/rest/objects/"+projectRootKey+"> ?pred ?obj }";
//		jsonSPARQLResultObject = semanticSearch(queryCollectionDetails);
//		resultsSPARQL = jsonSPARQLResultObject.getJsonObject("results");
//		bindings = resultsSPARQL.getJsonArray("bindings");
//		assertTrue("Informations about project is empty", bindings.size()>0);
//		
		user1.close();
		admin.close();
	}
	
	protected StringBuffer listKeys(OrtolangRestClient client, String key, StringBuffer buffer) {
		
		try {
			JsonObject objectJsonObject = client.getObject(key);
			String type = objectJsonObject.getJsonString("type").getString();
			
			buffer.append(key).append(",");

			JsonObject oobject = objectJsonObject.getJsonObject("object");
			
			if(type.equals("collection")) {
				JsonArray elements = oobject.getJsonArray("elements");
				
				for(JsonValue element : elements) {
					if(element.getValueType() == ValueType.OBJECT) {
						listKeys(client, ((JsonObject) element).getString("key"), buffer);
					}
				}
			}
			
			JsonArray metadatas = oobject.getJsonArray("metadatas");
			for(JsonValue metadata : metadatas) {
				if(metadata.getValueType() == ValueType.OBJECT) {
					buffer.append(((JsonObject) metadata).getString("key")).append(",");
				}
			}
			
		} catch (OrtolangRestClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer;
	}

//	private JsonObject semanticSearch(String query) {
//		JsonObject jsonSPARQLResultObject = null;
//		Response getSemanticRootCollectionResponse = ortolangObjects.path("semantic").queryParam("query", UriComponent.encode(query, UriComponent.Type.QUERY_PARAM)).request(MediaType.APPLICATION_JSON_TYPE).get();
//		if (getSemanticRootCollectionResponse.getStatus() == Status.OK.getStatusCode()) {
//			String sparqlResponse = getSemanticRootCollectionResponse.readEntity(String.class);
//			jsonSPARQLResultObject = Json.createReader(new StringReader(sparqlResponse)).readObject();
//			logger.log(Level.INFO, jsonSPARQLResultObject.toString());
//			//TODO check that the metadata project is published
//			//statusProcessPublish = jsonProcessObject.getJsonString("status").getString();
//			//logger.log(Level.INFO, "Process retreived : " + projectRootKey);
//		} else {
//			logger.log(Level.WARNING, "Unexpected response code while trying to get sparql response : " + getSemanticRootCollectionResponse.getStatus());
//			logger.log(Level.WARNING, "entity: " + getSemanticRootCollectionResponse.readEntity(String.class)); 
//			fail("Unable to get SPARQL Result");
//		}
//		return jsonSPARQLResultObject;
//	}
}
