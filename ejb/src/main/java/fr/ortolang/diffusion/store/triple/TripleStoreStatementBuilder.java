package fr.ortolang.diffusion.store.triple;

import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;

import fr.ortolang.diffusion.OrtolangIndexableObject;

public class TripleStoreStatementBuilder {
	
	public static Set<Statement> buildStatements(OrtolangIndexableObject object) throws TripleStoreServiceException {
		Set<Statement> statements = new HashSet<Statement> ();
		statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.ortolang.fr/2014/05/diffusion#Object"));
		statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.ortolang.fr/2014/05/diffusion#service", object.getService()));
		statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.ortolang.fr/2014/05/diffusion#type", object.getType()));
		for ( Triple triple : object.getSemanticContent().getTriples() ) {
			statements.add(buildStatement(triple.getSubject(), triple.getPredicate(), triple.getObject()));
		}
		return statements;
	}
	
	private static Statement buildStatement(String subject, String predicate, String object) {
		URI usubject = new URIImpl(subject);
        URI upredicate = new URIImpl(predicate);
        Value uobject = null;
        try {
            uobject = new URIImpl(object);
        } catch (IllegalArgumentException iae) {
            uobject = new LiteralImpl(object);
        }
        return new StatementImpl(usubject, upredicate, uobject);
	}

}
