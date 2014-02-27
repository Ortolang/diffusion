package fr.ortolang.diffusion.store.triple;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.ejb.Singleton;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.sail.nativerdf.NativeStore;

import fr.ortolang.diffusion.OrtolangConfig;

@Local(TripleStoreService.class)
@Singleton(name = TripleStoreService.SERVICE_NAME)
public class TripleStoreServiceBean implements TripleStoreService {
	
	public static final String DEFAULT_TRIPLE_HOME = "/triple-store";
    
    private Logger logger = Logger.getLogger(TripleStoreServiceBean.class.getName());
    private Path base;
    private static Repository repository;
    
    public TripleStoreServiceBean() {
    	logger.log(Level.INFO, "Instanciating service");
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
	    } catch (Exception e) {
    		logger.log(Level.SEVERE, "unable to initialize triple store", e);
    	}
    }

    public Path getBase() {
		return base;
	}

	@Override
	public void importOntology(String ontologyURI, String resourceName) throws TripleStoreServiceException {
		logger.log(Level.INFO, "importing ontology [" + ontologyURI + "] into triple store");
        try {
            RepositoryConnection con = repository.getConnection();
            try {
                if (!con.hasStatement(new URIImpl(ontologyURI), new URIImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), new URIImpl("http://www.w3.org/2002/07/owl#Ontology"), false)) {
                    logger.log(Level.INFO, "ontology not present, importing...");
                    InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
                    con.add(is, ontologyURI, RDFFormat.RDFXML);
                } else {
                    logger.log(Level.INFO, "ontology already present, no need to import");
                }
            } finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to import ontology in store", e);
        }
	}

	@Override
	public void insertTriple(String subject, String predicate, String object) throws TripleStoreServiceException {
		try {
            Value objectValue;
            try {
                objectValue = new URIImpl(object);
            } catch (IllegalArgumentException iae) {
                objectValue = new LiteralImpl(object);
            }
            RepositoryConnection con = repository.getConnection();
            try {
                con.add(new URIImpl(subject), new URIImpl(predicate), objectValue);
            } finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to add triple in store", e);
        }
	}

	@Override
	public void removeTriple(String subject, String predicate, String object) throws TripleStoreServiceException {
		try {
            Value objectValue;
            try {
                objectValue = new URIImpl(object);
            } catch (IllegalArgumentException iae) {
                objectValue = new LiteralImpl(object);
            }
            RepositoryConnection con = repository.getConnection();
            try {
                con.remove(new URIImpl(subject), new URIImpl(predicate), objectValue);
            } finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to remove triple from store", e);
        }
	}

	@Override
	public void removeTriples(String subject, String predicate, String object) throws TripleStoreServiceException {
		URIImpl usubject = null;
        URIImpl upredicate = null;
        Value uobject = null;
        if (subject != null) {
            usubject = new URIImpl(subject);
        }
        if (predicate != null) {
            upredicate = new URIImpl(predicate);
        }
        if (object != null) {
            try {
                uobject = new URIImpl(object);
            } catch (IllegalArgumentException iae) {
                uobject = new LiteralImpl(object);
            }
        }
        if (usubject == null && upredicate == null && uobject == null) {
            throw new TripleStoreServiceException("unable to remove all triples of the store");
        }
        try {
            RepositoryConnection con = repository.getConnection();
            try {
                con.remove(usubject, upredicate, uobject);
            } finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to remove triples" + subject, e);
        }
	}

	@Override
	public List<Triple> listTriples(String subject, String predicate, String object) throws TripleStoreServiceException {
		URIImpl usubject = null;
        URIImpl upredicate = null;
        Value uobject = null;
        if (subject != null) {
            usubject = new URIImpl(subject);
        }
        if (predicate != null) {
            upredicate = new URIImpl(predicate);
        }
        if (object != null) {
            try {
                uobject = new URIImpl(object);
            } catch (IllegalArgumentException iae) {
                uobject = new LiteralImpl(object);
            }
        }
        try {
            RepositoryConnection con = repository.getConnection();
            try {
                RepositoryResult<Statement> statements = con.getStatements(usubject, upredicate, uobject, true);
                Vector<Triple> result = new Vector<Triple>();
                while (statements.hasNext()) {
                    Statement statement = statements.next();
                    Triple tuple = new Triple(statement.getSubject().stringValue(), statement.getPredicate().stringValue(), statement.getObject().stringValue());
                    result.add(tuple);
                }
                statements.close();
                return result;
            } finally {
                con.close();
            }
        } catch (Exception e) {
            throw new TripleStoreServiceException("unable to list triples" + subject, e);
        }
	}

	@Override
	public String query(String language, String query) throws TripleStoreServiceException {
		try {
        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
        	RepositoryConnection con = repository.getConnection();
        	try {
        		if ( language.equals(QueryLanguage.SERQL.getName()) ) {
	        		RDFXMLWriter writer = new RDFXMLWriter(baos);
		            GraphQuery gquery = con.prepareGraphQuery(QueryLanguage.SERQL, query);
	                gquery.evaluate(writer);
        		} else if ( language.equals(QueryLanguage.SPARQL.getName()) ) {
        			SPARQLResultsXMLWriter writer = new SPARQLResultsXMLWriter(baos);
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
	public void query(String language, String query, OutputStream os) throws TripleStoreServiceException {
		try {
        	RepositoryConnection con = repository.getConnection();
        	try {
        		if ( language.equals(QueryLanguage.SERQL.getName()) ) {
	        		RDFXMLWriter writer = new RDFXMLWriter(os);
		            GraphQuery gquery = con.prepareGraphQuery(QueryLanguage.SERQL, query);
	                gquery.evaluate(writer);
        		} else if ( language.equals(QueryLanguage.SPARQL.getName()) ) {
        			SPARQLResultsXMLWriter writer = new SPARQLResultsXMLWriter(os);
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

}
