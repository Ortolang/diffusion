package fr.ortolang.diffusion.store.triple;

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
			object.setHidden(false);
			object.setLocked(false);
			object.setStatus("DRAFT");
			object.setAuthor("jayblanc");
			object.setCreationDate(System.currentTimeMillis());
			object.setLastModificationDate(System.currentTimeMillis());
			object.setProperties(Arrays.asList(new OrtolangObjectProperty[] {new OrtolangObjectProperty("foo", "bar")} ));
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
