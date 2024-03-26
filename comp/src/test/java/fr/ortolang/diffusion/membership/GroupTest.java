package fr.ortolang.diffusion.membership;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import fr.ortolang.diffusion.membership.entity.Group;

public class GroupTest {
    @Test
    public void testRemoveMember() {
        String member = "toto (titi)";
        Group group = new Group();
        group.addMember("fifi");
        group.addMember(member);
        group.addMember("riri");

        group.removeMember(member);
        assertEquals("fifi,riri", group.getMembersList());

        group.removeMember("riri");
        assertEquals("fifi", group.getMembersList());

        group.removeMember("fifi");
        assertEquals("", group.getMembersList());
    }
}