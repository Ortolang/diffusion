package fr.ortolang.diffusion.api;

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
