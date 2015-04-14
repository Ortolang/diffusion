package fr.ortolang.diffusion.store.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;
import fr.ortolang.diffusion.store.json.JsonStoreServiceBean;
import fr.ortolang.diffusion.store.triple.IndexableSemanticContent;
import fr.ortolang.diffusion.store.triple.Triple;

public class JsonStoreServiceTest {

	private static final Logger LOGGER = Logger.getLogger(JsonStoreServiceTest.class.getName());
	private JsonStoreServiceBean service;

	@Before
	public void setup() {
		try {
			service = new JsonStoreServiceBean();
			service.init();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@After
	public void tearDown() {
		service.shutdown();
		try {
			Files.walkFileTree(service.getBase(), new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					LOGGER.log(Level.SEVERE, "unable to purge temporary created filesystem", exc);
					return FileVisitResult.TERMINATE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}

			});
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	@Test
	public void testInsert() {
		OrtolangIndexableObject<IndexableJsonContent> object = getOrtolangIndexableObject();
		try {

			System.out.println(" =========");
			System.out.println(" = Index =");
			System.out.println(" =========");
			
			service.index(object);

			System.out.println(" =========");
			
			String query = "SELECT FROM type WHERE meta_format1.title = 'Dede'";
			List<String> results = service.search(query);
			for(String r : results) {
				System.out.println(r);
			}
			assertEquals(1, results.size());

			System.out.println(" =========");
			System.out.println(" = ReIndex =");
			System.out.println(" =========");
			
			// Changes a little bit
			IndexableJsonContent jsonContent = object.getContent();
			try {
				jsonContent.put("format1", new FileInputStream("src/test/resources/json/sample2.json"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
			
			service.reindex(object);
			System.out.println(" =========");
			
			String querySampleV2 = "SELECT FROM type WHERE meta_format1.title = 'Dede2'";
			List<String> resultsSampleV2 = service.search(querySampleV2);
			for(String r : resultsSampleV2) {
				System.out.println(r);
			}
			assertEquals(1, resultsSampleV2.size());

			System.out.println(" =========");
			System.out.println(" = Remove =");
			System.out.println(" =========");
			
			service.remove(object.getKey());
			System.out.println(" =========");
			
			List<String> resultsAfterRemove = service.search(querySampleV2);
			for(String r : resultsAfterRemove) {
				System.out.println(r);
			}
			assertEquals(0, resultsAfterRemove.size());
			
			
		} catch (JsonStoreServiceException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	private OrtolangIndexableObject<IndexableJsonContent> getOrtolangIndexableObject() {
		IndexablePlainTextContent content = new IndexablePlainTextContent();
		content.addContentPart("tagada");
		content.addContentPart("ceci est une petite phrase");
		content.addContentPart("qui dure longtemps...");
		IndexableSemanticContent scontent = new IndexableSemanticContent();
		scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/description", "W3Schools - Free tutorials"));
		scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/publisher", "Refsnes Data as"));
		scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/date", "2008-09-01"));
		scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/type", "Web Development"));
		scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/format", "text/html"));
		scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/language", "en"));
		IndexableJsonContent jsonContent = new IndexableJsonContent();
		try {
			jsonContent.put("format1", new FileInputStream("src/test/resources/json/sample.json"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		OrtolangIndexableObject<IndexableJsonContent> object = new OrtolangIndexableObject<IndexableJsonContent>();
		object.setKey("K1");
		object.setIdentifier(new OrtolangObjectIdentifier("service", "type", "id1"));
		object.setService("service");
		object.setType("type");
		object.setName("the name");
		object.setHidden(false);
		object.setLocked(false);
		object.setStatus("DRAFT");
		object.setAuthor("jayblanc");
		object.setCreationDate(System.currentTimeMillis());
		object.setLastModificationDate(System.currentTimeMillis());
		object.setProperties(Arrays.asList(new OrtolangObjectProperty[] {new OrtolangObjectProperty("foo", "bar")} ));
		object.setContent(jsonContent);
		
		return object;
	}
}
