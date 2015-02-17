package fr.ortolang.diffusion.store.binary;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BinaryStoreServiceTest {

	private Logger logger = Logger.getLogger(BinaryStoreServiceTest.class.getName());
	private BinaryStoreServiceBean service;
	
	@Before
	public void setup() {
		try {
			service = new BinaryStoreServiceBean();
			service.init();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@After
	public void tearDown() {
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
	public void testHash() {
		byte[] content1 = "Sample Digital Content v1.0".getBytes();
		byte[] content2 = "Sample Digital Content v1.0".getBytes();
		byte[] content3 = "Sample Digital Content v1.1".getBytes();
		byte[] content4 = "Another Sample Digital Content".getBytes();

		try {
			String hash1 = service.generate(new ByteArrayInputStream(content1));
			String hash2 = service.generate(new ByteArrayInputStream(content2));
			String hash3 = service.generate(new ByteArrayInputStream(content3));
			String hash4 = service.generate(new ByteArrayInputStream(content4));
			String hash5 = service.generate(new ByteArrayInputStream(content1));
			String hash6 = service.generate(new ByteArrayInputStream(content1));
			
			assertEquals(hash1,hash2);
			assertEquals(hash1,hash5);
			assertEquals(hash1,hash6);
			assertEquals(hash5,hash6);
			assertFalse(hash1.equals(hash3));
			assertFalse(hash1.equals(hash4));
			assertFalse(hash3.equals(hash4));
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			fail(e.getMessage());
		}
	}
	
	@Test(expected = DataNotFoundException.class)  
	public void testRetrieveUnexistingObject() throws BinaryStoreServiceException, DataNotFoundException {
		service.get("unexistingidentifier");
	}

	@Test
	public void testInsertUnexistingObject() {
		final byte[] content = "Sample Digital Content v1.0".getBytes();

		try {
			String identifier = service.put(new ByteArrayInputStream(content));
			InputStream is = service.get(identifier);
			assertTrue(IOUtils.contentEquals(new ByteArrayInputStream(content), is));
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testInsertExistingObject() throws BinaryStoreServiceException {
		final byte[] content1 = "Sample Digital Content v1.0".getBytes();
		final byte[] content2 = "Sample Digital Content v1.0".getBytes();
		
		try {
			String id1 = service.put(new ByteArrayInputStream(content1));
			String id2 = service.put(new ByteArrayInputStream(content2));
			assertEquals(id1, id2);
		} catch (DataCollisionException e) {
			fail(e.getMessage());
		} 
	}
	
	@Test(expected = DataCollisionException.class)  
	public void testInsertCollisionObject() throws BinaryStoreServiceException, DataCollisionException {
		service.setHashedFilterInputStreamFactory(new CollisionFilterInputStreamFactory());
		final byte[] content1 = "Sample Digital Content v1.0".getBytes();
		final byte[] content2 = "Sample Digital Content that generate a collision".getBytes();
		
		service.put(new ByteArrayInputStream(content1));
		service.put(new ByteArrayInputStream(content2));
		 
	}
	
	@Test(expected = DataNotFoundException.class)
	public void testCheckUnexistingObject() throws DataCorruptedException, BinaryStoreServiceException, DataNotFoundException {
		service.check("unexistingidentifier");
	}
	
	@Test
	public void testCheckExistingObject() {
		final byte[] content = "Sample Digital Content v1.0".getBytes();
		
		try {
			String identifier1 = service.put(new ByteArrayInputStream(content));
			service.check(identifier1);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test(expected = DataCorruptedException.class)
	public void testCheckExistingCorruptedObject() throws DataNotFoundException, BinaryStoreServiceException, DataCollisionException, DataCorruptedException {
		service.setHashedFilterInputStreamFactory(new RandomFilterInputStreamFactory());
		final byte[] content = "Sample Digital Content v1.0".getBytes();
		String identifier1 = service.put(new ByteArrayInputStream(content));
		service.check(identifier1);
	}
	
	@Test
	public void testExtractDataContent() throws BinaryStoreServiceException, DataCollisionException, DataNotFoundException {
		final byte[] content = "Sample Digital Content v1.0".getBytes();
		String identifier1 = service.put(new ByteArrayInputStream(content));
		String extraction = service.extract(identifier1);
		System.out.println("extracted data: " + extraction);
	}
}
