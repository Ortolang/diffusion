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
        if (!identifier.startsWith(DiffusionItemRepository.PREFIX_IDENTIFIER)) {
            throw new IdDoesNotExistException();
        }
        String key = identifier.replaceFirst(DiffusionItemRepository.PREFIX_IDENTIFIER, "");
        List<String> metadataFormat = new ArrayList<String>();
        try {
            String doc = search.getCollection(key);
            if(doc!=null) {
                metadataFormat.add("oai_dc");
                metadataFormat.add("olac");
            }
        } catch (SearchServiceException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        if (metadataFormat.isEmpty())
            throw new NoMetadataFormatsException();
        else
            return metadataFormat;
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
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from)
            throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, from);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from)
            throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, from, null, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec)
            throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec)
            throws OAIException {
        //TODO filter on setSpec
        return getItemIdentifiersFromQuery(metadataPrefix, null, null, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from, Date until)
            throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, from, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from, Date until)
            throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, from, until, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from)
            throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec, from);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from)
            throws OAIException {
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
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, Date until)
            throws OAIException {
        return getItemIdentifiersUntil(filters, "oai_dc", offset, length, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date until)
            throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, null, until, offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Date until)
            throws OAIException {
        return getItemIdentifiersUntil(filters, "oai_dc", offset, length, setSpec, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date until)
            throws OAIException {
        //TODO filter on setSpec
        return getItemIdentifiersFromQuery(metadataPrefix, null, until, offset, length);
    }

    /**
     * Retrieves identifier of records based of a query and a metadata prefix.
     * @param query
     * @param metadataPrefix
     * @param offset
     * @param length
     * @return
     */
    protected ListItemIdentifiersResult getItemIdentifiersFromQuery(String metadataPrefix, Date from, Date until, int offset, int length) {

        List<DiffusionItemIdentifier> list = new ArrayList<>();

        try {
            Map<String, String> fieldsProjection = new HashMap<>();
            fieldsProjection.put("meta_ortolang-workspace-json.wsalias", "wsalias");
            fieldsProjection.put("lastModificationDate", "lastModificationDate");
            Map<String, Object> fieldsMap = new HashMap<String, Object>();
            fieldsMap.put("status", "published");
            fieldsMap.put("meta_ortolang-workspace-json.wsalias", "");
            fieldsMap.put("meta_ortolang-item-json.title", "");
            if(from!=null) {
                fieldsMap.put("lastModificationDate>=", from.getTime());
            }
            if(until!=null) {
                fieldsMap.put("lastModificationDate<=", until.getTime()+86400000);
            }
            List<String> docs = search.findCollections(fieldsProjection, null, null, null, null, null, fieldsMap);
            Map<String, DiffusionItemIdentifier> registerMap = new HashMap<>();
            for(String doc : docs) {
                StringReader reader = new StringReader(doc);
                JsonReader jsonReader = Json.createReader(reader);
                JsonObject jsonObj = jsonReader.readObject();
                try {
                    String key = jsonObj.getString("wsalias");
                    JsonNumber lastModificationDate = jsonObj.getJsonNumber("lastModificationDate");
                    Long longTimestamp = lastModificationDate.longValue();
                    Date datestamp = new Date(longTimestamp);
                    DiffusionItemIdentifier itemIdentifier = new DiffusionItemIdentifier().withIdentifier(PREFIX_IDENTIFIER+key).withDatestamp(datestamp);

                    if(registerMap.containsKey(itemIdentifier.getIdentifier()) && registerMap.get(itemIdentifier.getIdentifier()).getDatestamp().before(itemIdentifier.getDatestamp())) {
                        list.set(list.indexOf(registerMap.get(itemIdentifier.getIdentifier())), itemIdentifier);
                    } else {
                        list.add(itemIdentifier);
                    }
                    registerMap.put(itemIdentifier.getIdentifier(), itemIdentifier);
                } catch(NullPointerException | ClassCastException | NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "No property 'key' or lastModificationDate in json object", e);
                } finally {
                    jsonReader.close();
                    reader.close();
                }
            }
        } catch (SearchServiceException e) {
            LOGGER.log(Level.SEVERE, "Unable to get item identifiers with metadataPrefix : "+metadataPrefix+" and from "+from+" until "+until);
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
        try {
            HashMap<String, String> fieldsProjection = new HashMap<String, String>();
            HashMap<String, Object> fieldsMap = new HashMap<String, Object>();
            fieldsMap.put("status", "published");
            fieldsMap.put("meta_ortolang-workspace-json.wsalias", key);

            List<String> docs = search.findCollections(fieldsProjection, null, null, null, null, null, fieldsMap);

            DiffusionItem lastItem = null;
            for(String doc : docs) {
                DiffusionItem item = diffusionItem(doc, metadataPrefix);
                if(lastItem==null) {
                    lastItem = item;
                } else if (lastItem.getDatestamp().before(item.getDatestamp())) {
                    lastItem = item;
                }
            }
            if(lastItem!=null) {
                return lastItem;
            } else {
                throw new OAIException("There is no record for this identifier "+identifier);
            }
        } catch (SearchServiceException | IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to get item with key : "+identifier, e);
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
        return getItemsFromQuery(metadataPrefix, null, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
            int length, Date from) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, from);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
            int length, Date from) throws OAIException {
        return getItemsFromQuery(metadataPrefix, from, null, offset, length);
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
        return getItemsFromQuery(metadataPrefix, null, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset,
            int length, Date from, Date until) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, from, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
            int length, Date from, Date until) throws OAIException {
        return getItemsFromQuery(metadataPrefix, from, until, offset, length);
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
        return getItemsFromQuery(metadataPrefix, from , null, offset, length);
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
        return getItemsFromQuery(metadataPrefix,from, until, offset, length);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset,
            int length, Date until) throws OAIException {
        return getItemsUntil(filters, "oai_dc", offset, length, until);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, String metadataPrefix, int offset,
            int length, Date until) throws OAIException {
        return getItemsFromQuery(metadataPrefix, null, until, offset, length);
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
        return getItemsFromQuery(metadataPrefix, null, until, offset, length);
    }

    public ListItemsResults getItemsFromQuery(String metadataPrefix, Date from, Date until, int offset, int length) {
        List<DiffusionItem> list = new ArrayList<>();
        try {
            Map<String, String> fieldsProjection = new HashMap<>();
            Map<String, Object> fieldsMap = new HashMap<>();
            fieldsMap.put("status", "published");
            fieldsMap.put("meta_ortolang-workspace-json.wsalias", "");
            fieldsMap.put("meta_ortolang-item-json.title", "");
            if(from!=null) {
                fieldsMap.put("lastModificationDate>=", from.getTime());
            }
            if(until!=null) {
                fieldsMap.put("lastModificationDate<=", until.getTime()+86400000);
            }
            List<String> docs = search.findCollections(fieldsProjection, null, null, null, null, null, fieldsMap);

            if(docs.size()>0) {
                Map<String, DiffusionItem> registerMap = new HashMap<>();

                for(String doc : docs) {
                    DiffusionItem item = diffusionItem(doc, metadataPrefix);
                    if(item!=null) {
                        if(registerMap.containsKey(item.getIdentifier()) && registerMap.get(item.getIdentifier()).getDatestamp().before(item.getDatestamp())) {
                            list.set(list.indexOf(registerMap.get(item.getIdentifier())), item);
                        } else {
                            list.add(item);
                        }
                        registerMap.put(item.getIdentifier(), item);
                    }
                }
            }
        } catch (SearchServiceException | IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to get item with metadataPrefix : "+metadataPrefix+" and from "+from+" until "+until, e);
        }
        return new ListItemsResults(offset + length < list.size(), new ArrayList<Item>(list.subList(offset, min(offset + length, list.size()))));
    }

    protected DiffusionItem diffusionItem(String doc, String metadataPrefix) throws IOException {
        DiffusionItem item = null;
        StringReader reader = new StringReader(doc);
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject jsonDoc = jsonReader.readObject();
        try {
            JsonObject content = jsonDoc.getJsonObject("meta_ortolang-workspace-json");
            String wsalias = content.getString("wsalias");
            JsonNumber lastModificationDate = jsonDoc.getJsonNumber("lastModificationDate");
            Long longTimestamp = lastModificationDate.longValue();
            Date datestamp = new Date(longTimestamp);

            JsonObject workspaceDoc = searchWorkspace(wsalias);

            InputStream metadata = null;
            if ("oai_dc".equals(metadataPrefix)) {
                metadata = transformToOAI_DC(jsonDoc, workspaceDoc);
            } else if("olac".equals(metadataPrefix)) {
                metadata = transformToOLAC(jsonDoc, workspaceDoc);
            }

            if(metadata!=null) {
                item = DiffusionItem.item();
                item.withIdentifier(PREFIX_IDENTIFIER+wsalias);
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

    protected JsonObject searchWorkspace(String wsalias) {
        try {
            List<String> workspace = search.jsonSearch("SELECT FROM Workspace WHERE `meta_ortolang-workspace-json.wsalias` = '"+wsalias+"'");
            if(workspace.size()>0) {

                StringReader reader = new StringReader(workspace.get(0));
                JsonReader jsonReader = Json.createReader(reader);
                try {

                    JsonObject jsonDoc = jsonReader.readObject();
                    jsonReader.close();

                    return jsonDoc;
                } catch(Exception e) {
                    LOGGER.log(Level.WARNING, "Cannot read json object representation for the workspace "+wsalias, e);
                } finally {
                    reader.close();
                }
            }
        } catch (SearchServiceException e) {
            LOGGER.log(Level.WARNING, "Unable to get workspace with wsalias : "+wsalias, e);
        }
        return null;
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
    protected InputStream transformToOAI_DC(JsonObject jsonDoc, JsonObject workspaceDoc) {
        OAI_DC oai_dc = OAI_DC.valueOf(jsonDoc, workspaceDoc);
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
    protected InputStream transformToOLAC(JsonObject jsonDoc, JsonObject workspaceDoc) {
        OLAC olac = OLAC.valueOf(jsonDoc, workspaceDoc);
        return new ByteArrayInputStream(olac.toString().getBytes(StandardCharsets.UTF_8));
    }
}
