package fr.ortolang.diffusion.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jgroups.util.UUID;
import org.junit.Test;

import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;


public class CollectionUnitTest {
	
	private static Logger logger = Logger.getLogger(CollectionUnitTest.class.getName());
	
	@Test 
	public void testRegexp() {
		String work = "1,2,3,4,5,6";
		
		work = removeElement(work, "2");
		logger.log(Level.INFO,work);
		assertEquals("1,3,4,5,6", work);
		
		work = removeElement(work, "1");
		logger.log(Level.INFO,work);
		assertEquals("3,4,5,6", work);
		
		work = removeElement(work, "6");
		logger.log(Level.INFO,work);
		assertEquals("3,4,5", work);
		
		work = removeElement(work, "5");
		work = removeElement(work, "3");
		work = removeElement(work, "4");
		logger.log(Level.INFO,work);
		assertEquals("", work);
	}
	
	private String removeElement(String collection, String element) {
		collection = collection.replaceAll("(" + element + "){1},?", "");
		if ( collection.endsWith(",") ) {
			collection = collection.substring(0, collection.length()-1);
		}
		return collection;
	}
	
	@Test 
	public void testRegexp2() {
		String work = "1\n2\n3\n4\n5\n6";
		
		work = removeElement2(work, "2");
		logger.log(Level.INFO,"\n" + work);
		assertEquals("1\n3\n4\n5\n6", work);
		
		work = removeElement2(work, "1");
		logger.log(Level.INFO,"\n" + work);
		assertEquals("3\n4\n5\n6", work);
		
		work = removeElement2(work, "6");
		logger.log(Level.INFO,"\n" + work);
		assertEquals("3\n4\n5", work);
		
		work = removeElement2(work, "5");
		work = removeElement2(work, "3");
		work = removeElement2(work, "4");
		logger.log(Level.INFO,"\n" + work);
		assertEquals("", work);
	}
	
	private String removeElement2(String collection, String element) {
		collection = collection.replaceAll("(?m)^(" + element + ")\n?", "");
		if ( collection.endsWith("\n") ) {
			collection = collection.substring(0, collection.length()-1);
		}
		return collection;
	}
	
	@Test
	public void testKeySizeImpact() {
		StringBuffer serial1 = new StringBuffer();
		StringBuffer serial2 = new StringBuffer();
		int cpt = 0;
		for ( int j=0; j<100; j++ ) {
			for ( int i=0; i<10; i++) {
				cpt++;
				serial1.append(Integer.toHexString(cpt)).append(",");
				serial2.append(UUID.randomUUID().toString()).append(",");
			}
			logger.log(Level.INFO, cpt + "," + serial1.length() + "," + serial2.length());
		}
	}
	
//	@Test
//	public void benchCollectionElement() {
//		Collection c = new Collection();
//		c.setId("1");
//		c.setClock(1);
//		c.setKey("K1");
//		c.setRoot(false);
//		c.setName("collection");
//		c.setDescription("description");
//		
//		
//		for ( int j=1; j<=10; j++) {
//			for ( int i=1; i<=2000; i++) {
//				c.addElement(new CollectionElement(DataObject.OBJECT_TYPE, "name." + j + i, System.currentTimeMillis(), UUID.randomUUID().toString()));
//			}
//		
//			long start = System.currentTimeMillis();
//			int size = c.getElements().size();
//			long stop = System.currentTimeMillis();
//			logger.log(Level.INFO, (stop-start) + " ms to get the " + size + " collection elements !!");
//			//logger.log(Level.INFO, "for " + size + " elements, collection contains " + c.getSegments().size() + " segments");
//			
//			start = System.currentTimeMillis();
//			size = c.getElements().size();
//			stop = System.currentTimeMillis();
//			logger.log(Level.INFO, (stop-start) + " ms to get the " + size + " collection elements the second time !!");
//			
//			start = System.currentTimeMillis();
//			boolean exists = c.containsElementName("name."+j+"10");
//			stop = System.currentTimeMillis();
//			logger.log(Level.INFO, (stop-start) + " ms to check that name."+j+"10 exists : " + exists + ",  in the collection collection elements !!");
//			
//			start = System.currentTimeMillis();
//			CollectionElement element = c.findElementByName("name."+j+"10");
//			stop = System.currentTimeMillis();
//			logger.log(Level.INFO, (stop-start) + " ms to find element with name."+j+"10 element : " + element);
//		}
//		
////		for ( String segment : c.getSegments() ) {
////			logger.log(Level.INFO, segment);
////		}
//		
//	}
	
	
	@Test
	public void testCollectionElement() {
		Collection c = new Collection();
		c.setId("1");
		c.setClock(1);
		c.setKey("K1");
		c.setRoot(false);
		c.setName("collection");
		c.setDescription("description");
		long tsk1 = System.currentTimeMillis();
		long tsk2 = System.currentTimeMillis();
		long tsk3 = System.currentTimeMillis();
		long tsk4 = System.currentTimeMillis();
		
		
		String key1 = UUID.randomUUID().toString();
		c.addElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname1", tsk1, "ortolang/collection", key1));
		
		assertTrue(c.containsElementName("myname1"));
		assertTrue(c.containsElementKey(key1));
		
//		for ( String segment : c.getSegments() ) {
//			logger.log(Level.INFO, segment);
//		}
		
		String key2 = UUID.randomUUID().toString();
		c.addElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname2", tsk2, "ortolang/collection", key2));
		String key3 = UUID.randomUUID().toString();
		c.addElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname3", tsk3, "ortolang/collection", key3));
		String key4 = UUID.randomUUID().toString();
		c.addElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname4", tsk4, "ortolang/collection", key4));
		
		assertTrue(c.containsElementName("myname1"));
		assertTrue(c.containsElementKey(key1));
		assertTrue(c.containsElementName("myname2"));
		assertTrue(c.containsElementKey(key2));
		assertTrue(c.containsElementName("myname3"));
		assertTrue(c.containsElementKey(key3));
		assertTrue(c.containsElementName("myname4"));
		assertTrue(c.containsElementKey(key4));
		
//		logger.log(Level.INFO, "Segment Size : " + c.getSegments().size());
//		for ( String segment : c.getSegments() ) {
//			logger.log(Level.INFO, segment);
//		}
		
		c.removeElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname2", tsk2, "ortolang/collection", key2));
		assertTrue(c.containsElementName("myname1"));
		assertTrue(c.containsElementKey(key1));
		assertFalse(c.containsElementName("myname2"));
		assertFalse(c.containsElementKey(key2));
		assertTrue(c.containsElementName("myname3"));
		assertTrue(c.containsElementKey(key3));
		assertTrue(c.containsElementName("myname4"));
		assertTrue(c.containsElementKey(key4));
		
//		logger.log(Level.INFO, "Segment Size : " + c.getSegments().size());
//		for ( String segment : c.getSegments() ) {
//			logger.log(Level.INFO, segment);
//		}
		
		c.removeElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname4", tsk4, "ortolang/collection", key4));
		assertTrue(c.containsElementName("myname1"));
		assertTrue(c.containsElementKey(key1));
		assertFalse(c.containsElementName("myname2"));
		assertFalse(c.containsElementKey(key2));
		assertTrue(c.containsElementName("myname3"));
		assertTrue(c.containsElementKey(key3));
		assertFalse(c.containsElementName("myname4"));
		assertFalse(c.containsElementKey(key4));
		
//		logger.log(Level.INFO, "Segment Size : " + c.getSegments().size());
//		for ( String segment : c.getSegments() ) {
//			logger.log(Level.INFO, segment);
//		}
		
		c.removeElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname1", tsk1, "ortolang/collection", key1));
		assertFalse(c.containsElementName("myname1"));
		assertFalse(c.containsElementKey(key1));
		assertFalse(c.containsElementName("myname2"));
		assertFalse(c.containsElementKey(key2));
		assertTrue(c.containsElementName("myname3"));
		assertTrue(c.containsElementKey(key3));
		assertFalse(c.containsElementName("myname4"));
		assertFalse(c.containsElementKey(key4));
		
//		logger.log(Level.INFO, "Segment Size : " + c.getSegments().size());
//		for ( String segment : c.getSegments() ) {
//			logger.log(Level.INFO, segment);
//		}
		
		c.removeElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname3", tsk3, "ortolang/collection", key3));
		assertFalse(c.containsElementName("myname1"));
		assertFalse(c.containsElementKey(key1));
		assertFalse(c.containsElementName("myname2"));
		assertFalse(c.containsElementKey(key2));
		assertFalse(c.containsElementName("myname3"));
		assertFalse(c.containsElementKey(key3));
		assertFalse(c.containsElementName("myname4"));
		assertFalse(c.containsElementKey(key4));
		
//		logger.log(Level.INFO, "Segment Size : " + c.getSegments().size());
//		for ( String segment : c.getSegments() ) {
//			logger.log(Level.INFO, segment);
//		}
	}
	
	@Test
	public void testFindCollectionElement() {
		Collection c = new Collection();
		c.setId("1");
		c.setClock(1);
		c.setKey("K1");
		c.setRoot(false);
		c.setName("collection");
		c.setDescription("description");
		
		String key1 = UUID.randomUUID().toString();
		c.addElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname1", System.currentTimeMillis(), "ortolang/collection", key1));
		String key2 = UUID.randomUUID().toString();
		c.addElement(new CollectionElement(Collection.OBJECT_TYPE, "myname2", System.currentTimeMillis(), "image/svg+xml", key2));
		String key3 = UUID.randomUUID().toString();
		c.addElement(new CollectionElement(Link.OBJECT_TYPE, "myname3", System.currentTimeMillis(), "application/vnd.ms-excel", key3));
		String key4 = UUID.randomUUID().toString();
		c.addElement(new CollectionElement(DataObject.OBJECT_TYPE, "myname4", System.currentTimeMillis(), "application/EDI-X12", key4));
		
		String regexp = "(?s).*((" + Collection.OBJECT_TYPE + "|" + DataObject.OBJECT_TYPE + "|" + Link.OBJECT_TYPE + ")\tmyname1\t([0-9]{13})\t([^%]+)\t([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})).*$";
		
		for ( String segment : c.getSegments() ) {
			logger.log(Level.INFO, segment);
			logger.log(Level.INFO, "segment matches myname1: " + segment.matches(regexp));
			Pattern pattern = Pattern.compile(regexp);
			Matcher matcher = pattern.matcher(segment);
			matcher.matches();
			logger.log(Level.INFO, "group 1 : " + matcher.group(1));
			logger.log(Level.INFO, "group 2 : " + matcher.group(2));
			logger.log(Level.INFO, "group 3 : " + matcher.group(3));
			logger.log(Level.INFO, "group 4 : " + matcher.group(4));
		}
		
		for ( CollectionElement element : c.getElements() ) {
			logger.log(Level.INFO, "element: " + element.serialize());
		}
		
		CollectionElement ce1 = c.findElementByName("myname1");
		logger.log(Level.INFO, "ce1 : " + ce1);
		CollectionElement ce2 = c.findElementByName("myname2");
		logger.log(Level.INFO, "ce2 : " + ce2);
		CollectionElement ce3 = c.findElementByName("myname3");
		logger.log(Level.INFO, "ce3 : " + ce3);
		CollectionElement ce4 = c.findElementByName("myname4");
		logger.log(Level.INFO, "ce4 : " + ce4);
		
		
	}
	
}


