package fr.ortolang.diffusion.runtime.task;

import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.PreBag;
import gov.loc.repository.bagit.utilities.SimpleResult;
import gov.loc.repository.bagit.writer.Writer;
import gov.loc.repository.bagit.writer.impl.ZipWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.junit.Test;

public class BagItTest {
	
//	@Test
//	public void testCreateBag() throws IOException {
//		File sourceBagDir = new File(this.getClass().getClassLoader().getResource("src_bag").getPath());
//		File destBagDir = File.createTempFile("bagit", ".zip");
//		BagFactory factory = new BagFactory();
//		PreBag preBag = factory.createPreBag(sourceBagDir);
//		Bag bag = preBag.makeBagInPlace(BagFactory.LATEST, true, true);
//		bag.makeComplete();
//		bag.verifyComplete();
//		Writer writer = new ZipWriter(factory);
//		writer.write(bag, destBagDir);
//		System.out.println(bag.getBagInfoTxt());
//		System.out.println(bag.getBagItTxt());
//		bag.close();
//	}
//	
//	@Test
//	public void testCreateBag2() throws IOException {
//		File sourceBagDir = new File(this.getClass().getClassLoader().getResource("src_bag_2").getPath());
//		File destBagDir = File.createTempFile("bagit2", ".zip");
//		BagFactory factory = new BagFactory();
//		PreBag preBag = factory.createPreBag(sourceBagDir);
//		Bag bag = preBag.makeBagInPlace(BagFactory.LATEST, true, true);
//		bag.makeComplete();
//		bag.verifyComplete();
//		Writer writer = new ZipWriter(factory);
//		writer.write(bag, destBagDir);
//		System.out.println(bag.getBagInfoTxt());
//		System.out.println(bag.getBagItTxt());
//		bag.close();
//	}
	
	@Test
	public void testCreateBag3() throws IOException {
		File sourceBagDir = new File("/media/space/jerome/Data/dataset/100");
		File destBagDir = new File("/media/space/jerome/Data/bags/100.zip");
		BagFactory factory = new BagFactory();
		PreBag preBag = factory.createPreBag(sourceBagDir);
		Bag bag = preBag.makeBagInPlace(BagFactory.LATEST, true, true);
		bag.makeComplete();
		bag.verifyComplete();
		Writer writer = new ZipWriter(factory);
		writer.write(bag, destBagDir);
		System.out.println(bag.getBagInfoTxt());
		System.out.println(bag.getBagItTxt());
		bag.close();
	}
	
//	@Test
//	public void testReadBag() throws IOException {
//		File sourceBag = new File(this.getClass().getClassLoader().getResource("bag.zip").getPath());
//		BagFactory factory = new BagFactory();
//		Bag bag = factory.createBag(sourceBag);
//		SimpleResult result = bag.verifyPayloadManifests();
//		System.out.println("verify payload result : " + result.isSuccess());
//		System.out.println("verify payload result message: " + result.messagesToString());
//		Collection<BagFile> payload = bag.getPayload();
//		for ( BagFile file : payload ) {
//			System.out.println(file.getFilepath());
//			System.out.println(file.getSize());
//			dumpFileContent(file.newInputStream());
//		}
//		bag.close();
//	}
//	
//	@Test
//	public void testReadBadBag() throws IOException {
//		File sourceBag = new File(this.getClass().getClassLoader().getResource("badbag.zip").getPath());
//		BagFactory factory = new BagFactory();
//		Bag bag = factory.createBag(sourceBag);
//		SimpleResult result = bag.verifyPayloadManifests();
//		System.out.println("verify payload result : " + result.isSuccess());
//		System.out.println("verify payload result message: " + result.messagesToString());
//		Collection<BagFile> payload = bag.getPayload();
//		for ( BagFile file : payload ) {
//			System.out.println(file.getFilepath());
//			System.out.println(file.getSize());
//			dumpFileContent(file.newInputStream());
//		}
//		bag.close();
//	}
//	
//	public void dumpFileContent(InputStream is) throws IOException {
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		byte[] buffer = new byte[1024];
//		int nbreads;
//		while ( (nbreads = is.read(buffer)) >= 0 ) {
//			baos.write(buffer, 0, nbreads);
//		}
//		System.out.println(baos.toString());
//		baos.close();
//		is.close();
//	}

}
 