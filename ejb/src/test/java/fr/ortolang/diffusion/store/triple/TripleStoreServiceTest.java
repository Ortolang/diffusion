package fr.ortolang.diffusion.store.triple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;

public class TripleStoreServiceTest {
	
	private Logger logger = Logger.getLogger(TripleStoreServiceTest.class.getName());
	private TripleStoreServiceBean service;
	
	@Before
	public void setup() {
		try {
			service = new TripleStoreServiceBean();
			service.init();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@After
	public void tearDown() {
		try {
			Files.walkFileTree(service.getBase(), new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					logger.log(Level.SEVERE, "unable to purge temporary created filesystem", exc);
					return FileVisitResult.TERMINATE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
				
			});
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	@Test
    public void testInsertTriple() {
        try {
            service.insertTriple("http://diffusion.ortolang.fr/objects/uuid1", "http://diffusion.ortolang.fr/predicates/isMember", "http://diffusion.ortolang.fr/objects/uuid2");
            service.insertTriple("http://diffusion.ortolang.fr/objects/uuid1", "http://diffusion.ortolang.fr/predicates/age", "30");
            service.insertTriple("http://diffusion.ortolang.fr/objects/uuid1", "http://diffusion.ortolang.fr/predicates/name", "UUID Object One");
            assertTrue(service.listTriples("http://diffusion.ortolang.fr/objects/uuid1", null, null).size() > 0);
            service.removeTriples("http://diffusion.ortolang.fr/objects/uuid1", null, null);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testRemoveTriple() {
        try {
            service.insertTriple("http://diffusion.ortolang.fr/objects/uuid10", "http://diffusion.ortolang.fr/predicates/contains", "http://diffusion.ortolang.fr/objects/uuid1");
            service.insertTriple("http://diffusion.ortolang.fr/objects/uuid10", "http://diffusion.ortolang.fr/predicates/contains", "http://diffusion.ortolang.fr/objects/uuid2");
            service.insertTriple("http://diffusion.ortolang.fr/objects/uuid10", "http://diffusion.ortolang.fr/predicates/contains", "http://diffusion.ortolang.fr/objects/uuid3");
            service.insertTriple("http://diffusion.ortolang.fr/objects/uuid10", "http://diffusion.ortolang.fr/predicates/age", "52");
            service.insertTriple("http://diffusion.ortolang.fr/objects/uuid10", "http://diffusion.ortolang.fr/predicates/name", "Object Collection Ten");
            assertEquals(5, service.listTriples("http://diffusion.ortolang.fr/objects/uuid10", null, null).size());
            service.removeTriple("http://diffusion.ortolang.fr/objects/uuid10", "http://diffusion.ortolang.fr/predicates/age", "52");
            assertEquals(4, service.listTriples("http://diffusion.ortolang.fr/objects/uuid10", null, null).size());
            service.removeTriples("http://diffusion.ortolang.fr/objects/uuid10", null, null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "an error occured", e);
            fail(e.getMessage());
        }
    }

//    @Test
//    public void testRemoveTriples() {
//        try {
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/gerald");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/jayblanc");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/age", "52");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/name", "Profile One");
//            assertEquals(5, service.listTriples("http://diffusion.ortolang.fr/profiles/momo", null, null).size());
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", null);
//            assertEquals(2, service.listTriples("http://diffusion.ortolang.fr/profiles/momo", null, null).size());
//            service.removeTriples(null, null, "Profile One");
//            assertEquals(1, service.listTriples("http://diffusion.ortolang.fr/profiles/momo", null, null).size());
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/momo", null, null);
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "an error occured", e);
//            fail(e.getMessage());
//        }
//    }
//
//    @Test
//    public void testRemoveUnexistingTriples() {
//        try {
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/gerald");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/jayblanc");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/age", "52");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/name", "Profile One");
//            assertEquals(5, service.listTriples("http://diffusion.ortolang.fr/profiles/momo", null, null).size());
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", null);
//            assertEquals(2, service.listTriples("http://diffusion.ortolang.fr/profiles/momo", null, null).size());
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", null);
//            assertEquals(2, service.listTriples("http://diffusion.ortolang.fr/profiles/momo", null, null).size());
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/momo", null, null);
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "an error occured", e);
//            fail(e.getMessage());
//        }
//    }
//
//    @Test
//    public void testListTriples() {
//        try {
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/gerald");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/jayblanc");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isBoss", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/loves", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/age", "52");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/jayblanc", "http://diffusion.ortolang.fr/predicates/age", "30");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/chris", "http://diffusion.ortolang.fr/predicates/age", "52");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/gerald", "http://diffusion.ortolang.fr/predicates/age", "34");
//            assertEquals(6, service.listTriples("http://diffusion.ortolang.fr/profiles/momo", null, null).size());
//            assertEquals(3, service.listTriples("http://diffusion.ortolang.fr/profiles/momo", null, "http://diffusion.ortolang.fr/profiles/chris").size());
//            assertEquals(3, service.listTriples(null, null, "http://diffusion.ortolang.fr/profiles/chris").size());
//            assertEquals(1, service.listTriples(null, null, "http://diffusion.ortolang.fr/profiles/gerald").size());
//            assertEquals(3, service.listTriples(null, "http://diffusion.ortolang.fr/predicates/isSlave", null).size());
//            assertEquals(2, service.listTriples(null, "http://diffusion.ortolang.fr/predicates/age", "52").size());
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/momo", null, null);
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/jayblanc", null, null);
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/chris", null, null);
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/gerald", null, null);
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "an error occured", e);
//            fail(e.getMessage());
//        }
//    }
//
//    @Test
//    public void testSERQLQuery() {
//        try {
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/gerald");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/jayblanc");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isBoss", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/loves", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/chris", "http://diffusion.ortolang.fr/predicates/loves", "http://diffusion.ortolang.fr/profiles/momo");
//
//            String result = service.query(TripleStoreService.SERQL_QUERY_LANGUAGE, 
//                    "CONSTRUCT {Boss} plant:isBoss {Slave} FROM {Slave} plant:isSlave {Boss} USING NAMESPACE plant = <http://diffusion.ortolang.fr/predicates/>");
//            logger.log(Level.WARNING, result);
//            RDFXMLParser parser = new RDFXMLParser();
//            CountStatementsRDFHandler handler = new CountStatementsRDFHandler();
//            parser.setRDFHandler(handler);
//            parser.parse(new ByteArrayInputStream(result.getBytes()), "http://diffusion.ortolang.fr/");
//            assertEquals(3, handler.getCounterStatus());
//            
//
//            String result2 = service.query(TripleStoreService.SERQL_QUERY_LANGUAGE, 
//                    "CONSTRUCT {Boss} plant:isBoss {Slave} FROM {Slave} plant:isSlave {Boss}; plant:loves {Boss} USING NAMESPACE plant = <http://diffusion.ortolang.fr/predicates/>");
//            logger.log(Level.INFO, result2);
//            handler.resetCounter();
//            parser.parse(new ByteArrayInputStream(result2.getBytes()), "http://diffusion.ortolang.fr/");
//            assertEquals(1, handler.getCounterStatus());
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/momo", null, null);
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/chris", null, null);
//        } catch (Exception e) {
//        	e.printStackTrace();
//            logger.log(Level.WARNING, "an error occured", e);
//            fail(e.getMessage());
//        }
//    }
//
//    @Test
//    public void testSPARQLQuery() {
//        try {
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/gerald");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/jayblanc");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isSlave", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/isBoss", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/loves", "http://diffusion.ortolang.fr/profiles/chris");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/chris", "http://diffusion.ortolang.fr/predicates/loves", "http://diffusion.ortolang.fr/profiles/momo");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/chris", "http://diffusion.ortolang.fr/predicates/age", "35");
//            service.insertTriple("http://diffusion.ortolang.fr/profiles/momo", "http://diffusion.ortolang.fr/predicates/age", "42");
//
//            String result = service.query(TripleStoreService.SPARQL_QUERY_LANGUAGE, "SELECT ?x WHERE { ?x <http://diffusion.ortolang.fr/predicates/isSlave> <http://diffusion.ortolang.fr/profiles/jayblanc> }");
//            logger.log(Level.INFO, result);
//            SPARQLResultsXMLParser parser = new SPARQLResultsXMLParser();
//            CountTuplesResultHandler handler = new CountTuplesResultHandler();
//            parser.setTupleQueryResultHandler(handler);
//            parser.parse(new ByteArrayInputStream(result.getBytes()));
//            
//            assertEquals(1, handler.getCounterStatus());
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/momo", null, null);
//            service.removeTriples("http://diffusion.ortolang.fr/profiles/chris", null, null);
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "an error occured", e);
//            fail(e.getMessage());
//        }
//    }
//
//    @Test
//    public void testRDFSImport() {
//        try {
//            List<Triple> results = service.listTriples("http://www.w3.org/2000/01/rdf-schema#", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.w3.org/2002/07/owl#Ontology");
//            assertEquals(1, results.size());
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "an error occured", e);
//            fail(e.getMessage());
//        }
//    }
//
//    @Test
//    public void testOWLImport() {
//        try {
//            List<Triple> results = service.listTriples("http://www.w3.org/2002/07/owl", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.w3.org/2002/07/owl#Ontology");
//            assertEquals(1, results.size());
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "an error occured", e);
//            fail(e.getMessage());
//        }
//    }
//
//    @Test
//    public void testListAllStore() {
//        try {
//            List<Triple> results = service.listTriples(null, null, null);
//            for (Triple result : results) {
//                System.out.println(result.getSubject() + " -> " + result.getPredicate() + " -> " + result.getObject());
//            }
//        } catch (Exception e) {
//            logger.log(Level.WARNING, "an error occured", e);
//            fail(e.getMessage());
//        }
//    }

    public class CountStatementsRDFHandler implements RDFHandler {
    	
    	private int counter=0;
    	
    	public CountStatementsRDFHandler() {
    	}
    	
    	public int getCounterStatus() {
    		return counter;
    	}
    	
    	public void resetCounter() {
    		counter = 0;
    	}

    	@Override
		public void startRDF() throws RDFHandlerException {
		}
    
		@Override
		public void handleComment(String arg0) throws RDFHandlerException {
		}

		@Override
		public void handleNamespace(String arg0, String arg1) throws RDFHandlerException {
		}

		@Override
		public void	handleStatement(Statement stmt) {
    		counter++;
    	}
    	
    	@Override
		public void endRDF() throws RDFHandlerException {
		}
	}
    
    public class CountTuplesResultHandler implements TupleQueryResultHandler {
    	
    	private int counter=0;
    	
    	public CountTuplesResultHandler() {
    	}
    	
    	public int getCounterStatus() {
    		return counter;
    	}
    	
    	public void resetCounter() {
    		counter = 0;
    	}

		@Override
		public void startQueryResult(List<String> arg0) throws TupleQueryResultHandlerException {
		}

		@Override
		public void handleSolution(BindingSet arg0) throws TupleQueryResultHandlerException {
			counter++;
		}
		
		@Override
		public void handleBoolean(boolean arg0) throws QueryResultHandlerException {
		}

		@Override
		public void handleLinks(List<String> arg0) throws QueryResultHandlerException {
		}
		
		@Override
		public void endQueryResult() throws TupleQueryResultHandlerException {
		}

		
    }


}
