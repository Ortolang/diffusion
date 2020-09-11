package fr.ortolang.diffusion.api.sru.fcs;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLTermNode;

import eu.clarin.sru.server.CQLQueryParser;
import eu.clarin.sru.server.SRUConfigException;
import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUDiagnosticList;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.SRUQuery;
import eu.clarin.sru.server.SRUQueryParserRegistry.Builder;
import eu.clarin.sru.server.SRURequest;
import eu.clarin.sru.server.SRUSearchResultSet;
import eu.clarin.sru.server.SRUServerConfig;
import eu.clarin.sru.server.fcs.Constants;
import eu.clarin.sru.server.fcs.EndpointDescription;
import eu.clarin.sru.server.fcs.SimpleEndpointSearchEngineBase;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.content.ContentSearchService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.indexing.DataObjectContentIndexableContent;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.search.Highlight;
import fr.ortolang.diffusion.search.SearchQuery;
import fr.ortolang.diffusion.search.SearchResult;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.store.handle.HandleNotFoundException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;
import fr.ortolang.diffusion.store.handle.entity.Handle;

/**
 * ORTOLANG search engine endpoint implementation supporting SRU calls
 * with operation explain and search retrieve.
 * 
 * @author cpestel
 * */
public class OrtolangEndpoint extends SimpleEndpointSearchEngineBase {

	private static final Logger LOGGER = Logger.getLogger(OrtolangEndpoint.class.getName());
	
	public static final String CLARIN_FCS_RECORD_SCHEMA = "http://clarin.eu/fcs/resource";
	public static final String ORTOLANG_WEB_URL = "http://www.ortolang.fr/";
	
	public static final String HIGHLIGHT_PRETAG = "<hits:Hit>";
	public static final String HIGHLIGHT_POSTTAG = "</hits:Hit>";
	
	private static final int SEARCH_SIZE = 1000;
	
	public static final String CAPABILITY_BASIC_SEARCH = "http://clarin.eu/fcs/capability/basic-search";
	
	private OrtolangEndpointDescription ortolangEndpointDescription;

    private SearchService search;
    private HandleStoreService handleStore;
    private RegistryService registry;
    
	@Override
	protected EndpointDescription createEndpointDescription(ServletContext context, SRUServerConfig config,
			Map<String, String> params) throws SRUConfigException {
		try {
			ortolangEndpointDescription = new OrtolangEndpointDescription(context);
		} catch (OrtolangException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		return ortolangEndpointDescription;
	}

	@Override
	protected void doInit(ServletContext context, SRUServerConfig config, Builder queryParsersBuilder,
			Map<String, String> params) throws SRUConfigException {
		try {
			search = (SearchService) OrtolangServiceLocator.findService(SearchService.SERVICE_NAME);
			handleStore = (HandleStoreService) OrtolangServiceLocator.findService(HandleStoreService.SERVICE_NAME);
			registry = (RegistryService) OrtolangServiceLocator.findService(RegistryService.SERVICE_NAME);
		} catch (OrtolangException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * Searches to the index store based on the FCS request.
	 */
	@Override
	public SRUSearchResultSet search(SRUServerConfig config, SRURequest request, SRUDiagnosticList diagnostics)
			throws SRUException {

		//
        checkRequestRecordSchema(request);

        String queryStr = request.getQuery().getRawQuery();
        if ((queryStr == null) || queryStr.isEmpty()) {
            throw new SRUException(SRUConstants.SRU_EMPTY_TERM_UNSUPPORTED, "Empty term is not supported.");
        }
        
        String requestDataview = request.getExtraRequestData("x-fcs-dataviews");
        if (requestDataview != null && !requestDataview.isEmpty()) {
        	for( String dataviewWanted : requestDataview.split(",")) {
    			if (!dataviewWanted.equals("hits")) {
    				diagnostics.addDiagnostic(
                        Constants.FCS_DIAGNOSTIC_REQUESTED_DATA_VIEW_INVALID,
                        "The requested Data View " + requestDataview + " is not supported.",
                        "Using the default Data View(s): hits.");
    			}
        	}
        }
        
        String context = request.getExtraRequestData("x-fcs-context");
        SearchQuery query;
        if (request.isQueryType(Constants.FCS_QUERY_TYPE_CQL)) {
        	query = convertESQueryFromCQL(request.getQuery(CQLQueryParser.CQLQuery.class), context);
        } else {
        	throw new SRUException(
 				   SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
 				   "Queries with queryType '" +
 				   request.getQueryType() +
 				   "' are not supported by this CLARIN-FCS Endpoint.");
        }
        
        SearchResult result = search.search(query);
        
		return new OrtolangSRUSearchResultSet(diagnostics, OrtolangSearchHits.valueOf(result), request.getStartRecord() - 2);
	}


    private void checkRequestRecordSchema(SRURequest request)
            throws SRUException {
        final String recordSchemaIdentifier = request
                .getRecordSchemaIdentifier();
        if ((recordSchemaIdentifier != null)
                && !recordSchemaIdentifier.equals(CLARIN_FCS_RECORD_SCHEMA)) {
            throw new SRUException(
                    SRUConstants.SRU_UNKNOWN_SCHEMA_FOR_RETRIEVAL,
                    recordSchemaIdentifier, "Record schema \""
                            + recordSchemaIdentifier
                            + "\" is not supported by this endpoint.");
        }
    }
    
    /**
     * Creates a SearchQuery based on the query (query parameter) and the context (x-fcs-context parameter).
     * @param query the query
     * @param context the context from fcs
     * @return 
     * @throws SRUException
     */
    private SearchQuery convertESQueryFromCQL(SRUQuery<CQLNode> query, String context) throws SRUException {
    	final CQLNode node = query.getParsedQuery();
    	
    	String index = ContentSearchService.SERVICE_NAME;
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setIndex(index);
        searchQuery.setType(DataObject.OBJECT_TYPE);
        searchQuery.setHighlight(Highlight.highlight().field(DataObjectContentIndexableContent.CONTENT_FIELD).preTag(HIGHLIGHT_PRETAG).postTag(HIGHLIGHT_POSTTAG));
        searchQuery.setSize(SEARCH_SIZE);
        if (context != null && context.trim().length()>0) {
        	Set<String> wsKeys = new HashSet<String>();
        	for (String handle : context.trim().split(",")) {        		
        		String wskey = findWorkspaceKey(handle);
        		if (wskey != null) {
        			wsKeys.add(wskey);
        		}
        	}
        	searchQuery.addQuery("workspace.key.keyword", wsKeys.stream().toArray(String[]::new));
        }
        
        StringBuilder builder = new StringBuilder();
        if (node instanceof CQLTermNode) {
            CQLTermNode ctn = (CQLTermNode) node;

            builder.append(ctn.getTerm());
        } else {
            throw new SRUException(
				   SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
				   "unknown cql node: " + node);
        }
        searchQuery.addQuery("content*", new String[] {builder.toString()});
        
    	return searchQuery;
    }
    
    /**
     * Finds the workspace key from the handle URL.
     * @param context
     * @return
     * @throws SRUException
     */
    private String findWorkspaceKey(String handleURL) throws SRUException {
    	try {
    		String handleName = URLDecoder.decode(handleURL, StandardCharsets.UTF_8.name());
    		handleName = handleURL.replaceFirst(HandleStoreService.HDL_PROXY_URL, "");
			List<Handle> handles = handleStore.listHandleValues(handleName.toUpperCase());
			if (!handles.isEmpty()) {        		
				String collectionKey = handles.get(0).getKey();
				if (collectionKey != null) {
					return registry.getProperty(collectionKey, CoreService.WORKSPACE_REGISTRY_PROPERTY_KEY);
				}
			}
		} catch (HandleStoreServiceException | RegistryServiceException | KeyNotFoundException | PropertyNotFoundException | UnsupportedEncodingException | HandleNotFoundException e) {
			LOGGER.log(Level.SEVERE, "unknown context identifier " + handleURL, e);
			throw new SRUException(SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN, "unknown context identifier " + handleURL);
		}
    	return null;
    }

}
