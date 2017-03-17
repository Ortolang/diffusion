package fr.ortolang.diffusion.store.index;

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

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangSearchResult;

public class IndexStoreServiceTest {
	
	//TODO uses the same method to test elasticSearch service
	
//	private static final Logger LOGGER = Logger.getLogger(IndexStoreServiceTest.class.getName());
//	private IndexStoreServiceBean service;
//	
//	@BeforeClass
//	public static void globalSetup() {
//		try {
//			LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
//		} catch (SecurityException | IOException e) {
//			LOGGER.log(Level.SEVERE, e.getMessage(), e);
//		}
//	}
//	
//	@Before
//	public void setup() {
//		try {
//		    service = new IndexStoreServiceBean();
//		    Files.walkFileTree(service.getBase(), new FileVisitor<Path>() {
//                @Override
//                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                    Files.delete(file);
//                    return FileVisitResult.CONTINUE;
//                }
//
//                @Override
//                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//                    LOGGER.log(Level.SEVERE, "unable to purge temporary created filesystem", exc);
//                    return FileVisitResult.TERMINATE;
//                }
//
//                @Override
//                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//                    Files.delete(dir);
//                    return FileVisitResult.CONTINUE;
//                }
//                
//            });
//			service.init();
//		} catch (Exception e) {
//			fail(e.getMessage());
//		}
//	}
//	
//	@After
//	public void tearDown() {
//		try {
//			service.shutdown();
//			Files.walkFileTree(service.getBase(), new FileVisitor<Path>() {
//				@Override
//				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//					return FileVisitResult.CONTINUE;
//				}
//
//				@Override
//				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//					Files.delete(file);
//					return FileVisitResult.CONTINUE;
//				}
//
//				@Override
//				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//					LOGGER.log(Level.SEVERE, "unable to purge temporary created filesystem", exc);
//					return FileVisitResult.TERMINATE;
//				}
//
//				@Override
//				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//					Files.delete(dir);
//					return FileVisitResult.CONTINUE;
//				}
//				
//			});
//		} catch (IOException e) {
//			LOGGER.log(Level.SEVERE, e.getMessage(), e);
//		}
//	}
//	
//	@Test
//    public void testIndexDocument() {
//		IndexablePlainTextContent content = new IndexablePlainTextContent();
//		content.addContentPart("tagada");
//		content.addContentPart("ceci est une petite phrase");
//		content.addContentPart("qui dure longtemps...");
//		OrtolangIndexableObject<IndexablePlainTextContent> object = new OrtolangIndexableObject<IndexablePlainTextContent>();
//		object.setKey("K1");
//		object.setIdentifier(new OrtolangObjectIdentifier("service", "type", "id1"));
//		object.setService("service");
//		object.setType("type");
//		object.setHidden(false);
//		object.setLocked(false);
//		object.setStatus("draft");
//		object.setProperties(Arrays.asList(new OrtolangObjectProperty[] {new OrtolangObjectProperty("AUTHOR", "jayblanc")} ));
//		object.setContent(content);
//		
//		try {
//			service.index(object);
//			List<OrtolangSearchResult> results = service.search("tagada");
//			dumpResults(results);
//			assertEquals(1, results.size());
//			results = service.search("PROPERTY.AUTHOR:jayblanc");
//			dumpResults(results);
//			assertEquals(1, results.size());
//			results = service.search("STATUS:DRAFT");
//			dumpResults(results);
//			assertEquals(1, results.size());
//		} catch (IndexStoreServiceException e) {
//			fail(e.getMessage());
//		}
//	}
//	
//	@Test
//    public void testReindexDocument() {
//		IndexablePlainTextContent content = new IndexablePlainTextContent();
//		content.addContentPart("tagada");
//		content.addContentPart("ceci est une petite phrase");
//		content.addContentPart("qui dure longtemps...");
//		OrtolangIndexableObject<IndexablePlainTextContent> object = new OrtolangIndexableObject<IndexablePlainTextContent>();
//		object.setKey("K1");
//		object.setIdentifier(new OrtolangObjectIdentifier("service", "type", "id1"));
//		object.setService("service");
//		object.setType("type");
//		object.setHidden(false);
//		object.setLocked(false);
//		object.setStatus("DRAFT");
//		object.setProperties(Arrays.asList(new OrtolangObjectProperty[] {new OrtolangObjectProperty("AUTHOR", "jayblanc")} ));
//		object.setContent(content);
//		
//		try {
//			service.index(object);
//			List<OrtolangSearchResult> results = service.search("tagada");
//			dumpResults(results);
//			assertEquals(1, results.size());
//			
//			results = service.search("bidules");
//			dumpResults(results);
//			assertEquals(0, results.size());
//			
//			object.getContent().addContentPart("avec des bidules en plus !");
//			service.index(object);
//			
//			results = service.search("bidules");
//			dumpResults(results);
//			assertEquals(1, results.size());
//		} catch (IndexStoreServiceException e) {
//			fail(e.getMessage());
//		}
//	}
//	
//	@Test
//    public void testRemoveDocument() {
//		IndexablePlainTextContent content = new IndexablePlainTextContent();
//		content.addContentPart("tagada");
//		content.addContentPart("ceci est une petite phrase");
//		content.addContentPart("qui dure longtemps...");
//		OrtolangIndexableObject<IndexablePlainTextContent> object = new OrtolangIndexableObject<IndexablePlainTextContent>();
//		object.setKey("K1");
//		object.setIdentifier(new OrtolangObjectIdentifier("service", "type", "id1"));
//		object.setService("service");
//		object.setType("type");
//		object.setHidden(false);
//		object.setLocked(false);
//		object.setStatus("DRAFT");
//		object.setProperties(Arrays.asList(new OrtolangObjectProperty[] {new OrtolangObjectProperty("AUTHOR", "jayblanc")} ));
//		object.setContent(content);
//		
//		try {
//			service.index(object);
//			List<OrtolangSearchResult> results = service.search("tagada");
//			dumpResults(results);
//			assertEquals(1, results.size());
//			
//			service.remove("K1");
//			
//			results = service.search("tagada");
//			dumpResults(results);
//			assertTrue(results.size() <= 1);
//			assertEquals(0, results.size());
//		} catch (IndexStoreServiceException e) {
//			fail(e.getMessage());
//		}
//	}
//	
//	private void dumpResults(List<OrtolangSearchResult> results) {
//		StringBuffer dump = new StringBuffer();
//		dump.append(results.size()).append(" results found :");
//		for ( OrtolangSearchResult result : results ) {
//			dump.append("\r\n").append(result.toString());
//		}
//		LOGGER.log(Level.FINE, dump.toString());
//	}
}
