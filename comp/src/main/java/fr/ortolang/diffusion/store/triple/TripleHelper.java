package fr.ortolang.diffusion.store.triple;

import java.io.IOException;
import java.io.Reader;
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
	 * @param reader A Reader from which RDF data can be read.
	 * @param contentType A MIME type, e.g. "application/rdf+xml".
	 * @return the list of triples extracted from RDF
	 */
	public static Set<Triple> extractTriples(Reader reader, String contentType) throws TripleStoreServiceException {
		RDFFormat format = Rio.getParserFormatForMIMEType(contentType);
		
		try {
			Model results = Rio.parse(reader, "", format);

			Set<Triple> triples = new HashSet<Triple>();
			for (Statement statement: results) {
				triples.add(new Triple(statement.getSubject().stringValue(), statement.getPredicate().stringValue(), statement.getObject().stringValue()));
			}
			return triples;
		} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
			throw new TripleStoreServiceException("Unable to extract triples", e);
		}
	}
}
