package fr.ortolang.diffusion.store.triple;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;

public class TripleHelper {
	
	/**
	 * Extracts triples from RDF string.
	 * @param rdf a string representation of the RDF
	 * @return the list of triples extracted from RDF
	 */
	public static Set<Triple> extractTriples(String subject, InputStream input, String contentType) throws TripleStoreServiceException {
		RDFFormat format = Rio.getParserFormatForMIMEType(contentType); //TODO use fallback ?
		
		try {
			//TODO creates our own RDFHandler
			Model results = Rio.parse(input, "", format);

			Set<Triple> triples = new HashSet<Triple>();
			for (Statement statement: results) {
				// statement.getSubject().stringValue()
				triples.add(new Triple(subject, statement.getPredicate().stringValue(), statement.getObject().stringValue()));
			}
			return triples;
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new TripleStoreServiceException("Unable to extract triples");
		}
	}
}
