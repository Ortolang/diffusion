package fr.ortolang.diffusion.store.triple;

import java.io.OutputStream;
import java.util.List;

import org.openrdf.query.QueryLanguage;

public interface TripleStoreService {
	
	public static final String SERVICE_NAME = "TripleStore";

	public static final String SERQL_QUERY_LANGUAGE = QueryLanguage.SERQL.getName();
    public static final String SPARQL_QUERY_LANGUAGE = QueryLanguage.SPARQL.getName();

	public void importOntology(String ontologyURI, String resourceName) throws TripleStoreServiceException;

	public void insertTriple(String subject, String predicate, String object) throws TripleStoreServiceException;
	
	public void removeTriple(String subject, String predicate, String object) throws TripleStoreServiceException;
	
	public void removeTriples(String subject, String predicate, String object) throws TripleStoreServiceException;
	
	public List<Triple> listTriples(String subject, String predicate, String object) throws TripleStoreServiceException;
	
	public String query(String language, String query) throws TripleStoreServiceException;
	
	public void query(String language, String query, OutputStream os) throws TripleStoreServiceException;

}
