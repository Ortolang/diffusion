package fr.ortolang.diffusion.store.triple;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
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
import fr.ortolang.diffusion.store.DeleteFileVisitor;

@Local(TripleStoreService.class)
@Startup
@Singleton(name = TripleStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@Lock(LockType.READ)
@PermitAll
public class TripleStoreServiceBean implements TripleStoreService {
	
	public static final String DEFAULT_TRIPLE_HOME = "/triple-store";
	
    
    private static final Logger LOGGER = Logger.getLogger(TripleStoreServiceBean.class.getName());
    private Path base;
    private static Repository repository;
    
    public TripleStoreServiceBean() {
    	LOGGER.log(Level.FINE, "Instanciating service");
    	this.base = Paths.get(OrtolangConfig.getInstance().getHome(), DEFAULT_TRIPLE_HOME);
    }

    @PostConstruct
    public void init() {
    	LOGGER.log(Level.INFO, "Initializing service with base folder: " + base);
    	try {
    		if ( Files.exists(base) && Boolean.parseBoolean(OrtolangConfig.getInstance().getProperty("store.triple.purge")) ) {
				LOGGER.log(Level.FINEST, "base directory exists and config is set to purge, recursive delete of base folder");
				Files.walkFileTree(base, new DeleteFileVisitor());
			} 
    		Files.createDirectories(base);
    		repository = new SailRepository(new NativeStore(base.toFile()));
            repository.initialize();
            this.importOntology("http://www.w3.org/2000/01/rdf-schema#", "ontology/rdfs.xml");
            this.importOntology("http://xmlns.com/foaf/0.1/", "ontology/foaf.xml");
            //this.importOntology("http://lexvo.org/ontology", "ontology/lexvo-ontology.xml");
            //this.importOntology("http://lexvo.org/dump", "ontology/lexvo_2013-02-09.rdf");
            this.importOntology("http://www.ortolang.fr/2014/05/diffusion#", "ontology/ortolang.xml");
            this.importOntology("http://www.ortolang.fr/2014/09/market#", "ontology/ortolang-market.xml");
	    } catch (Exception e) {
    		LOGGER.log(Level.SEVERE, "unable to initialize triple store", e);
    	}
    }
    
    @PreDestroy
    public void shutdown() {
    	LOGGER.log(Level.INFO, "Shuting down triple store");
    	try {
    		repository.shutDown();
        } catch (Exception e) {
    		LOGGER.log(Level.SEVERE, "unable to shutdown triple store", e);
    	}
    }

    public Path getBase() {
		return base;
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void importOntology(String ontologyURI, String resourceName) throws TripleStoreServiceException {
		LOGGER.log(Level.FINE, "importing ontology [" + ontologyURI + "] into triple store");
        try {
            RepositoryConnection con = repository.getConnection();
            try {
                if (!con.hasStatement(new URIImpl(ontologyURI), new URIImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), new URIImpl("http://www.w3.org/2002/07/owl#Ontology"), false)) {
                    LOGGER.log(Level.FINE, "ontology not present, importing...");
                    InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
                    con.add(is, ontologyURI, RDFFormat.RDFXML);
                } else {
                    LOGGER.log(Level.FINE, "ontology already present, no need to import");
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
	public void index(OrtolangIndexableObject<IndexableSemanticContent> object) throws TripleStoreServiceException {
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
	public void reindex(OrtolangIndexableObject<IndexableSemanticContent> object) throws TripleStoreServiceException {
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
