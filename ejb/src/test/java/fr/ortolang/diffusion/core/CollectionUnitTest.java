package fr.ortolang.diffusion.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jgroups.util.UUID;
import org.junit.Test;

import fr.ortolang.diffusion.core.entity.Collection;


public class CollectionUnitTest {
	
	@Test 
	public void testRegexp() {
		String work = "1,2,3,4,5,6";
		
		work = removeElement(work, "2");
		System.out.println(work);
		assertEquals("1,3,4,5,6", work);
		
		work = removeElement(work, "1");
		System.out.println(work);
		assertEquals("3,4,5,6", work);
		
		work = removeElement(work, "6");
		System.out.println(work);
		assertEquals("3,4,5", work);
		
		work = removeElement(work, "5");
		work = removeElement(work, "3");
		work = removeElement(work, "4");
		System.out.println(work);
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
	public void testCollectionSegmentSize() {
		Collection c = new Collection();
		c.setKey("Key");
		c.setName("test");
		c.setDescription("description");
		c.setId("ID");
		
		List<String> uuids = new ArrayList<String> ();
		for ( int i=0; i<2500; i++) {
			String uuid = UUID.randomUUID().toString();
			uuids.add(uuid);
			c.addElement(uuid);
		}
		assertEquals(12, c.getSegments().size());
		
		Collections.shuffle(uuids);
		for (String uuid : uuids ) {
			c.removeElement(uuid);
		}
		assertEquals(0, c.getElements().size());
		assertEquals(0, c.getSegments().size());
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
			System.out.println(cpt + "," + serial1.length() + "," + serial2.length());
		}
	}
	

}
