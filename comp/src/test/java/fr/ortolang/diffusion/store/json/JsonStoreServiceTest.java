package fr.ortolang.diffusion.store.json;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;

public class JsonStoreServiceTest {

	private static final Logger LOGGER = Logger.getLogger(JsonStoreServiceTest.class.getName());
	private static JsonStoreServiceBean service;

	@BeforeClass
	public static void setup() {
		try {
			service = new JsonStoreServiceBean();
			service.init();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@AfterClass
	public static void tearDown() {
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
			
			service.index(object);
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
	
	@Test
    public void testSystemRead() {
	    String key = "K1";
	    try {
	        OrtolangIndexableObject<IndexableJsonContent> object = getOrtolangIndexableObject();
	        service.index(object);
	        String document = service.systemGetDocument(key);
	        System.out.println("DOC: " + document);
	        assertTrue(document.contains("K1"));
	    } catch (JsonStoreServiceException e) {
	        fail("unable to admin document: " + e.getMessage());
	    }
	}
	
	private OrtolangIndexableObject<IndexableJsonContent> getOrtolangIndexableObject() {
		IndexablePlainTextContent content = new IndexablePlainTextContent();
		content.addContentPart("tagada");
		content.addContentPart("ceci est une petite phrase");
		content.addContentPart("qui dure longtemps...");
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
