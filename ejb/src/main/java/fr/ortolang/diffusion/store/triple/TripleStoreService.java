package fr.ortolang.diffusion.store.triple;

import java.io.OutputStream;

import org.openrdf.query.QueryLanguage;

import fr.ortolang.diffusion.OrtolangIndexableObject;

public interface TripleStoreService {
	
	public static final String SERVICE_NAME = "triple-store";

	public static final String SERQL_QUERY_LANGUAGE = QueryLanguage.SERQL.getName();
    public static final String SPARQL_QUERY_LANGUAGE = QueryLanguage.SPARQL.getName();
    
    public static final String BASE_CONTEXT_URI = "http://www.ortolang.fr/rdf-context#";

	public void importOntology(String ontologyURI, String resourceName) throws TripleStoreServiceException;

	public void index(OrtolangIndexableObject object) throws TripleStoreServiceException;
	
	public void reindex(OrtolangIndexableObject object) throws TripleStoreServiceException;
	
	public void remove(String key) throws TripleStoreServiceException;
	
	public String query(String language, String query, String languageResult) throws TripleStoreServiceException;
	
	public void query(String language, String query, OutputStream os, String languageResult) throws TripleStoreServiceException;

}
