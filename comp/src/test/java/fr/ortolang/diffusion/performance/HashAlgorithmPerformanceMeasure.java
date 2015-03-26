package fr.ortolang.diffusion.performance;

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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

import org.getopt.util.hash.FNV132;
import org.getopt.util.hash.FNV1a32;
import org.junit.Test;

public class HashAlgorithmPerformanceMeasure {
	
	private static final String SAMPLE_FOLDER = "/home/jerome/Vidéos";
	//private static final String SAMPLE_FOLDER = "/home/jerome/Images/Photos/Travaux Maison";
	
	private static final Logger LOGGER = Logger.getLogger(HashAlgorithmPerformanceMeasure.class.getName());
	
	@Test
	public void testGenerateHash1() {
		try {
			DirectoryStream<Path> directory = Files.newDirectoryStream(Paths.get(SAMPLE_FOLDER));
			Float sha1 = new Float(0);
			Float sha256 = new Float(0);
			Float sha512 = new Float(0);
			Float crc32 = new Float(0);
			Float fnv132 = new Float(0);
			Float fnva32 = new Float(0);
			int cpt = 0;
			for ( Path path : directory ) {
				StringBuffer result = new StringBuffer();
				result.append("Treating file " + path + "\r\n");
				Long start = System.currentTimeMillis();
				String hash = HashAlgorithmPerformanceMeasure.sha1(path);
				Long stop = System.currentTimeMillis();
				Float throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				sha1 += throughout;
				result.append(throughout + " Mo/s [sha1   :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceMeasure.sha256(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				sha256 += throughout;
				result.append(throughout + " Mo/s [sha256 :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceMeasure.sha512(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				sha512 += throughout;
				result.append(throughout + " Mo/s [sha512 :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceMeasure.crc32(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				crc32 += throughout;
				result.append(throughout + " Mo/s [crc32  :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceMeasure.fnv132(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				fnv132 += throughout;
				result.append(throughout + " Mo/s [fnv132 :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceMeasure.fnva32(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				fnva32 += throughout;
				result.append(throughout + " Mo/s [fnv1a32:" + hash + "]\r\n");
				cpt ++;
				LOGGER.log(Level.INFO, result.toString());
			}
				
			sha1 = sha1 / cpt;
			sha256 = sha256 / cpt;
			sha512 = sha512 / cpt;
			crc32 = crc32 / cpt;
			fnv132 = fnv132 / cpt;
			fnva32 = fnva32 / cpt;
			StringBuffer results = new StringBuffer();
			results.append("Results: \r\n");
			results.append("Average throughput for SHA-1   : " + sha1 + " Mb/s \r\n");
			results.append("Average throughput for SHA-256 : " + sha256 + " Mb/s \r\n");
			results.append("Average throughput for SHA-512 : " + sha512 + " Mb/s \r\n");
			results.append("Average throughput for CRC32   : " + crc32 + " Mb/s \r\n");
			results.append("Average throughput for FNV132  : " + fnv132 + " Mb/s \r\n");
			results.append("Average throughput for FNV1a32 : " + fnva32 + " Mb/s \r\n");
			LOGGER.log(Level.INFO, results.toString());
		} catch ( Exception e ) {
			LOGGER.log(Level.INFO, "Error during generating hash", e);
			fail(e.getMessage());
		}
	}
	
	private static String sha1(Path path) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = Files.newInputStream(path) ) {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] buffer = new byte[10240];
			int nbread = 0;
			while ((nbread = input.read(buffer)) > 0) {
				md.update(buffer, 0, nbread);
			}
			BigInteger bi = new BigInteger(1, md.digest());
			return bi.toString(16);
		}
	}
	
	private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = Files.newInputStream(path) ) {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[10240];
			int nbread = 0;
			while ((nbread = input.read(buffer)) > 0) {
				md.update(buffer, 0, nbread);
			}
			BigInteger bi = new BigInteger(1, md.digest());
			return bi.toString(16);
		}
	}
	
	private static String sha512(Path path) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = Files.newInputStream(path) ) {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			byte[] buffer = new byte[10240];
			int nbread = 0;
			while ((nbread = input.read(buffer)) > 0) {
				md.update(buffer, 0, nbread);
			}
			BigInteger bi = new BigInteger(1, md.digest());
			return bi.toString(16);
		}
	}
	
	private static String crc32(Path path) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = Files.newInputStream(path) ) {
			CRC32 crc = new CRC32();
			byte[] buffer = new byte[10240];
			int nbread = 0;
			while ((nbread = input.read(buffer)) > 0) {
				crc.update(buffer, 0, nbread);
			}
			return Long.toString(crc.getValue());
		}
	}
	
	private static String fnv132(Path path) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = Files.newInputStream(path) ) {
			FNV132 fnv = new FNV132();
			byte[] buffer = new byte[10240];
			int nbread = 0;
			while ((nbread = input.read(buffer)) > 0) {
				fnv.update(buffer, 0, nbread);
			}
			return Long.toString(fnv.getHash());
		}
	}
	
	private static String fnva32(Path path) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = Files.newInputStream(path) ) {
			FNV1a32 fnv = new FNV1a32();
			byte[] buffer = new byte[10240];
			int nbread = 0;
			while ((nbread = input.read(buffer)) > 0) {
				fnv.update(buffer, 0, nbread);
			}
			return Long.toString(fnv.getHash());
		}
	}
	
}
