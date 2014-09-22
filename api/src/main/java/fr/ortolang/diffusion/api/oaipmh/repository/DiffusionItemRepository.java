package fr.ortolang.diffusion.api.oaipmh.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.ItemIdentifier;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.search.SearchServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.triple.TripleStoreServiceException;
import fr.ortolang.diffusion.store.triple.URIHelper;
import static java.lang.Math.min;

public class DiffusionItemRepository implements MultiMetadataItemRepository {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	// From fr.ortolang.diffusion.store.triple.TripleStoreStatementBuilder
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
	public static final String PREFIX_IDENTIFIER = "oai:ortolang.fr:";

	public static String identifier(String semanticUri) {
		return semanticUri.substring(semanticUri.lastIndexOf("/")+1);
	}

	private SearchService search;
	private CoreService core;
	
	public DiffusionItemRepository(SearchService search, CoreService core) {
		this.search = search;
		this.core = core;
	}


	@Override
    public List<String> getListMetadataFormats(String identifier) throws IdDoesNotExistException, NoMetadataFormatsException, OAIException {

		if(!identifier.startsWith(DiffusionItemRepository.PREFIX_IDENTIFIER))
			throw new IdDoesNotExistException();
		
		String key = identifier.replaceFirst(DiffusionItemRepository.PREFIX_IDENTIFIER, "");
		String subjectURI = null;
		try {
			subjectURI = URIHelper.fromKey(key);
		} catch (TripleStoreServiceException e1) {
			e1.printStackTrace();
		}
		
		String query = "SELECT ?metadataPrefix WHERE { "
				+"<"+subjectURI+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.ortolang.fr/2014/05/diffusion#Object> "
				+"; <http://www.ortolang.fr/2014/05/diffusion#publicationDate> ?pubDate "
				+"; <http://www.ortolang.fr/2014/05/diffusion#hasMetadata> ?metadata "
				+". ?metadata <http://www.ortolang.fr/2014/05/diffusion#metadataFormat> ?metadataPrefix "
				+"}";
		
		String languageResult = "json";
		try {
			List<String> metadataFormat = new ArrayList<String>();
			logger.log(Level.FINE, "SPARQL query : "+query);
			String semanticResult = search.semanticSearch(query, languageResult);
			logger.log(Level.FINE, "SPARQL Result : "+semanticResult);
			JsonObject jsonObject = Json.createReader(new StringReader(semanticResult)).readObject();
			JsonArray results = jsonObject.getJsonObject("results").getJsonArray("bindings");
			JsonObject result = null;
			
			for(JsonValue value : results) {
				
				if(value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
					result = (JsonObject) value;
					
					String metadataPrefix = result.getJsonObject("metadataPrefix").getString("value");
					metadataFormat.add(metadataPrefix);
				}
			}
			
			return metadataFormat;
		} catch (SearchServiceException e) {
			logger.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
		}
		
		throw new NoMetadataFormatsException();
    }
	

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, int offset, int length) throws OAIException {
		return getItemIdentifiers(filters, "oai_dc", offset, length);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length) throws OAIException {

		String query = query(metadataPrefix);
		
		return getItemIdentifiersFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, int offset, int length, Date from)
			throws OAIException {
		
		return getItemIdentifiers(filters, "oai_dc", offset, length, from);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from)
			throws OAIException {
		String query = query(metadataPrefix, from);
		
		return getItemIdentifiersFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, int offset, int length, String setSpec)
			throws OAIException {
		
		return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec)
			throws OAIException {
		//TODO filter on setSpec
		String query = query(metadataPrefix);
		
		return getItemIdentifiersFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, int offset, int length, Date from, Date until)
			throws OAIException {
		
		return getItemIdentifiers(filters, "oai_dc", offset, length, from, until);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from, Date until)
			throws OAIException {
		String query = query(metadataPrefix, from, until);
		
		return getItemIdentifiersFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, int offset, int length, String setSpec, Date from)
			throws OAIException {
		
		return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec, from);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from)
			throws OAIException {
		//TODO filter on setSpec
		String query = query(metadataPrefix, from);
		
		return getItemIdentifiersFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, int offset, int length, String setSpec,
			Date from, Date until) throws OAIException {
		
		return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec, from, until);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec,
			Date from, Date until) throws OAIException {
		//TODO filter on setSpec
		String query = query(metadataPrefix, from, until);
		
		return getItemIdentifiersFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiersUntil(
			List<ScopedFilter> filters, int offset, int length, Date until)
			throws OAIException {
		
		return getItemIdentifiersUntil(filters, "oai_dc", offset, length, until);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiersUntil(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date until)
			throws OAIException {
		String query = query(metadataPrefix, null, until);
		
		return getItemIdentifiersFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiersUntil(
			List<ScopedFilter> filters, int offset, int length, String setSpec, Date until)
			throws OAIException {
		
		return getItemIdentifiersUntil(filters, "oai_dc", offset, length, setSpec, until);
	}

	@Override
	public ListItemIdentifiersResult getItemIdentifiersUntil(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date until)
			throws OAIException {
		//TODO filter on setSpec
		String query = query(metadataPrefix, null, until);
		
		return getItemIdentifiersFromQuery(query, metadataPrefix, offset, length);
	}

	protected String query(String metadataPrefix) {
		return query(metadataPrefix, null, null);
	}
	
	protected String query(String metadataPrefix, Date from) {
		return query(metadataPrefix, from, null);
	}
	
	protected String query(String metadataPrefix, Date from, Date until) {
		StringBuilder query = new StringBuilder("SELECT ?item ?pubDate ?metadata WHERE {")
			.append(" ?item <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.ortolang.fr/2014/05/diffusion#Object> ")
			.append("; <http://www.ortolang.fr/2014/05/diffusion#publicationDate> ?pubDate ")
			.append("; <http://www.ortolang.fr/2014/05/diffusion#hasMetadata> ?metadata ")
			.append(". ?metadata <http://www.ortolang.fr/2014/05/diffusion#metadataFormat> '").append(metadataPrefix).append("' ");
		
		HashMap<String,StringBuilder> conditions = new HashMap<String, StringBuilder>();
		if(from!=null)
			conditions.put("from", new StringBuilder("(?pubDate >= \"").append(sdf.format(from)).append("\")"));

		if(until!=null)
			conditions.put("until", new StringBuilder("(?pubDate <= \"").append(sdf.format(until)).append("\")"));
		
		if(conditions.size()>0) {
			query.append(". FILTER( ");

			int iCond = 1;
			int condLength = conditions.values().size();
			
			for(StringBuilder sb : conditions.values()) {
				query.append(sb);
				
				if(iCond<condLength)
					query.append(" && ");
				iCond++;
			}
			
			query.append(")");
		}
		
		return query.append("}").toString();
	}
	

	protected ListItemIdentifiersResult getItemIdentifiersFromQuery(String query, String metadataPrefix, int offset, int length) {

		List<DiffusionItemIdentifier> list = new ArrayList<DiffusionItemIdentifier>();
		
		String languageResult = "json";
		try {
			logger.log(Level.FINE, "SPARQL query : "+query);
			String semanticResult = search.semanticSearch(query, languageResult);
			logger.log(Level.FINE, "Result of SPARQL query : "+semanticResult);
			JsonObject jsonObject = Json.createReader(new StringReader(semanticResult)).readObject();
			JsonArray results = jsonObject.getJsonObject("results").getJsonArray("bindings");
			
			for(JsonValue value : results) {
				
				if(value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
					JsonObject result = (JsonObject) value;

					// Gets item identifier
					//TODO get handle (persistent identifier) and the last version of the metadata
					String semanticUri = result.getJsonObject("item").getString("value");
					String itemID = identifier(semanticUri);
					
					// Gets datestamp
					String lastDate = result.getJsonObject("pubDate").getString("value");
					Date datestamp = null;
					try {
						datestamp = sdf.parse(lastDate);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					if(itemID!=null && datestamp!=null) {
						list.add(new DiffusionItemIdentifier().withIdentifier(PREFIX_IDENTIFIER+itemID).withDatestamp(datestamp));
					} else {
						logger.log(Level.SEVERE, "Unable to parse datestamp string : "+datestamp);
					}
				}
			}
		} catch (SearchServiceException e) {
//			e.printStackTrace();
			logger.log(Level.SEVERE, "Unable to search into the triplestore with SPARQL query : "+query);
			logger.log(Level.SEVERE, "Stack traces : ", e.fillInStackTrace());
		}
		
		return new ListItemIdentifiersResult(offset + length < list.size(), new ArrayList<ItemIdentifier>(list.subList(offset, min(offset + length, list.size()))));
	}

	
	@Override
	public Item getItem(String identifier) throws IdDoesNotExistException,
			OAIException {
		return getItem(identifier, "oai_dc");
	}
	

	@Override
	public Item getItem(String identifier, String metadataPrefix) throws IdDoesNotExistException,
			OAIException {

		if(!identifier.startsWith(DiffusionItemRepository.PREFIX_IDENTIFIER))
			throw new IdDoesNotExistException();
		
		String key = identifier.replaceFirst(DiffusionItemRepository.PREFIX_IDENTIFIER, "");
		
		String subjectURI = null;
		try {
			subjectURI = URIHelper.fromKey(key);
		} catch (TripleStoreServiceException e1) {
			logger.log(Level.SEVERE, "Unable to create subject URI with key : "+key);
			logger.log(Level.SEVERE, "Stack traces : ", e1.fillInStackTrace());
			throw new IdDoesNotExistException();
		}
		
		String query = "SELECT ?pubDate ?metadata WHERE { "
				+"<"+subjectURI+"> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.ortolang.fr/2014/05/diffusion#Object> "
				+"; <http://www.ortolang.fr/2014/05/diffusion#publicationDate> ?pubDate "
				+"; <http://www.ortolang.fr/2014/05/diffusion#hasMetadata> ?metadata "
				+". ?metadata <http://www.ortolang.fr/2014/05/diffusion#metadataFormat> '"+metadataPrefix+"' "
				+"}";
		logger.log(Level.FINE, "SPARQL query : "+query);

		String languageResult = "json";
		try {
			logger.log(Level.FINE, "SPARQL query : "+query);
			String semanticResult = search.semanticSearch(query, languageResult);
			logger.log(Level.FINE, "SPARQL Result : "+semanticResult);
			JsonObject jsonObject = Json.createReader(new StringReader(semanticResult)).readObject();
			JsonArray results = jsonObject.getJsonObject("results").getJsonArray("bindings");
			JsonObject result = null;
			
			for(JsonValue value : results) {
				
				if(value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
					result = (JsonObject) value;
				}
			}
			
			if(result!=null) {
				try {
					String lastDate = result.getJsonObject("pubDate").getString("value");
					String metadataUri = result.getJsonObject("metadata").getString("value");

					return diffusionItem(subjectURI, metadataUri, lastDate);
				} catch (OrtolangException | KeyNotFoundException
						| CoreServiceException
						| DataNotFoundException | IOException e) {
					logger.log(Level.SEVERE, "Unable to get metadata of "+key);
					logger.log(Level.SEVERE, "Stack traces : ", e.fillInStackTrace());
					return null;
				}
			}
		} catch (SearchServiceException e) {
			logger.log(Level.SEVERE, "Unable to search to the triplestore with the query  "+query);
			logger.log(Level.SEVERE, "Stack traces : ", e.fillInStackTrace());
			return null;
		}
		
		throw new IdDoesNotExistException();
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length)
			throws OAIException {
		
		return getItems(filters, "oai_dc", offset, length);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length)
			throws OAIException {
		String query = query(metadataPrefix);
		
		return getItemsFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
			int length, Date from) throws OAIException {
		
		return getItems(filters, "oai_dc", offset, length, from);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, Date from) throws OAIException {
		String query = query(metadataPrefix, from);
		
		return getItemsFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
			int length, String setSpec) throws OAIException {
		
		return getItems(filters, "oai_dc", offset, length, setSpec);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, String setSpec) throws OAIException {
		//TODO filter by spec
		String query = query(metadataPrefix);
		
		return getItemsFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
			int length, Date from, Date until) throws OAIException {

		return getItems(filters, "oai_dc", offset, length, from, until);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, Date from, Date until) throws OAIException {

		String query = query(metadataPrefix, from, until);
		
		return getItemsFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
			int length, String setSpec, Date from) throws OAIException {
		
		return getItems(filters, "oai_dc", offset, length, setSpec, from);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, String setSpec, Date from) throws OAIException {
		// TODO Filter by set
		String query = query(metadataPrefix, from);
		
		return getItemsFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
			int length, String setSpec, Date from, Date until) throws OAIException {
		
		return getItems(filters, "oai_dc", offset, length, setSpec, from, until);
	}

	@Override
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, String setSpec, Date from, Date until) throws OAIException {
		// TODO Filter by set
		String query = query(metadataPrefix, from, until);
		
		return getItemsFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset,
			int length, Date until) throws OAIException {
		
		return getItemsUntil(filters, "oai_dc", offset, length, until);
	}

	@Override
	public ListItemsResults getItemsUntil(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, Date until) throws OAIException {
		String query = query(metadataPrefix, null, until);
		
		return getItemsFromQuery(query, metadataPrefix, offset, length);
	}

	@Override
	public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset,
			int length, String setSpec, Date until) throws OAIException {
		
		return getItemsUntil(filters, "oai_dc", offset, length, setSpec, until);
	}

	@Override
	public ListItemsResults getItemsUntil(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, String setSpec, Date until) throws OAIException {
		// TODO Filter by set
		String query = query(metadataPrefix, null, until);
		
		return getItemsFromQuery(query, metadataPrefix, offset, length);
	}
	
	public ListItemsResults getItemsFromQuery(String query, String metadataPrefix, int offset, int length) {

		List<DiffusionItem> list = new ArrayList<DiffusionItem>();
		
		String languageResult = "json";
		try {
			logger.log(Level.FINE, "SPARQL query : "+query);
			String semanticResult = search.semanticSearch(query, languageResult);
			logger.log(Level.FINE, "Result for item OAI : "+semanticResult);
			JsonObject jsonObject = Json.createReader(new StringReader(semanticResult)).readObject();
			JsonArray results = jsonObject.getJsonObject("results").getJsonArray("bindings");
			
			for(JsonValue value : results) {
				
				if(value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
					JsonObject result = (JsonObject) value;

					//TODO get handle (persistent identifier)
					String semanticIdentifier = result.getJsonObject("item").getString("value");
					
					String lastDate = result.getJsonObject("pubDate").getString("value");
					
					String metadataUri = result.getJsonObject("metadata").getString("value");
					
					DiffusionItem item = null;
					try {
						item = diffusionItem(semanticIdentifier, metadataUri, lastDate);
						
					} catch (OrtolangException | KeyNotFoundException
							| CoreServiceException
							| DataNotFoundException | IOException e) {
						logger.log(Level.SEVERE, "Unable to get metadata of "+semanticIdentifier);
						logger.log(Level.SEVERE, "Stack traces : ", e.fillInStackTrace());
					}
					
					if(item!=null) {
						list.add(item);
					}
				}
			}
		} catch (SearchServiceException e) {
			logger.log(Level.SEVERE, "Unable to search into the triplestore with SPARQL query : "+query);
			logger.log(Level.SEVERE, "Stack traces : ", e.fillInStackTrace());
		}
		
		return new ListItemsResults(offset + length < list.size(), new ArrayList<Item>(list.subList(offset, min(offset + length, list.size()))));
	}

	protected DiffusionItem diffusionItem(String semanticUri, String metadataUri, String lastDate) throws OrtolangException, KeyNotFoundException, CoreServiceException, DataNotFoundException, IOException {
		DiffusionItem item = DiffusionItem.item();
		
		//TODO get handle (or another persistent identifier)
		String key = identifier(semanticUri);
		item.withIdentifier(PREFIX_IDENTIFIER+key);

		Date datestamp = null;
		try {
			datestamp = sdf.parse(lastDate);
		} catch (ParseException e) {
			logger.log(Level.SEVERE, "Unable to parse datestamp : "+lastDate);
			logger.log(Level.SEVERE, "Stacktraces : ", e.fillInStackTrace());
		}
		item.withDatestamp(datestamp);
		
		String metadataKey = identifier(metadataUri);
		
		if ( metadataKey !=null ) {
						
			InputStream input = null;
			try {
				input = core.download(metadataKey);
			} catch (AccessDeniedException e) {
				logger.log(Level.SEVERE, "Unable to access to metadata of "+metadataKey);
				logger.log(Level.SEVERE, "Stack traces : ", e.fillInStackTrace());
			}
			
			item.withMetadata(input);
				
		}
		
		return item;
	}
	
}