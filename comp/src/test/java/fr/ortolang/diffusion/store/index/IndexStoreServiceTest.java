package fr.ortolang.diffusion.store.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.ortolang.diffusion.OrtolangIndexablePlainTextContent;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.indexing.IndexingContext;

public class IndexStoreServiceTest {
	
	private Logger logger = Logger.getLogger(IndexStoreServiceTest.class.getName());
	private IndexStoreServiceBean service;
	
	@BeforeClass
	public static void globalSetup() {
		try {
			LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Before
	public void setup() {
		try {
			service = new IndexStoreServiceBean();
			service.init();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@After
	public void tearDown() {
		try {
			service.shutdown();
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
    public void testIndexDocument() {
		OrtolangIndexablePlainTextContent content = new OrtolangIndexablePlainTextContent();
		content.addContentPart("tagada");
		content.addContentPart("ceci est une petite phrase");
		content.addContentPart("qui dure longtemps...");
		OrtolangIndexableObject object = new OrtolangIndexableObject();
		object.setKey("K1");
		object.setIdentifier(new OrtolangObjectIdentifier("service", "type", "id1"));
		object.setService("service");
		object.setType("type");
		object.setName("the name");
		object.setHidden(false);
		object.setLocked(false);
		object.setStatus("draft");
		object.setProperties(Arrays.asList(new OrtolangObjectProperty[] {new OrtolangObjectProperty("AUTHOR", "jayblanc")} ));
		object.setPlainTextContent(content);
		object.setContext(new IndexingContext("root", "/path"));
		
		try {
			service.index(object);
			List<OrtolangSearchResult> results = service.search("tagada");
			dumpResults(results);
			assertEquals(1, results.size());
			results = service.search("PROPERTY.AUTHOR:jayblanc");
			dumpResults(results);
			assertEquals(1, results.size());
			results = service.search("STATUS:DRAFT");
			dumpResults(results);
			assertEquals(1, results.size());
		} catch (IndexStoreServiceException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
    public void testReindexDocument() {
		OrtolangIndexablePlainTextContent content = new OrtolangIndexablePlainTextContent();
		content.addContentPart("tagada");
		content.addContentPart("ceci est une petite phrase");
		content.addContentPart("qui dure longtemps...");
		OrtolangIndexableObject object = new OrtolangIndexableObject();
		object.setKey("K1");
		object.setIdentifier(new OrtolangObjectIdentifier("service", "type", "id1"));
		object.setService("service");
		object.setType("type");
		object.setName("the name");
		object.setHidden(false);
		object.setLocked(false);
		object.setStatus("DRAFT");
		object.setProperties(Arrays.asList(new OrtolangObjectProperty[] {new OrtolangObjectProperty("AUTHOR", "jayblanc")} ));
		object.setPlainTextContent(content);
		object.setContext(new IndexingContext("root", "/path"));
		
		try {
			service.index(object);
			List<OrtolangSearchResult> results = service.search("tagada");
			dumpResults(results);
			assertEquals(1, results.size());
			
			results = service.search("bidules");
			dumpResults(results);
			assertEquals(0, results.size());
			
			object.getPlainTextContent().addContentPart("avec des bidules en plus !");
			service.reindex(object);
			
			results = service.search("bidules");
			dumpResults(results);
			assertEquals(1, results.size());
		} catch (IndexStoreServiceException e) {
			fail(e.getMessage());
		}
	}
	
	@Test
    public void testRemoveDocument() {
		OrtolangIndexablePlainTextContent content = new OrtolangIndexablePlainTextContent();
		content.addContentPart("tagada");
		content.addContentPart("ceci est une petite phrase");
		content.addContentPart("qui dure longtemps...");
		OrtolangIndexableObject object = new OrtolangIndexableObject();
		object.setKey("K1");
		object.setIdentifier(new OrtolangObjectIdentifier("service", "type", "id1"));
		object.setService("service");
		object.setType("type");
		object.setName("the name");
		object.setHidden(false);
		object.setLocked(false);
		object.setStatus("DRAFT");
		object.setProperties(Arrays.asList(new OrtolangObjectProperty[] {new OrtolangObjectProperty("AUTHOR", "jayblanc")} ));
		object.setPlainTextContent(content);
		object.setContext(new IndexingContext("root", "/path"));
		
		try {
			service.index(object);
			List<OrtolangSearchResult> results = service.search("tagada");
			dumpResults(results);
			assertEquals(1, results.size());
			
			service.remove("K1");
			
			results = service.search("tagada");
			dumpResults(results);
			assertTrue(results.size() <= 1);
			assertEquals(0, results.size());
		} catch (IndexStoreServiceException e) {
			fail(e.getMessage());
		}
	}
	
	private void dumpResults(List<OrtolangSearchResult> results) {
		StringBuffer dump = new StringBuffer();
		dump.append(results.size() + " results found :");
		for ( OrtolangSearchResult result : results ) {
			dump.append("\r\n").append(result.toString());
		}
		logger.log(Level.FINE, dump.toString());
	}
}
