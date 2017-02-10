package fr.ortolang.diffusion.indexing.elastic;

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

import fr.ortolang.diffusion.*;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.search.SearchQuery;
import fr.ortolang.diffusion.search.SearchResult;
import fr.ortolang.diffusion.util.StreamUtils;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.DocumentMissingException;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.InternalNested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.elasticsearch.script.Script.DEFAULT_SCRIPT_LANG;

@Startup
@Local(ElasticSearchService.class)
@Singleton(name = ElasticSearchService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ElasticSearchServiceBean implements ElasticSearchService {

    private static final Logger LOGGER = Logger.getLogger(ElasticSearchServiceBean.class.getName());

    @EJB
    private RegistryService registry;

    private TransportClient client;

    private Map<String, Set<String>> indices;

    private Map<String, String> scripts;

    public ElasticSearchServiceBean() throws IOException {
        LOGGER.log(Level.INFO, "Instantiating Elastic Search Service");
        indices = new HashMap<>();
        scripts = new HashMap<>();
        LOGGER.log(Level.INFO, "Loading Painless scripts");
        LOGGER.log(Level.INFO, "Load updateRootCollectionChild script");
        String script = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("indexing/updateRootCollectionChild.painless"), StandardCharsets.UTF_8);
        scripts.put("updateRootCollectionChild", script);
    }

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initializing Elastic Search Service");
        try {
            if (OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.ELASTIC_SEARCH_HOST) == null || OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.ELASTIC_SEARCH_HOST).length() == 0) {
                LOGGER.log(Level.INFO, "Elastic Search not configured, skipping initialization");
                client = null;
                return;
            }
            String[] hosts = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.ELASTIC_SEARCH_HOST).split(",");
            String[] ports = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.ELASTIC_SEARCH_PORT).split(",");

            if (hosts.length != ports.length) {
                throw new IllegalStateException("Elastic search configuration incorrect: host number and port number should be equal");
            }
            Settings settings = Settings.builder().put("cluster.name", "ortolang").build();
            client = new PreBuiltTransportClient(settings);
            for (int i = 0; i < hosts.length; i++) {
                client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hosts[i]), Integer.parseInt(ports[i])));
            }
        } catch (UnknownHostException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.log(Level.INFO, "Shutting down Elastic Search Service");
        client.close();
    }

    @Override
    public void index(String key) throws ElasticSearchServiceException {
        try {
            LOGGER.log(Level.FINEST, "Starting to index key [" + key + "]");
            if (client == null || client.connectedNodes().isEmpty()) {
                return;
            }
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
            List<OrtolangIndexableContent> indexableContents = service.getIndexableContent(key);
            for (OrtolangIndexableContent indexableContent : indexableContents) {
                LOGGER.log(Level.FINEST, "Start to index key [" + indexableContent.getId() + "] of type [" + indexableContent.getType() + "] in index [" + indexableContent.getIndex() + "]");
                if (!indexableContent.isEmpty()) {
                    try {
                        IndicesAdminClient adminClient = client.admin().indices();
                        if (!indices.containsKey(indexableContent.getIndex())) {
                            // Check if index exists and create it if not
                            if (adminClient.prepareExists(indexableContent.getIndex()).get().isExists()) {
                                indices.put(indexableContent.getIndex(), new HashSet<>());
                                checkType(indexableContent, adminClient);
                            } else {
                                LOGGER.log(Level.INFO, "Creating index [" + indexableContent.getIndex() + "] and add mapping for type [" + indexableContent.getType() + "]");
                                CreateIndexRequestBuilder requestBuilder = adminClient.prepareCreate(indexableContent.getIndex());
                                InputStream mapping = this.getClass().getResourceAsStream(PATH_TO_MAPPINGS + indexableContent.getType() + EXTENSION_MAPPING);
                                if (mapping != null) {
                                	String mappingAsString = StreamUtils.getContent(mapping);
                                	if (mappingAsString != null) {                                		
                                		requestBuilder.addMapping(indexableContent.getType(), mappingAsString);
                                	} else {
                                		LOGGER.log(Level.SEVERE, "Unable to put mapping : cannot read file : "+PATH_TO_MAPPINGS + indexableContent.getType() + EXTENSION_MAPPING);
                                	}
                                } else {
                                	LOGGER.log(Level.SEVERE, "Unable to put mapping : file not found : "+PATH_TO_MAPPINGS + indexableContent.getType() + EXTENSION_MAPPING);
                                }
                                requestBuilder.get();
                                HashSet<String> types = new HashSet<>();
                                types.add(indexableContent.getType());
                                indices.put(indexableContent.getIndex(), types);
                            }
                        } else {
                            checkType(indexableContent, adminClient);
                        }
                        if (indexableContent.isUpdate()) {
                            LOGGER.log(Level.FINE, "Updating key [" + indexableContent.getId() + "] in index [" + indexableContent.getIndex() + "] with type [" + indexableContent.getType() + "]");

                            Script script = new Script(ScriptType.INLINE, DEFAULT_SCRIPT_LANG, scripts.get(indexableContent.getScript()), indexableContent.getScriptParams());
                            client.prepareUpdate(indexableContent.getIndex(), indexableContent.getType(), indexableContent.getId()).setScript(script).get();
                        } else {
                            LOGGER.log(Level.FINE, "Indexing key [" + indexableContent.getId() + "] in index [" + indexableContent.getIndex() + "] with type [" + indexableContent.getType() + "]");
                            client.prepareIndex(indexableContent.getIndex(), indexableContent.getType(), indexableContent.getId()).setSource(indexableContent.getContent()).get();
                        }
                    } catch (IndexNotFoundException e) {
                        LOGGER.log(Level.INFO, "Index not found: removing it from registry and re-trying to index key [" + key + "]");
                        indices.remove(indexableContent.getIndex());
                        index(key);
                        return;
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.WARNING, "IllegalArgumentException for key [" + indexableContent.getId() + "] with type [" + indexableContent.getType() +
                                "] in index [" + indexableContent.getIndex() + "]", e);
                        return;
                    } catch (DocumentMissingException e) {
                        LOGGER.log(Level.WARNING, "Document missing for key [" + indexableContent.getId() + "] with type [" + indexableContent.getType() +
                                "] in index [" + indexableContent.getIndex() + "] " + e.getMessage());
                    } catch (MapperParsingException e) {
                        LOGGER.log(Level.WARNING, "MapperParsingException for key [" + indexableContent.getId() + "] with type [" + indexableContent.getType() +
                                "] in index [" + indexableContent.getIndex() + "]", e);
                        throw new ElasticSearchServiceException(e.getMessage(), e);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "An unexpected error happened while indexing key [" + indexableContent.getId() + "]", e);
                        // TODO throw exception ???
                        return;
                    }
                }
            }
        } catch (OrtolangException | KeyNotFoundException | RegistryServiceException | IndexingServiceException e) {
            throw new ElasticSearchServiceException("An unexpected error happened while indexing key [" + key + "]", e);
        }
    }

    @Override
    public void remove(String key) throws ElasticSearchServiceException {
        // TODO not implemented
    }

    @Override
    public SearchResult search(SearchQuery query) {
    	LOGGER.log(Level.FINE, "Search in " + query.getIndex() + " and type " + query.getType() + " with query " + query.getQuery());
        SearchRequestBuilder searchRequest = client.prepareSearch();
        if (!query.getQuery().isEmpty()) {
        	QueryBuilder queryBuilder = ElasticSearchQueryParser.parse(query.getQuery());
	        if (queryBuilder!=null) {
	        	searchRequest.setQuery(queryBuilder);
	        }
        }
        if (query.getIndex() != null) {
            searchRequest.setIndices(query.getIndex());
        }
        if (query.getType() != null) {
            searchRequest.setTypes(query.getType());
        }
        if (query.getSize() != null) {
        	searchRequest.setSize(query.getSize());
        }
        searchRequest.setFetchSource(query.getIncludes(), query.getExcludes());
        if (query.getAggregations() != null) {
	        for(String agg : query.getAggregations()) {
	        	searchRequest.addAggregation(ElasticSearchAggregationParser.parse(agg));
	        }
        }
        LOGGER.log(Level.FINE, searchRequest.toString());
        SearchResponse searchResponse = searchRequest.get();
        
        // Parses
        SearchResult result = new SearchResult();
        SearchHit[] searchHits = searchResponse.getHits().getHits();
        for (SearchHit searchHit : searchHits) {
        	result.addHit(searchHit.getSourceAsString());
        }
        if (query.getAggregations() != null) {
	        for(String agg : query.getAggregations()) {
	        	//TODO optimizes by using forEach method
	        	if (agg.endsWith("[]")) {
	        		String aggName = agg.substring(0, agg.length() - 2);
	        		InternalNested nestedAgg = searchResponse.getAggregations().get(aggName);
	       		 	Terms terms = nestedAgg.getAggregations().get("content");
	       		 	for(Terms.Bucket bucket : terms.getBuckets()) {
			        	result.addAggregation(aggName, bucket.getKeyAsString());
			        }
	        	} else {
			        Terms terms = searchResponse.getAggregations().get(agg);
			        for(Terms.Bucket bucket : terms.getBuckets()) {
			        	result.addAggregation(agg, bucket.getKeyAsString());
			        }
	        	}
	        }
        }
        return result;
    }
    
    @Override
    public String get(String index, String type, String id) {
    	GetRequestBuilder requestBuilder = client.prepareGet(index, type, id);
		
		GetResponse getResponse = requestBuilder.get();
		if(getResponse.isExists()) {
			return getResponse.getSourceAsString();
		}
		return null;
    }

    private void checkType(OrtolangIndexableContent indexableContent, IndicesAdminClient adminClient) {
        if (!indices.get(indexableContent.getIndex()).contains(indexableContent.getType())) {
            LOGGER.log(Level.INFO, "Put mapping for type [" + indexableContent.getType() + "] in index [" + indexableContent.getIndex() + "]");
            try {
	            InputStream mapping = this.getClass().getResourceAsStream(PATH_TO_MAPPINGS + indexableContent.getType() + EXTENSION_MAPPING);
	            if (mapping != null) {
	            	String mappingAsString;
						mappingAsString = StreamUtils.getContent(mapping);
	            	if (mappingAsString != null) {
	            		adminClient.preparePutMapping(indexableContent.getIndex())
	                    .setType(indexableContent.getType())
	                    .setSource(mappingAsString)
	                    .get();
	            	} else {
	            		LOGGER.log(Level.SEVERE, "Unable to put mapping : cannot read file : "+PATH_TO_MAPPINGS + indexableContent.getType() + EXTENSION_MAPPING);
	            	}
	            } else {
	            	LOGGER.log(Level.SEVERE, "Unable to put mapping : file not found : "+PATH_TO_MAPPINGS + indexableContent.getType() + EXTENSION_MAPPING);
	            }
	            
	            indices.get(indexableContent.getIndex()).add(indexableContent.getType());
            } catch (IOException e) {
            	LOGGER.log(Level.SEVERE, "Unabled to put mapping for type [" + indexableContent.getType() + "] in index [" + indexableContent.getIndex() + "]", e);
            }
        }
    }

    @Override
    public String getServiceName() {
        return ElasticSearchService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

}
