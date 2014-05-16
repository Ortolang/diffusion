package fr.ortolang.diffusion.store.triple;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexablePlainTextContent;
import fr.ortolang.diffusion.OrtolangIndexableSemanticContent;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;

public class TripleStoreServiceTest {

	private Logger logger = Logger.getLogger(TripleStoreServiceTest.class.getName());
	private TripleStoreServiceBean service;

	@Before
	public void setup() {
		try {
			service = new TripleStoreServiceBean();
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
					logger.log(Level.SEVERE, "unable to purge temporary created filesystem", exc);
					return FileVisitResult.TERMINATE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}

			});
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Test
	public void testInsertAndRemoveTriples() {
		try {
			OrtolangIndexablePlainTextContent content = new OrtolangIndexablePlainTextContent();
			content.addContentPart("tagada");
			content.addContentPart("ceci est une petite phrase");
			content.addContentPart("qui dure longtemps...");
			OrtolangIndexableSemanticContent scontent = new OrtolangIndexableSemanticContent();
			scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/description", "W3Schools - Free tutorials"));
			scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/publisher", "Refsnes Data as"));
			scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/date", "2008-09-01"));
			scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/type", "Web Development"));
			scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/format", "text/html"));
			scontent.addTriple(new Triple("http://www.w3schools.com", "http://purl.org/dc/elements/1.1/language", "en"));
			OrtolangIndexableObject object = new OrtolangIndexableObject();
			object.setKey("K1");
			object.setIdentifier(new OrtolangObjectIdentifier("service", "type", "id1"));
			object.setService("service");
			object.setType("type");
			object.setName("the name");
			object.setDeleted(false);
			object.setHidden(false);
			object.setLocked(false);
			object.setStatus("DRAFT");
			object.setProperties(Arrays.asList(new OrtolangObjectProperty[] {new OrtolangObjectProperty("AUTHOR", "jayblanc")} ));
			object.setPlainTextContent(content);
			object.setSemanticContent(scontent);
			
			String query = "SELECT ?x WHERE { ?x  <http://purl.org/dc/elements/1.1/language>  \"en\" }";
			
			service.index(object);
			String result = service.query("SPARQL", query, "xml");
			assertTrue(result.contains("<uri>http://www.w3schools.com</uri>"));
			
			service.remove("K1");
			result = service.query("SPARQL", query, "xml");
			assertFalse(result.contains("<uri>http://www.w3schools.com</uri>"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
}
