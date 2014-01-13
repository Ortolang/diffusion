package fr.ortolang.diffusion.performance;

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
import org.getopt.util.hash.FNV164;
import org.getopt.util.hash.FNV1a32;
import org.getopt.util.hash.FNV1a64;
import org.junit.Test;

public class HashAlgorithmPerformanceTest {
	
	private static final String SAMPLE_FOLDER = "/home/jerome/Vidéos";
	//private static final String SAMPLE_FOLDER = "/home/jerome/Images/Photos/Travaux Maison";
	
	private Logger logger = Logger.getLogger(HashAlgorithmPerformanceTest.class.getName());
	
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
				String hash = HashAlgorithmPerformanceTest.sha1(path);
				Long stop = System.currentTimeMillis();
				Float throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				sha1 += throughout;
				result.append(throughout + " Mo/s [sha1   :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceTest.sha256(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				sha256 += throughout;
				result.append(throughout + " Mo/s [sha256 :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceTest.sha512(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				sha512 += throughout;
				result.append(throughout + " Mo/s [sha512 :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceTest.crc32(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				crc32 += throughout;
				result.append(throughout + " Mo/s [crc32  :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceTest.fnv132(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				fnv132 += throughout;
				result.append(throughout + " Mo/s [fnv132 :" + hash + "]\r\n");
				start = System.currentTimeMillis();
				hash = HashAlgorithmPerformanceTest.fnva32(path);
				stop = System.currentTimeMillis();
				throughout = new Float( Files.size(path) / Math.max(1,(stop-start)) * 1000 / (1024*1024) );
				fnva32 += throughout;
				result.append(throughout + " Mo/s [fnv1a32:" + hash + "]\r\n");
				cpt ++;
				logger.log(Level.INFO, result.toString());
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
			logger.log(Level.INFO, results.toString());
		} catch ( Exception e ) {
			logger.log(Level.INFO, "Error during generating hash", e);
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
	
	private static String fnv164(Path path) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = Files.newInputStream(path) ) {
			FNV164 fnv = new FNV164();
			byte[] buffer = new byte[10240];
			int nbread = 0;
			while ((nbread = input.read(buffer)) > 0) {
				fnv.update(buffer, 0, nbread);
			}
			return Long.toString(fnv.getHash());
		}
	}
	
	private static String fnva64(Path path) throws IOException, NoSuchAlgorithmException {
		try ( InputStream input = Files.newInputStream(path) ) {
			FNV1a64 fnv = new FNV1a64();
			byte[] buffer = new byte[10240];
			int nbread = 0;
			while ((nbread = input.read(buffer)) > 0) {
				fnv.update(buffer, 0, nbread);
			}
			return Long.toString(fnv.getHash());
		}
	}
	
}
