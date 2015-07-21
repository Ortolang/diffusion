package fr.ortolang.diffusion.store.index;

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
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
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
@Lock(LockType.READ)
@PermitAll
public class IndexStoreServiceBean implements IndexStoreService {

    public static final String DEFAULT_INDEX_HOME = "/index-store";

    private static final Logger LOGGER = Logger.getLogger(IndexStoreServiceBean.class.getName());

    private Path base;
    private Analyzer analyzer;
    private Directory directory;
    private IndexWriterConfig config;
    private IndexWriter writer;

    public IndexStoreServiceBean() {
        LOGGER.log(Level.INFO, "Instanciating service");
        this.base = Paths.get(OrtolangConfig.getInstance().getHome(), DEFAULT_INDEX_HOME);
    }

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initializing service with base folder: " + base);
        try {
            if ( Files.exists(base) && Boolean.parseBoolean(OrtolangConfig.getInstance().getProperty("store.index.purge")) ) {
                LOGGER.log(Level.FINEST, "base directory exists and config is set to purge, recursive delete of base folder");
                Files.walkFileTree(base, new DeleteFileVisitor());
            }
            analyzer = new FrenchAnalyzer(Version.LUCENE_46);
            directory = FSDirectory.open(base.toFile());
            LOGGER.log(Level.FINEST, "directory implementation: " + directory.getClass());
            config = new IndexWriterConfig(Version.LUCENE_46, analyzer);
            writer = new IndexWriter(directory, config);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "unable to configure lucene index writer", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.log(Level.INFO, "Shuting down service");
        try {
            writer.close();
            directory.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "unable to close lucene index writer", e);
        }
    }

    public Path getBase() {
        return base;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void index(OrtolangIndexableObject<IndexablePlainTextContent> object) throws IndexStoreServiceException {
        LOGGER.log(Level.FINE, "Indexing new object: " + object.getIdentifier());
        try {
            Term term = new Term("KEY", object.getKey());
            writer.deleteDocuments(term);
            writer.addDocument(IndexStoreDocumentBuilder.buildDocument(object));
            writer.commit();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "unable to index object " + object, e);
            throw new IndexStoreServiceException("Can't index an object", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void remove(String key) throws IndexStoreServiceException {
        LOGGER.log(Level.FINE, "Removing key: " + key);
        try {
            Term term = new Term("KEY", key);
            writer.deleteDocuments(term);
            writer.commit();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "unable to remove object " + key + " from index", e);
            throw new IndexStoreServiceException("Can't remove object " + key + " from index", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OrtolangSearchResult> search(String queryString) throws IndexStoreServiceException {
        LOGGER.log(Level.FINE, "Searching query: " + queryString);
        try {
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
            LOGGER.log(Level.WARNING, "unable search in index using " + queryString, e);
            throw new IndexStoreServiceException("Can't search in index using '" + queryString + "'\n", e);
        }
    }

}
