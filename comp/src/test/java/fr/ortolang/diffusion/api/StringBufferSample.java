package fr.ortolang.diffusion.api;

import org.junit.Test;

public class StringBufferSample {
	
	@Test
	public void testSB() {
		StringBuffer sb = new StringBuffer();
		sb.insert(0, "/2");
		sb.insert(0, "/1");
		sb.insert(0, "/0");
		System.out.println(sb.toString());
		
	}

}
