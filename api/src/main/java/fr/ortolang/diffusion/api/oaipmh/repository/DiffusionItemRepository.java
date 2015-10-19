package fr.ortolang.diffusion.api.oaipmh.repository;

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

import static java.lang.Math.min;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.ItemIdentifier;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.api.oaipmh.format.OAI_DC;
import fr.ortolang.diffusion.api.oaipmh.format.OLAC;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.search.SearchServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

public class DiffusionItemRepository implements MultiMetadataItemRepository {

	private static final Logger LOGGER = Logger.getLogger(DiffusionItemRepository.class.getName());
	
	public static final String PREFIX_IDENTIFIER = "oai:ortolang.fr:";

	public static String identifier(String semanticUri) {
		return semanticUri.substring(semanticUri.lastIndexOf("/")+1);
	}

	private SearchService search;
	
	public DiffusionItemRepository(SearchService search) {
		this.search = search;
	}


	@Override
    public List<String> getListMetadataFormats(String identifier) throws IdDoesNotExistException, NoMetadataFormatsException, OAIException {

		if(!identifier.startsWith(DiffusionItemRepository.PREFIX_IDENTIFIER))
			throw new IdDoesNotExistException();

		String key = identifier.replaceFirst(DiffusionItemRepository.PREFIX_IDENTIFIER, "");
		List<String> metadataFormat = new ArrayList<String>();
		
		String query = "SELECT meta_ortolang-workspace-json.wsalias as wsalias FROM Collection WHERE status='published' AND meta_ortolang-workspace-json.wsalias='"+key+"'";
		try {
			List<String> docs = search.jsonSearch(query);
			
			if(!docs.isEmpty()) {
				metadataFormat.add("oai_dc");
			}
			
		} catch (SearchServiceException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		
		if(metadataFormat.isEmpty())
			throw new NoMetadataFormatsException();
		else
			return metadataFormat;
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
		String query = buildQuery(metadataPrefix, from, until);
		
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
		String query = buildQuery(metadataPrefix, from, until);
		
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
		String query = buildQuery(metadataPrefix, null, until);
		
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
		String query = buildQuery(metadataPrefix, null, until);
		
		return getItemIdentifiersFromQuery(query, metadataPrefix, offset, length);
	}

	protected String query(String metadataPrefix) {
		return buildQuery(metadataPrefix, null, null);
	}
	
	protected String query(String metadataPrefix, Date from) {
		return buildQuery(metadataPrefix, from, null);
	}

    protected String buildQuery(String metadataPrefix, Date from, Date until) {
        return buildQuery(metadataPrefix, from, until, null);
    }
	/**
	 * Builds a query allowing to retrieve identifier of records based of some criteria. 
	 * @param metadataPrefix
	 * @param from
	 * @param until
	 * @return
	 */
	protected String buildQuery(String metadataPrefix, Date from, Date until, String key) {

		StringBuilder query = new StringBuilder("SELECT lastModificationDate, meta_ortolang-workspace-json.*, meta_ortolang-item-json.* FROM Collection WHERE status='published' AND meta_ortolang-workspace-json.wsalias IS NOT null");
		
		if(from!=null) {
		    query.append(" AND lastModificationDate>=").append(from.getTime());
		}
		
		if(until!=null) {
		    query.append(" AND lastModificationDate<=").append(until.getTime());
		}
		
		if(key!=null) {
		    query.append(" AND meta_ortolang-workspace-json.wsalias='").append(key).append("'");
		}
		
		return query.toString();
	}

	/**
	 * Retrieves identifier of records based of a query and a metadata prefix.
	 * @param query
	 * @param metadataPrefix
	 * @param offset
	 * @param length
	 * @return
	 */
	protected ListItemIdentifiersResult getItemIdentifiersFromQuery(String query, String metadataPrefix, int offset, int length) {

		List<DiffusionItemIdentifier> list = new ArrayList<DiffusionItemIdentifier>();
		
		try {
			List<String> docs = search.jsonSearch(query);
			
			for(String doc : docs) {
				StringReader reader = new StringReader(doc);
				JsonReader jsonReader = Json.createReader(reader);
				JsonObject jsonObj = jsonReader.readObject();
				
				try {
					String key = jsonObj.getString("meta_ortolang-workspace-jsonwsalias");
					JsonNumber lastModificationDate = jsonObj.getJsonNumber("lastModificationDate");
					Long longTimestamp = Long.valueOf(lastModificationDate.longValue());
					Date datestamp = new Date(longTimestamp);
					
					list.add(new DiffusionItemIdentifier().withIdentifier(PREFIX_IDENTIFIER+key).withDatestamp(datestamp));
				} catch(NullPointerException | ClassCastException | NumberFormatException e) {
					LOGGER.log(Level.WARNING, "No property 'key' or lastModificationDate in json object", e);
				} finally {
					jsonReader.close();
					reader.close();
				}
			}
		} catch (SearchServiceException e) {
			LOGGER.log(Level.SEVERE, "Unable to get item identifiers with query : "+query);
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
		
		String query = buildQuery(metadataPrefix, null, null, key);
		
		try {
		    List<String> docs = search.jsonSearch(query);
		    
		    if(docs.size()==1) {
		        DiffusionItem item = diffusionItem(docs.get(0), metadataPrefix);
		        
		        if(item!=null) {
		            return item;
		        } else {
		            throw new OAIException();
		        }
		    } else if(docs.size()>1) {
		        LOGGER.log(Level.WARNING, "Too many item for identifier "+identifier);
		    }
		} catch (SearchServiceException | IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to get item with query : "+query, e);
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

		String query = buildQuery(metadataPrefix, from, until);
		
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
		String query = buildQuery(metadataPrefix, from, until);
		
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
		String query = buildQuery(metadataPrefix, null, until);
		
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
		String query = buildQuery(metadataPrefix, null, until);
		
		return getItemsFromQuery(query, metadataPrefix, offset, length);
	}
	
	public ListItemsResults getItemsFromQuery(String query, String metadataPrefix, int offset, int length) {

		List<DiffusionItem> list = new ArrayList<DiffusionItem>();
		
        try {
            List<String> docs = search.jsonSearch(query.toString());
            
            if(docs.size()>0) {
                
                for(String doc : docs) {
                    DiffusionItem item = diffusionItem(doc, metadataPrefix);
                    
                    if(item!=null) {
                        list.add(item);
                    }
                }
            }
        } catch (SearchServiceException | IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to get item with query : "+query, e);
        }
		
		return new ListItemsResults(offset + length < list.size(), new ArrayList<Item>(list.subList(offset, min(offset + length, list.size()))));
	}

    protected DiffusionItem diffusionItem(String doc, String metadataPrefix) throws IOException {
        DiffusionItem item = null;

        //TODO get handle (or another persistent identifier)
        StringReader reader = new StringReader(doc);
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject jsonDoc = jsonReader.readObject();
        
        try {
            String key = jsonDoc.getString("meta_ortolang-workspace-jsonwsalias");
            JsonNumber lastModificationDate = jsonDoc.getJsonNumber("lastModificationDate");
            Long longTimestamp = Long.valueOf(lastModificationDate.longValue());
            Date datestamp = new Date(longTimestamp);
            
            InputStream metadata = null;
            if(metadataPrefix.equals("oai_dc")) {
                metadata = transformToOAI_DC(jsonDoc);
            } else if(metadataPrefix.equals("olac")) {
            	metadata = transformToOLAC(jsonDoc);
            }
            
            if(metadata!=null) {
                item = DiffusionItem.item();
                item.withIdentifier(PREFIX_IDENTIFIER+key);
                item.withDatestamp(datestamp);
                item.withMetadata(metadata);
            }
            
        } catch(NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "No property 'key' or lastModificationDate in json object", e);
        } finally {
            jsonReader.close();
            reader.close();
        }
        
        return item;
    }

    /**
     * Converts JSON document (String representation) to XML OAI_DC
     * @param document
     * @return
     * @throws OrtolangException
     * @throws KeyNotFoundException
     * @throws CoreServiceException
     * @throws DataNotFoundException
     * @throws IOException
     */
    protected InputStream transformToOAI_DC(JsonObject jsonDoc) {
        OAI_DC oai_dc = OAI_DC.valueOf(jsonDoc);
        return new ByteArrayInputStream(oai_dc.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts JSON document (String representation) to XML OLAC
     * @param document
     * @return
     * @throws OrtolangException
     * @throws KeyNotFoundException
     * @throws CoreServiceException
     * @throws DataNotFoundException
     * @throws IOException
     */
    protected InputStream transformToOLAC(JsonObject jsonDoc) {
        OLAC olac = OLAC.valueOf(jsonDoc);
        return new ByteArrayInputStream(olac.toString().getBytes(StandardCharsets.UTF_8));
    }
}
