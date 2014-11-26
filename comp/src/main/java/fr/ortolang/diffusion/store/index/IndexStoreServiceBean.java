package fr.ortolang.diffusion.store.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.store.DeleteFileVisitor;

@Local(IndexStoreService.class)
@Startup
@Singleton(name = IndexStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed({"system", "user"})
@Lock(LockType.READ)
public class IndexStoreServiceBean implements IndexStoreService {

	public static final String DEFAULT_INDEX_HOME = "/index-store";

	private static Logger logger = Logger.getLogger(IndexStoreServiceBean.class.getName());

	private Path base;
	private Analyzer analyzer;
	private Directory directory;
	private IndexWriterConfig config;
	private IndexWriter writer;
	
	public IndexStoreServiceBean() {
		logger.log(Level.INFO, "Instanciating service");
		this.base = Paths.get(OrtolangConfig.getInstance().getProperty("home"), DEFAULT_INDEX_HOME);
	}

	@PostConstruct
	public void init() {
		logger.log(Level.INFO, "Initializing service with base folder: " + base);
		try {
			if ( Files.exists(base) && Boolean.parseBoolean(OrtolangConfig.getInstance().getProperty("store.index.purge")) ) {
				logger.log(Level.FINEST, "base directory exists and config is set to purge, recursive delete of base folder");
				Files.walkFileTree(base, new DeleteFileVisitor());
			}
			analyzer = new StandardAnalyzer(Version.LUCENE_46);
			directory = FSDirectory.open(base.toFile());
			logger.log(Level.FINEST, "directory implementation: " + directory.getClass());
			config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
			writer = new IndexWriter(directory, config);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "unable to configure lucene index writer", e);
		}
	}

	@PreDestroy
	public void shutdown() {
		logger.log(Level.INFO, "Shuting down service");
		try {
			writer.close();
			directory.close();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "unable to close lucene index writer", e);
		}
	}
	
	public Path getBase() {
		return base;
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void index(OrtolangIndexableObject object) throws IndexStoreServiceException {
		logger.log(Level.FINE, "Indexing new object: " + object.getIdentifier());
		try {
			writer.addDocument(IndexStoreDocumentBuilder.buildDocument(object));
			writer.commit();
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to index object " + object, e);
			throw new IndexStoreServiceException("Can't index an object", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void reindex(OrtolangIndexableObject object) throws IndexStoreServiceException {
		logger.log(Level.FINE, "Reindexing object: " + object.getIdentifier());
		try {
			Term term = new Term("KEY", object.getKey());
			writer.updateDocument(term, IndexStoreDocumentBuilder.buildDocument(object));
			writer.commit();
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to index object " + object, e);
			throw new IndexStoreServiceException("Can't index an object", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void remove(String key) throws IndexStoreServiceException {
		logger.log(Level.FINE, "Removing key: " + key);
		try {
			Term term = new Term("KEY", key);
			writer.deleteDocuments(term);
			writer.commit();
		} catch (IOException e) {
			logger.log(Level.WARNING, "unable to remove object " + key + " from index", e);
			throw new IndexStoreServiceException("Can't remove object " + key + " from index", e);
		}
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<OrtolangSearchResult> search(String queryString) throws IndexStoreServiceException {
		logger.log(Level.FINE, "Searching query: " + queryString);
		try {
			//IndexReader reader = DirectoryReader.open(writer, true);
			IndexReader reader = DirectoryReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
			QueryParser parser = new QueryParser(Version.LUCENE_46, "CONTENT", analyzer);
			Query query = parser.parse(queryString);

			TopDocs docs = searcher.search(query, 100);
			ArrayList<OrtolangSearchResult> results = new ArrayList<OrtolangSearchResult>(docs.totalHits);
			SimpleHTMLFormatter formatter = new SimpleHTMLFormatter("<highlighted>", "</highlighted>");
			QueryScorer scorer = new QueryScorer(query);
			Highlighter highlighter = new Highlighter(formatter, scorer);

			for (int i = 0; i < docs.scoreDocs.length; i++) {
				Document doc = searcher.doc(docs.scoreDocs[i].doc);
				float score = docs.scoreDocs[i].score;
				String identifier = doc.get("IDENTIFIER");
				String higlightedText = highlighter.getBestFragment(analyzer, "CONTENT", doc.get("CONTENT"));
				String name = doc.get("NAME");
				String service = doc.get("SERVICE");
				String type = doc.get("TYPE");
				String key = doc.get("KEY");
				String root = doc.get("ROOT");
				String path = doc.get("PATH");
				
				OrtolangSearchResult result = new OrtolangSearchResult();
				result.setScore(score);
				result.setName(name);
				result.setIdentifier(identifier);
				result.setService(service);
				result.setType(type);
				result.setKey(key);
				result.setExplain(higlightedText);
				result.setRoot(root);
				result.setPath(path);
				results.add(result);
			}
			return results;
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable search in index using " + queryString, e);
			throw new IndexStoreServiceException("Can't search in index using '" + queryString + "'\n", e);
		}
	}
	
}
