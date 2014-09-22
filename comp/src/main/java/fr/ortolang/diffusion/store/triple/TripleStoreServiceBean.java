package fr.ortolang.diffusion.store.triple;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.ejb3.annotation.SecurityDomain;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.resultio.TupleQueryResultWriter;
import org.openrdf.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.sail.nativerdf.NativeStore;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangIndexableObject;

@Local(TripleStoreService.class)
@Singleton(name = TripleStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed({"system", "user"})
@Lock(LockType.READ)
public class TripleStoreServiceBean implements TripleStoreService {
	
	public static final String DEFAULT_TRIPLE_HOME = "/triple-store";
	
    
    private Logger logger = Logger.getLogger(TripleStoreServiceBean.class.getName());
    private Path base;
    private static Repository repository;
    
    public TripleStoreServiceBean() {
    	logger.log(Level.FINE, "Instanciating service");
    	this.base = Paths.get(OrtolangConfig.getInstance().getProperty("home"), DEFAULT_TRIPLE_HOME);
    }

    @PostConstruct
    public void init() {
    	logger.log(Level.INFO, "Initializing service with base folder: " + base);
    	try {
    		Files.createDirectories(base);
    		repository = new SailRepository(new NativeStore(base.toFile()));
            repository.initialize();
            this.importOntology("http://www.w3.org/2000/01/rdf-schema#", "ontology/rdfs.xml");
            this.importOntology("http://xmlns.com/foaf/0.1/", "ontology/foaf.xml");
            this.importOntology("http://www.ortolang.fr/2014/05/diffusion#", "ontology/ortolang.xml");
            this.importOntology("http://www.ortolang.fr/2014/09/market#", "ontology/ortolang-market.xml");
	    } catch (Exception e) {
    		logger.log(Level.SEVERE, "unable to initialize triple store", e);
    	}
    }
    
    @PreDestroy
    public void shutdown() {
    	logger.log(Level.INFO, "Shuting down triple store");
    	try {
    		repository.shutDown();
        } catch (Exception e) {
    		logger.log(Level.SEVERE, "unable to shutdown triple store", e);
    	}
    }

    public Path getBase() {
		return base;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void importOntology(String ontologyURI, String resourceName) throws TripleStoreServiceException {
		logger.log(Level.FINE, "importing ontology [" + ontologyURI + "] into triple store");
        try {
            RepositoryConnection con = repository.getConnection();
            try {
                if (!con.hasStatement(new URIImpl(ontologyURI), new URIImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), new URIImpl("http://www.w3.org/2002/07/owl#Ontology"), false)) {
                    logger.log(Level.FINE, "ontology not present, importing...");
                    InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
                    con.add(is, ontologyURI, RDFFormat.RDFXML);
                } else {
                    logger.log(Level.FINE, "ontology already present, no need to import");
                }
            } finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to import ontology in store", e);
        }
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void index(OrtolangIndexableObject object) throws TripleStoreServiceException {
		try {
			RepositoryConnection con = repository.getConnection();
            try {
            	con.add(TripleStoreStatementBuilder.buildStatements(object), getContext(object.getKey()));
            } finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to add triple in store", e);
        }
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void reindex(OrtolangIndexableObject object) throws TripleStoreServiceException {
		try {
            RepositoryConnection con = repository.getConnection();
            try {
            	con.clear(getContext(object.getKey()));
            	con.add(TripleStoreStatementBuilder.buildStatements(object), getContext(object.getKey()));
            } finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to reindex triples for key: " + object.getKey(), e);
        }
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void remove(String key) throws TripleStoreServiceException {
		try {
            RepositoryConnection con = repository.getConnection();
            try {
            	con.clear(getContext(key));
            } finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to remove triples for key: " + key, e);
        }
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public String query(String language, String query, String languageResult) throws TripleStoreServiceException {
		try {
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	RepositoryConnection con = repository.getConnection();
        	try {
        		if ( language.equals(QueryLanguage.SERQL.getName()) ) {
	        		RDFXMLWriter writer = new RDFXMLWriter(baos);
		            GraphQuery gquery = con.prepareGraphQuery(QueryLanguage.SERQL, query);
	                gquery.evaluate(writer);
        		} else if ( language.equals(QueryLanguage.SPARQL.getName()) ) {
        			TupleQueryResultWriter writer = null;
        			switch(languageResult) {
        				case "json":
        					writer = new SPARQLResultsJSONWriter(baos);
        					break;
        				case "xml":
        				default:		
        					writer = new SPARQLResultsXMLWriter(baos);
        				break;
        			}
        			TupleQuery tquery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
        			tquery.evaluate(writer);
        		} else {
        			throw new TripleStoreServiceException("unsupported query language");
        		}
        	} finally {
                con.close();
            }
        	return baos.toString();
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to execute query", e);
        }
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void query(String language, String query, OutputStream os, String languageResult) throws TripleStoreServiceException {
		try {
        	RepositoryConnection con = repository.getConnection();
        	try {
        		if ( language.equals(QueryLanguage.SERQL.getName()) ) {
	        		RDFXMLWriter writer = new RDFXMLWriter(os);
		            GraphQuery gquery = con.prepareGraphQuery(QueryLanguage.SERQL, query);
	                gquery.evaluate(writer);
        		} else if ( language.equals(QueryLanguage.SPARQL.getName()) ) {
        			TupleQueryResultWriter writer = null;
        			switch(languageResult) {
        				case "json":
        					writer = new SPARQLResultsJSONWriter(os);
        					break;
        				case "xml":
        				default:		
        					writer = new SPARQLResultsXMLWriter(os);
        				break;
        			}
        			TupleQuery tquery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
        			tquery.evaluate(writer);
        		} else {
        			throw new TripleStoreServiceException("unsupported query language");
        		}
        	} finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to execute query", e);
        }
	}
	
	private Resource getContext(String key) {
		return new URIImpl(BASE_CONTEXT_URI + key);
	}

	
}
