package fr.ortolang.diffusion.core;

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

	private static Logger logger = Logger.getLogger(WorkspaceUnitTest.class.getName());
	
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
		logger.log(Level.INFO, "Workspace Snapshot Content : \r\n" + w.getSnapshotsContent());
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
		logger.log(Level.INFO, "Workspace Snapshot Content : \r\n" + w.getSnapshotsContent());
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
		logger.log(Level.INFO, "Workspace Snapshot Content : \r\n" + w.getSnapshotsContent());
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

}
