package fr.ortolang.diffusion.core;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.junit.Test;

import fr.ortolang.diffusion.core.entity.Node;

public class DiffUniteTest {
    
    @Test
    public void testDiff() {
        Javers javers = JaversBuilder.javers().build();
        
        Node root1 = new Node(null, "K1", "", "");
        Node c1 = new Node(root1, "K11", "child1", "");
        Node mdc1 = new Node(c1, "KMD11", "meta1", "aaaa");
        Node c2 = new Node(root1, "K12", "child2", "");
        Node c3 = new Node(root1, "K13", "child3", "");
        Node c4 = new Node(root1, "K14", "child4", "");
        Node c41 = new Node(c4, "K141", "child41", "");
        
        
        Node root2 = new Node(null, "K1", "", "");
        Node c1m = new Node(root2, "K11", "child1", "");
        Node mdc1m = new Node(c1m, "KMD11u", "meta1", "abba");
        Node c2m = new Node(root2, "K12", "child2", "");
        Node c3m = new Node(root2, "K13", "child3", "");
        Node c4m = new Node(root2, "K14", "child4", "");
        Node c41m = new Node(c4m, "K141u", "child41", "");
        Node c411 = new Node(c41m, "K1411", "file1.txt", "bbbb");
        
        Diff diff = javers.compare(root1, root2);
        
        System.out.println(diff.changesSummary());
        System.out.println(diff.prettyPrint());
        
        
    }

}
