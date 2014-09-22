package fr.ortolang.diffusion.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;

public class OrtolangObjectidentifierTest {
	
	@Test
	public void testPatternMatching() {
		OrtolangObjectIdentifier oid1 = new OrtolangObjectIdentifier("tada", "didi", "123456");
		OrtolangObjectIdentifier oid2 = new OrtolangObjectIdentifier("tada2", "didi", "1234567");
		OrtolangObjectIdentifier oid3 = new OrtolangObjectIdentifier("tada", "didi2", "12345678");
		System.out.println(OrtolangObjectIdentifier.buildFilterPattern("tada", "didi"));
		System.out.println(oid1.serialize());
		assertTrue(oid1.serialize().matches(OrtolangObjectIdentifier.buildFilterPattern("tada", "didi")));
		assertFalse(oid2.serialize().matches(OrtolangObjectIdentifier.buildFilterPattern("tada", "didi")));
		assertTrue(oid2.serialize().matches(OrtolangObjectIdentifier.buildFilterPattern("", "didi")));
		assertFalse(oid3.serialize().matches(OrtolangObjectIdentifier.buildFilterPattern("tada", "didi")));
		assertTrue(oid3.serialize().matches(OrtolangObjectIdentifier.buildFilterPattern("tada", "")));
		assertTrue(oid1.serialize().matches(OrtolangObjectIdentifier.buildFilterPattern("", "")));
		assertTrue(oid2.serialize().matches(OrtolangObjectIdentifier.buildFilterPattern("", "")));
		assertTrue(oid3.serialize().matches(OrtolangObjectIdentifier.buildFilterPattern("", "")));
	}

}
