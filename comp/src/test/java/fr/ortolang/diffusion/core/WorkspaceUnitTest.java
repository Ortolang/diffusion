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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jgroups.util.UUID;
import org.junit.Test;

import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.Workspace;

public class WorkspaceUnitTest {

	private static final Logger LOGGER = Logger.getLogger(WorkspaceUnitTest.class.getName());
	
	@Test
	public void testWorkspaceSnapshot() {
		Workspace w = new Workspace();
		w.setId("wid");
		w.setHead("head");
		w.setKey("wkey");
		w.setMembers("members");
		w.setChanged(true);
		w.setType("test");
		
		String sk1 = UUID.randomUUID().toString();
		String sn1 = "Version 1";
		SnapshotElement se1 = new SnapshotElement(sn1, sk1);
		w.addSnapshot(se1);
		LOGGER.log(Level.INFO, "Workspace Snapshot Content : \r\n" + w.getSnapshotsContent());
		assertEquals(1, w.getSnapshots().size());
		assertTrue(w.containsSnapshotKey(sk1));
		assertTrue(w.containsSnapshotName(sn1));
		assertTrue(w.containsSnapshot(se1));
		assertNotNull(w.findSnapshotByName(sn1));
		assertEquals(se1, w.findSnapshotByName(sn1));
		
		String sk2 = UUID.randomUUID().toString();
		String sn2 = "Version 2";
		SnapshotElement se2 = new SnapshotElement(sn2, sk2);
		w.addSnapshot(se2);
		LOGGER.log(Level.INFO, "Workspace Snapshot Content : \r\n" + w.getSnapshotsContent());
		assertEquals(2, w.getSnapshots().size());
		assertTrue(w.containsSnapshotKey(sk1));
		assertTrue(w.containsSnapshotName(sn1));
		assertTrue(w.containsSnapshot(se1));
		assertNotNull(w.findSnapshotByName(sn1));
		assertEquals(se1, w.findSnapshotByName(sn1));
		assertTrue(w.containsSnapshotKey(sk2));
		assertTrue(w.containsSnapshotName(sn2));
		assertTrue(w.containsSnapshot(se2));
		assertNotNull(w.findSnapshotByName(sn2));
		assertEquals(se2, w.findSnapshotByName(sn2));
		
		w.removeSnapshot(se1);
		LOGGER.log(Level.INFO, "Workspace Snapshot Content : \r\n" + w.getSnapshotsContent());
		assertEquals(1, w.getSnapshots().size());
		assertFalse(w.containsSnapshotKey(sk1));
		assertFalse(w.containsSnapshotName(sn1));
		assertFalse(w.containsSnapshot(se1));
		assertNull(w.findSnapshotByName(sn1));
		assertTrue(w.containsSnapshotKey(sk2));
		assertTrue(w.containsSnapshotName(sn2));
		assertTrue(w.containsSnapshot(se2));
		assertNotNull(w.findSnapshotByName(sn2));
		assertEquals(se2, w.findSnapshotByName(sn2));
		
	}
	
	@Test
	public void testFindSnapshotByNameOrKey() {
		Workspace w = new Workspace();
		w.setId("wid");
		w.setHead("head");
		w.setKey("wkey");
		w.setMembers("members");
		w.setChanged(true);
		w.setType("test");
		
		String sk1 = UUID.randomUUID().toString();
		String sn1 = "Version 1";
		SnapshotElement se1 = new SnapshotElement(sn1, sk1);
		w.addSnapshot(se1);
		LOGGER.log(Level.INFO, "Workspace Snapshot Content : \r\n" + w.getSnapshotsContent());
				
		String sk2 = UUID.randomUUID().toString();
		String sn2 = "Version 2";
		SnapshotElement se2 = new SnapshotElement(sn2, sk2);
		w.addSnapshot(se2);
		LOGGER.log(Level.INFO, "Workspace Snapshot Content : \r\n" + w.getSnapshotsContent());
		
		String sk3 = UUID.randomUUID().toString();
		String sn3 = "Version 3";
		SnapshotElement se3 = new SnapshotElement(sn3, sk3);
		w.addSnapshot(se3);
		LOGGER.log(Level.INFO, "Workspace Snapshot Content : \r\n" + w.getSnapshotsContent());
		
		SnapshotElement f1 = w.findSnapshotByName("Version 3");
		assertTrue(f1.equals(se3));
		
		SnapshotElement f2 = w.findSnapshotByKey(sk2);
		assertTrue(f2.equals(se2));
		
	}

}
