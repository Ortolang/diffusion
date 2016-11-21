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
import java.util.*;
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

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.api.oaipmh.format.OAI_DC;
import fr.ortolang.diffusion.api.oaipmh.format.OLAC;
import fr.ortolang.diffusion.oai.OaiService;
import fr.ortolang.diffusion.oai.RecordNotFoundException;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.search.SearchServiceException;
import fr.ortolang.diffusion.store.handle.HandleStoreService;
import fr.ortolang.diffusion.store.handle.HandleStoreServiceException;

public class DiffusionItemRepository implements MultiMetadataItemRepository {

    private static final Logger LOGGER = Logger.getLogger(DiffusionItemRepository.class.getName());

    public static final String PREFIX_IDENTIFIER = "oai:ortolang.fr:";

    public static String identifier(String semanticUri) {
        return semanticUri.substring(semanticUri.lastIndexOf("/") + 1);
    }

    private OaiService oaiService;
    private HandleStoreService handleStore;

    public DiffusionItemRepository(OaiService oaiService, HandleStoreService handleStore) {
        this.oaiService = oaiService;
        this.handleStore = handleStore;
    }

    /**
     * Response to a ListMetadataFormat request.
     * @param identifier
     * @return a list of metadata format
     */
    @Override
    public List<String> getListMetadataFormats(String identifier) throws IdDoesNotExistException, NoMetadataFormatsException, OAIException {
        if (!identifier.startsWith(DiffusionItemRepository.PREFIX_IDENTIFIER)) {
            throw new IdDoesNotExistException();
        }
        
        String key = identifier.replaceFirst(DiffusionItemRepository.PREFIX_IDENTIFIER, "");
        List<String> metadataFormat = new ArrayList<String>();
        try {
			Record record = oaiService.readRecord(key);
			metadataFormat.add("oai_dc");
			metadataFormat.add("olac");
		} catch (RecordNotFoundException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
        if (metadataFormat.isEmpty())
            throw new NoMetadataFormatsException();
        else
            return metadataFormat;
    }

    /**
     * Response to a ListIdentifiers request.
     * @param metadataPrefix
     * @param from
     * @param until
     * @param offset
     * @param length
     * @return a list of identifiers
     */
    protected ListItemIdentifiersResult getItemIdentifiersFromQuery(String metadataPrefix, Date from, Date until, int offset, int length) {

        List<DiffusionItemIdentifier> list = new ArrayList<>();

//        try {
//        	StringBuilder query = new StringBuilder("select expand($all) let $item = (SELECT `lastModificationDate` AS lastModificationDate ,`meta_ortolang-workspace-json.wsalias` AS wsalias ,`key` AS key FROM Collection WHERE `meta_ortolang-item-json` IS NOT NULL ");
//			if (from != null) {
//				query.append(" AND lastModificationDate>=").append(from.getTime());
//			}
//			if (until != null) {
//				query.append(" AND lastModificationDate<=").append(until.getTime() + 86400000);
//			}
//        	query.append(" ), $collection = (SELECT `lastModificationDate` AS lastModificationDate ,`meta_ortolang-workspace-json.wsalias` AS wsalias ,`key` AS key FROM Collection WHERE `meta_")
//        		.append(metadataPrefix).append("` IS NOT NULL");
//        	if (from != null) {
//				query.append(" AND lastModificationDate>=").append(from.getTime());
//			}
//			if (until != null) {
//				query.append(" AND lastModificationDate<=").append(until.getTime() + 86400000);
//			}
//			query.append(" ), $dataobject = (SELECT `lastModificationDate` AS lastModificationDate ,`meta_ortolang-workspace-json.wsalias` AS wsalias ,`key` AS key FROM Object WHERE `meta_")
//				.append(metadataPrefix).append("` IS NOT NULL");
//        	if (from != null) {
//				query.append(" AND lastModificationDate>=").append(from.getTime());
//			}
//			if (until != null) {
//				query.append(" AND lastModificationDate<=").append(until.getTime() + 86400000);
//			}
//        	query.append(" ), $all = UNIONALL($item, $collection, $dataobject)");
//        	List<String> docs = search.jsonSearch(query.toString());
//            Map<String, DiffusionItemIdentifier> registerMap = new HashMap<>();
//            for (String doc : docs) {
//                StringReader reader = new StringReader(doc);
//                JsonReader jsonReader = Json.createReader(reader);
//                JsonObject jsonObj = jsonReader.readObject();
//                try {
//                    String key = jsonObj.containsKey("wsalias") ? jsonObj.getString("wsalias") : jsonObj.getString("key");
//                    JsonNumber lastModificationDate = jsonObj.getJsonNumber("lastModificationDate");
//                    Long longTimestamp = lastModificationDate.longValue();
//                    Date datestamp = new Date(longTimestamp);
//                    DiffusionItemIdentifier itemIdentifier = new DiffusionItemIdentifier().withIdentifier(PREFIX_IDENTIFIER + key).withDatestamp(datestamp);
//
//                    if (registerMap.containsKey(itemIdentifier.getIdentifier()) && registerMap.get(itemIdentifier.getIdentifier()).getDatestamp().before(itemIdentifier.getDatestamp())) {
//                        list.set(list.indexOf(registerMap.get(itemIdentifier.getIdentifier())), itemIdentifier);
//                    } else {
//                        list.add(itemIdentifier);
//                    }
//                    registerMap.put(itemIdentifier.getIdentifier(), itemIdentifier);
//                } catch (NullPointerException | ClassCastException | NumberFormatException e) {
//                    LOGGER.log(Level.WARNING, "No property 'key' or lastModificationDate in json object", e);
//                } finally {
//                    jsonReader.close();
//                    reader.close();
//                }
//            }
//        } catch (SearchServiceException e) {
//            LOGGER.log(Level.SEVERE, "Unable to get item identifiers with metadataPrefix : " + metadataPrefix + " and from " + from + " until " + until);
//        }
        return new ListItemIdentifiersResult(offset + length < list.size(), new ArrayList<ItemIdentifier>(list.subList(offset, min(offset + length, list.size()))), list.size());
    }

    /**
     * Response to a GetRecord request.
     * @param identifier
     * @param metadataPrefix
     * @return a record
     */
    @Override
    public Item getItem(String identifier, String metadataPrefix) throws IdDoesNotExistException, OAIException {
        if (!identifier.startsWith(DiffusionItemRepository.PREFIX_IDENTIFIER))
            throw new IdDoesNotExistException();
//        String key = identifier.replaceFirst(DiffusionItemRepository.PREFIX_IDENTIFIER, "");
//        try {
//        	StringBuilder query = new StringBuilder("select expand($all) let $item = (SELECT FROM Collection WHERE `meta_ortolang-item-json` IS NOT NULL ");
//			query.append(" AND `meta_ortolang-workspace-json.wsalias`='").append(key).append("'");
//        	query.append(" ), $collection = (SELECT FROM Collection WHERE `meta_").append(metadataPrefix).append("` IS NOT NULL");
//        	query.append(" AND key='").append(key).append("'");
//        	query.append("), $object = (SELECT FROM Object WHERE `meta_").append(metadataPrefix).append("` IS NOT NULL");
//        	query.append(" AND key='").append(key).append("'");
//        	query.append(" ), $all = UNIONALL($item, $collection, $object)");
//        	List<String> docs = search.jsonSearch(query.toString());
//        	
//            DiffusionItem lastItem = null;
//            for (String doc : docs) {
//                DiffusionItem item = diffusionItem(doc, metadataPrefix);
//                if (lastItem == null) {
//                    lastItem = item;
//                } else if (lastItem.getDatestamp().before(item.getDatestamp())) {
//                    lastItem = item;
//                }
//            }
//            if (lastItem != null) {
//                return lastItem;
//            } else {
//                throw new OAIException("There is no record for this identifier " + identifier);
//            }
//        } catch (SearchServiceException | IOException e) {
//            LOGGER.log(Level.SEVERE, "Unable to get item with key : " + identifier, e);
//        }
        throw new IdDoesNotExistException();
    }

    /**
     * Response to a ListRecords request.
     * @param metadataPrefix the metadataPrefix asked 
     * @param from  
     * @param until
     * @param offset 
     * @param length
     * @return a list of records
     */
    public ListItemsResults getItemsFromQuery(String metadataPrefix, Date from, Date until, int offset, int length) {
        List<DiffusionItem> list = new ArrayList<>();
//        try {
//        	StringBuilder query = new StringBuilder("select expand($all) let $item = (SELECT FROM Collection WHERE `meta_ortolang-item-json` IS NOT NULL ");
//			if (from != null) {
//				query.append(" AND lastModificationDate>=").append(from.getTime());
//			}
//			if (until != null) {
//				query.append(" AND lastModificationDate<=").append(until.getTime() + 86400000);
//			}
//        	query.append(" ), $collection = (SELECT FROM Collection WHERE `meta_").append(metadataPrefix).append("` IS NOT NULL");
//        	if (from != null) {
//				query.append(" AND lastModificationDate>=").append(from.getTime());
//			}
//			if (until != null) {
//				query.append(" AND lastModificationDate<=").append(until.getTime() + 86400000);
//			}
//			query.append(" ), $object = (SELECT FROM Object WHERE `meta_").append(metadataPrefix).append("` IS NOT NULL");
//			if (from != null) {
//				query.append(" AND lastModificationDate>=").append(from.getTime());
//			}
//			if (until != null) {
//				query.append(" AND lastModificationDate<=").append(until.getTime() + 86400000);
//			}
//        	query.append(" ), $all = UNIONALL($item, $collection, $object)");
//        	List<String> docs = search.jsonSearch(query.toString());
//            if (!docs.isEmpty()) {
//                Map<String, DiffusionItem> registerMap = new HashMap<>();
//
//                for (String doc : docs) {
//                    DiffusionItem item = diffusionItem(doc, metadataPrefix);
//                    if (item != null) {
//                        if (registerMap.containsKey(item.getIdentifier()) && registerMap.get(item.getIdentifier()).getDatestamp().before(item.getDatestamp())) {
//                            list.set(list.indexOf(registerMap.get(item.getIdentifier())), item);
//                        } else {
//                            list.add(item);
//                        }
//                        registerMap.put(item.getIdentifier(), item);
//                    }
//                }
//            }
//        } catch (SearchServiceException | IOException e) {
//            LOGGER.log(Level.SEVERE, "Unable to get item with metadataPrefix : " + metadataPrefix + " and from " + from + " until " + until, e);
//        }
        return new ListItemsResults(offset + length < list.size(), new ArrayList<Item>(list.subList(offset, min(offset + length, list.size()))), list.size());
    }

    /**
     * Creates a record answering to GetRecord or ListRecords request.
     * @param doc a JSON document containing all informations about the record
     * @param metadataPrefix specify the metadataPrefix value
     * @return a record
     * @throws IOException
     */
    protected DiffusionItem diffusionItem(String doc, String metadataPrefix) throws IOException {
        DiffusionItem item = null;
        StringReader reader = new StringReader(doc);
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject jsonDoc = jsonReader.readObject();
        try {
        	String key = (jsonDoc.containsKey("meta_ortolang-workspace-json")) ? 
        			jsonDoc.getJsonObject("meta_ortolang-workspace-json").getString("wsalias") :
        			jsonDoc.getString("key");
        	JsonNumber lastModificationDate = jsonDoc.getJsonNumber("lastModificationDate");
        	Long longTimestamp = lastModificationDate.longValue();
        	Date datestamp = new Date(longTimestamp);
        	InputStream metadata = null;
        	
        	if ("oai_dc".equals(metadataPrefix)) {
        		metadata = transformToOaiDC(jsonDoc);
        	} else if ("olac".equals(metadataPrefix)) {
        		metadata = transformToOLAC(jsonDoc);
        	}

            if (metadata != null) {
                item = DiffusionItem.item();
                item.withIdentifier(PREFIX_IDENTIFIER + key);
                item.withDatestamp(datestamp);
                item.withMetadata(metadata);
            }

        } catch (NullPointerException | ClassCastException | NumberFormatException e) {
            LOGGER.log(Level.WARNING, "No property 'key' or lastModificationDate in json object", e);
        } finally {
            jsonReader.close();
            reader.close();
        }

        return item;
    }

    /**
     * Converts JSON document (String representation) to XML OAI_DC
     *
     */
    protected InputStream transformToOaiDC(JsonObject jsonDoc) {
        OAI_DC oaiDc = OAI_DC.valueOf(jsonDoc);
        List<String> handles;
		try {
			handles = handleStore.listHandlesForKey(jsonDoc.getString("key"));
			for(String handle : handles) {        	
				oaiDc.addDcField("identifier", 
						"http://hdl.handle.net/"+OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX)
		                + "/" +handle);
			}
		} catch (NullPointerException | ClassCastException | HandleStoreServiceException e) {
			LOGGER.log(Level.WARNING, "No handle for key " + jsonDoc.getString("key"), e);
		}
        
        return new ByteArrayInputStream(oaiDc.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts JSON document (String representation) to XML OLAC
     *
     */
    protected InputStream transformToOLAC(JsonObject jsonDoc) {
        OLAC olac = OLAC.valueOf(jsonDoc);
        return new ByteArrayInputStream(olac.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, null, null, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, from);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, from, null, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec) throws OAIException {
        //TODO filter on setSpec
        return getItemIdentifiersFromQuery(metadataPrefix, null, null, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from, Date until) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, from, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from, Date until) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, from, until, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec, from);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from) throws OAIException {
        //TODO filter on setSpec
        return getItemIdentifiersFromQuery(metadataPrefix, from, null, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec, from, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        //TODO filter on setSpec
        return getItemIdentifiersFromQuery(metadataPrefix, from, until, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, Date until) throws OAIException {
        return getItemIdentifiersUntil(filters, "oai_dc", offset, length, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date until) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, null, until, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Date until) throws OAIException {
        return getItemIdentifiersUntil(filters, "oai_dc", offset, length, setSpec, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date until) throws OAIException {
        //TODO filter on setSpec
        return getItemIdentifiersFromQuery(metadataPrefix, null, until, offset, length);
    }

    @Override
    public Item getItem(String identifier) throws IdDoesNotExistException, OAIException {
        return getItem(identifier, "oai_dc");
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length) throws OAIException {
        return getItems(filters, "oai_dc", offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length) throws OAIException {
        return getItemsFromQuery(metadataPrefix, null, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Date from) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, from);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from) throws OAIException {
        return getItemsFromQuery(metadataPrefix, from, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, setSpec);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec) throws OAIException {
        //TODO filter by spec
        return getItemsFromQuery(metadataPrefix, null, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Date from, Date until) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, from, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from, Date until) throws OAIException {
        return getItemsFromQuery(metadataPrefix, from, until, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, setSpec, from);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from) throws OAIException {
        // TODO Filter by set
        return getItemsFromQuery(metadataPrefix, from, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, setSpec, from, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        // TODO Filter by set
        return getItemsFromQuery(metadataPrefix, from, until, offset, length);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, Date until) throws OAIException {
        return getItemsUntil(filters, "oai_dc", offset, length, until);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date until) throws OAIException {
        return getItemsFromQuery(metadataPrefix, null, until, offset, length);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Date until) throws OAIException {
        return getItemsUntil(filters, "oai_dc", offset, length, setSpec, until);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date until) throws OAIException {
        // TODO Filter by set
        return getItemsFromQuery(metadataPrefix, null, until, offset, length);
    }
}
