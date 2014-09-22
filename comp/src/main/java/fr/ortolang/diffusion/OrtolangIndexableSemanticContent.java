package fr.ortolang.diffusion;

import java.util.HashSet;
import java.util.Set;

import fr.ortolang.diffusion.store.triple.Triple;

public class OrtolangIndexableSemanticContent {
	
	private Set<Triple> triples;

    public OrtolangIndexableSemanticContent() {
        triples = new HashSet<Triple> ();
    }

     public void addTriple(Triple triple) {
        triples.add(triple);
    }

	public Set<Triple> getTriples() {
		return triples;
	}

    
}
