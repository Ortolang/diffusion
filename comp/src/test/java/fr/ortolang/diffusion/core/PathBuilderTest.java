package fr.ortolang.diffusion.core;

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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class PathBuilderTest {
	
	@Test
	public void testBuild() {
		String expected = "/";
		String path = PathBuilder.newInstance().build();
		
		assertEquals(expected, path);
	}
	
	@Test
	public void testBuildFromPath() throws InvalidPathException {
		String expected = "/a/b/c";
		String path1 = PathBuilder.fromPath("/a/b/c").build();
		String path2 = PathBuilder.fromPath("a/b/c").build();
		String path3 = PathBuilder.fromPath("a/../a/b/c").build();
		String path4 = PathBuilder.fromPath("//a/../a/b/c").build();
		
		assertEquals(expected, path1);
		assertEquals(expected, path2);
		assertEquals(expected, path3);
		assertEquals(expected, path4);
	}
	
	@Test
	public void testBuildPartsFromPath() throws InvalidPathException {
		String[] expected = new String[] {"a", "b", "c"};
		String[] parts1 = PathBuilder.fromPath("/a/b/c").buildParts();
		String[] parts2 = PathBuilder.fromPath("a/b/c").buildParts();
		String[] parts3 = PathBuilder.fromPath("a/../a/b/c").buildParts();
		String[] parts4 = PathBuilder.fromPath("//a/../a/b/c").buildParts();
		
		assertArrayEquals(expected, parts1);
		assertArrayEquals(expected, parts2);
		assertArrayEquals(expected, parts3);
		assertArrayEquals(expected, parts4);
		
		expected = new String[] {};
		
		String[] parts5 = PathBuilder.fromPath("/").buildParts();
		String[] parts6 = PathBuilder.newInstance().buildParts();
		String[] parts7 = PathBuilder.fromPath("").buildParts();
		
		assertArrayEquals(expected, parts5);
		assertArrayEquals(expected, parts6);
		assertArrayEquals(expected, parts7);
	}
	
	@Test
	public void testBuildFromEmptyPath() throws InvalidPathException {
		String expected = "/";
		String path1 = PathBuilder.fromPath("").build();
		String path2 = PathBuilder.fromPath("/").build();
		String path3 = PathBuilder.fromPath("/./").build();
		String path4 = PathBuilder.fromPath("/../").build();
		
		assertEquals(expected, path1);
		assertEquals(expected, path2);
		assertEquals(expected, path3);
		assertEquals(expected, path4);
	}
	
	@Test
	public void testBuildFromInvalidPath() {
		String[] invalidpaths = new String[] { 
				"/toto|2", 
				"/tés~té", 
				"/he\"llo\"/delu", 
				"/<yop>/enslip", 
				"/that*is/bullshit", 
				"/10%", 
				"/what?",
				"/abc	def",
				"/aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffffgggggggggghhhhhhhhhhiiiiiiiiiijjjjjjjjjjkkkkkkkkkkllllllllllmmmmmmmmmmnnnnnnnnnnooooooooooppppppppppqqqqqqqqqqrrrrrrrrrrssssssssssttttttttttuuuuuuuuuuvvvvvvvvvvwwwwwwwwwwxxxxxxxxxxyyyyyyyyyyyyzzzzzzzzzz"};
		
		for ( String path : invalidpaths ) {
			try {
				String built = PathBuilder.fromPath(path).build();
				fail("Build from path: " + path + " should have raised an InvalidPathException but have produced: " + built);
			} catch ( InvalidPathException e ) {
				//
			}
		}
	}
	
	@Test
	public void testBuildFromValidPath() {
		String[] validpaths = new String[] { 
				"/(aka12)",
				"/toto/2 3 4/bidule", 
				"/é²ùµ$£ø!§;.,n",
				"/+={zapet}_`",
				"/[a]{b}#c@d°æ€â",
				"/$êþÿûîœôöŀïüð¬’↓‘ ëäß«»©ë",
				"/ˇ¹²³",
				"/öïüëä",
				"/abcdefghijklmnopqrstuvwxyz",
				"/ABCDEFGHIUJKLMNOPQRSTUVWXYZ",
				"/0123456789",
				"/↙↓↘←↔→↖↑↗÷×−-+",
				"/¡×¿ø",
				"/aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffffgggggggggghhhhhhhhhhiiiiiiiiiijjjjjjjjjjkkkkkkkkkkllllllllllmmmmmmmmmmnnnnnnnnnnooooooooooppppppppppqqqqqqqqqqrrrrrrrrrrssssssssssttttttttttuuuuuuuuuuvvvvvvvvvvwwwwwwwwwwxxxxxxxxxxyyyyyyyyyyyyzzz", 
		};
		
		for ( String path : validpaths ) {
			try {
				PathBuilder.fromPath(path).build();
			} catch ( InvalidPathException e ) {
				fail("Build from path: " + path + " should not have raised any exception");
			}
		}
	}
	
	@Test
	public void testIsRoot() throws InvalidPathException {
		assertTrue(PathBuilder.newInstance().isRoot());
		assertTrue(PathBuilder.fromPath("").isRoot());
		assertTrue(PathBuilder.fromPath("/").isRoot());
		assertTrue(PathBuilder.fromPath("/./").isRoot());
		assertTrue(PathBuilder.fromPath("/../").isRoot());
		assertTrue(PathBuilder.fromPath("").path("/").isRoot());
		assertTrue(PathBuilder.fromPath("").path("/").path("/").isRoot());
		assertTrue(PathBuilder.fromPath("").path("/").path("/").path("/").isRoot());
		assertTrue(PathBuilder.newInstance().path("/").path("/").path("/").isRoot());
	}
	
	@Test
	public void testPathDepth() throws InvalidPathException {
		int path0 = PathBuilder.newInstance().depth();
		int path1 = PathBuilder.fromPath("//1").depth();
		int path2 = PathBuilder.fromPath("/1/2").depth();
		int path3 = PathBuilder.fromPath("/a/../1/2/3").depth();
		int path4 = PathBuilder.newInstance().path("a").path("b").path("c").path("d").depth();
		int path5 = PathBuilder.newInstance().path("1").path("2").path("3/4/5").depth();
		int path20 = PathBuilder.fromPath("/1/2/3/4/5/6/7/8/9/10/11/12/13/14/15/16/17/18/19/20").depth();
		int path00 = PathBuilder.newInstance().path("/").path("/").path("/").depth();
		
		assertEquals(0, path0);
		assertEquals(PathBuilder.fromPath("//1").build(), 1, path1);
		assertEquals(2, path2);
		assertEquals(3, path3);
		assertEquals(4, path4);
		assertEquals(5, path5);
		assertEquals(20, path20);
		assertEquals(00, path00);
	}
	
	@Test
	public void testPathPart() throws InvalidPathException {
		String part0 = PathBuilder.newInstance().part();
		String part1 = PathBuilder.fromPath("").part();
		String part2 = PathBuilder.fromPath("/a/b/c").part();
		String part3 = PathBuilder.newInstance().path("a").path("b").part();
		String part4 = PathBuilder.fromPath("/1/2/3/4/5/tagada").part();
		
		assertEquals("", part0);
		assertEquals("", part1);
		assertEquals("c", part2);
		assertEquals("b", part3);
		assertEquals("tagada", part4);
	}
	
	@Test
	public void testPathParent() throws InvalidPathException {
		String parent0 = PathBuilder.newInstance().parent().build();
		String parent1 = PathBuilder.fromPath("").parent().build();
		String parent2 = PathBuilder.fromPath("/a/b/c").parent().build();
		String parent3 = PathBuilder.newInstance().path("a").path("b").parent().build();
		String parent4 = PathBuilder.fromPath("/1/2/3/4/5/tagada").parent().parent().parent().build();
		String parent5 = PathBuilder.newInstance().parent().parent().parent().build();
		
		assertEquals("/", parent0);
		assertEquals("/", parent1);
		assertEquals("/a/b", parent2);
		assertEquals("/a", parent3);
		assertEquals("/1/2/3", parent4);
		assertEquals("/", parent5);
	}
	
	@Test
	public void testPathIsChild() throws InvalidPathException {
		assertFalse(PathBuilder.newInstance().isChild("/a"));
		assertFalse(PathBuilder.fromPath("/a/b/c").isChild("/b"));
		assertTrue(PathBuilder.fromPath("/a/b/c").isChild("/a/b/c/d/../../"));
		assertFalse(PathBuilder.fromPath("/a/b/caddy").isChild("/a/b/caddy"));
		assertFalse(PathBuilder.fromPath("/a/b/caddy").isChild("/a/b/c"));
		assertFalse(PathBuilder.fromPath("/a/b/c/d/e/f").isChild("/a/b/caddy/d/e/f"));
		assertTrue(PathBuilder.fromPath("/a/b/c/d/e/f").isChild("/"));
		assertTrue(PathBuilder.fromPath("/a/b/c/d/e/f").isChild(""));
		assertTrue(PathBuilder.fromPath("/a/b/c/d/e/f").isChild("/a/b/c/"));
		assertTrue(PathBuilder.fromPath("/a/b/c/d/e/f").isChild("/a"));
		assertTrue(PathBuilder.fromPath("/a/b/c/d/e/f").isChild("/a/b/c/d/e/"));
	}
	
	@Test
	public void testPathIsParent() throws InvalidPathException {
		assertTrue(PathBuilder.newInstance().isParent("/a"));
		assertFalse(PathBuilder.fromPath("/a").isParent("/b/c"));
		assertTrue(PathBuilder.fromPath("/a/b/c/d/../../").isParent("/a/b/c"));
		assertFalse(PathBuilder.fromPath("/a/b").isParent("/a/b/c/d/../../"));
		assertFalse(PathBuilder.fromPath("/a/b/caddy").isParent("/a/b/c"));
		assertTrue(PathBuilder.fromPath("/a/b/c").isParent("/a/b/c/d"));
		assertTrue(PathBuilder.fromPath("/a").isParent("/a/b/caddy/d/e/f"));
		assertTrue(PathBuilder.fromPath("/a/b/c").isParent("/a/b/caddy/../c/e/flagada/"));
	}
	
	@Test
	public void testClone() throws InvalidPathException {
		PathBuilder builder = PathBuilder.fromPath("/a");
		PathBuilder clone = builder.clone();
		
		assertEquals("/a", builder.build());
		assertEquals("/a", clone.build());
		
		clone.path("b");
		
		assertEquals("/a", builder.build());
		assertEquals("/a/b", clone.build());
	}
	
	@Test 
	public void testEquals() throws InvalidPathException {
		PathBuilder one = PathBuilder.fromPath("/tagada");
		PathBuilder two = PathBuilder.newInstance();
		two.path("/tagada");
		
		assertTrue(one.equals(two));
		
		PathBuilder three = PathBuilder.newInstance();
		three.path("tagada");
		
		assertTrue(one.equals(three));
		assertTrue(three.equals(one));
		assertTrue(three.equals(two));
		assertTrue(two.equals(three));
		
		three.path("toto");
		two.path("toto");
		
		assertFalse(one.equals(three));
		assertFalse(one.equals(two));
		assertTrue(two.equals(three));
		
		three.parent();
		
		assertTrue(one.equals(three));
		assertFalse(one.equals(two));
		assertFalse(two.equals(three));
		
		PathBuilder four = one;
		
		assertTrue(one.equals(four));
		
		PathBuilder five = null;
		
		assertFalse(one.equals(five));
	}
	
	@Test 
	public void testRelativize() throws InvalidPathException {
		String path = "/a/b/ccc/d/e";
		
		assertEquals("/ccc/d/e", PathBuilder.fromPath(path).relativize("/a/b").build());
		assertEquals("/e", PathBuilder.fromPath(path).relativize("/a/b/ccc/d").build());
		assertEquals("/b/ccc/d/e", PathBuilder.fromPath(path).relativize("a").build());
		
		try {
			PathBuilder.fromPath(path).relativize("/a/b/cc");
			fail("Should fail...");
		} catch ( InvalidPathException e ) {
			//
		}
		
		try {
			PathBuilder.fromPath(path).relativize("/d");
			fail("Should fail...");
		} catch ( InvalidPathException e ) {
			//
		}
	
	}
	
	@Test 
	public void testRelativize2() throws InvalidPathException {
		String path = "/a/b/ccc/d/e";
		
		assertEquals("/ccc/d/e", PathBuilder.fromPath(path).relativize(2).build());
		assertEquals("/e", PathBuilder.fromPath(path).relativize(4).build());
		assertEquals("/b/ccc/d/e", PathBuilder.fromPath(path).relativize(1).build());
		assertEquals("/", PathBuilder.fromPath(path).relativize(5).build());
		
		try {
			PathBuilder.fromPath(path).relativize(-1);
			fail("Should fail...");
		} catch ( InvalidPathException e ) {
			//
		}
		
		try {
			PathBuilder.fromPath(path).relativize(6);
			fail("Should fail...");
		} catch ( InvalidPathException e ) {
			//
		}
	
	}

}
