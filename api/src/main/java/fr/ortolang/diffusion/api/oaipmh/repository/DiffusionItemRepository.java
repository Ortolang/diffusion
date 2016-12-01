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

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.ItemIdentifier;

import fr.ortolang.diffusion.oai.OaiService;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.exception.OaiServiceException;
import fr.ortolang.diffusion.oai.exception.RecordNotFoundException;

public class DiffusionItemRepository implements MultiMetadataItemRepository {

    private static final Logger LOGGER = Logger.getLogger(DiffusionItemRepository.class.getName());

    public static final String PREFIX_IDENTIFIER = "oai:ortolang.fr:";

    public static String identifier(String semanticUri) {
        return semanticUri.substring(semanticUri.lastIndexOf("/") + 1);
    }

    private OaiService oaiService;

    public DiffusionItemRepository(OaiService oaiService) {
        this.oaiService = oaiService;
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
        
        identifier = identifier.replaceFirst(DiffusionItemRepository.PREFIX_IDENTIFIER, "");
        List<String> metadataFormat = new ArrayList<String>();
        try {
			List<Record> records = oaiService.listRecordsByIdentifier(identifier);
			for(Record r : records) {
			    metadataFormat.add(r.getMetadataPrefix());
			}
		} catch (RecordNotFoundException e) {
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
    protected ListItemIdentifiersResult getItemIdentifiersFromQuery(String metadataPrefix, Date from, Date until, int offset, int length, String setSpec) {
        List<DiffusionItemIdentifier> list = new ArrayList<>();
        Long fromTime = null;
        Long untilTime = null;
        try {
            if (from != null)
                fromTime = from.getTime();
            if (until != null)
                untilTime = until.getTime() + 86400000;
            List<Record> records = oaiService.listRecordsByMetadataPrefixAndSetspec(metadataPrefix, setSpec, fromTime, untilTime);
            for(Record rec : records) {
                list.add(DiffusionItemIdentifier.fromRecord(rec));
            }
        } catch (RecordNotFoundException e) {
        } catch (OaiServiceException e) {
            LOGGER.log(Level.SEVERE, "Unable to list identifiers with metadataPrefix : " + metadataPrefix + " and from " + from + " until " + until, e);
        }
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
        identifier = identifier.replaceFirst(DiffusionItemRepository.PREFIX_IDENTIFIER, "");
        try {
            return DiffusionItem.fromRecord(oaiService.findRecord(identifier, metadataPrefix));
        } catch (RecordNotFoundException | IOException e) {
            throw new IdDoesNotExistException();
        }
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
    public ListItemsResults getItemsFromQuery(String metadataPrefix, String setSpec, Date from, Date until, int offset, int length) {
        List<DiffusionItem> list = new ArrayList<>();
        Long fromTime = null;
        Long untilTime = null;
        try {
            if (from != null)
                fromTime = from.getTime();
            if (until != null)
                untilTime = until.getTime() + 86400000;
            List<Record> records = oaiService.listRecordsByMetadataPrefixAndSetspec(metadataPrefix, setSpec, fromTime, untilTime);
            for(Record rec : records) {
                list.add(DiffusionItem.fromRecord(rec));
            }
        } catch (RecordNotFoundException e) {
        } catch (IOException | OaiServiceException e) {
            LOGGER.log(Level.SEVERE, "Unable to list records with metadataPrefix : " + metadataPrefix + " and from " + from + " until " + until, e);
        }
        return new ListItemsResults(offset + length < list.size(), new ArrayList<Item>(list.subList(offset, min(offset + length, list.size()))), list.size());
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, null, null, offset, length, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, from);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, from, null, offset, length, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, null, null, offset, length, setSpec);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, Date from, Date until) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, from, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from, Date until) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, from, until, offset, length, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec, from);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, from, null, offset, length, setSpec);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        return getItemIdentifiers(filters, "oai_dc", offset, length, setSpec, from, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, from, until, offset, length, setSpec);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, Date until) throws OAIException {
        return getItemIdentifiersUntil(filters, "oai_dc", offset, length, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date until) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, null, until, offset, length, null);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Date until) throws OAIException {
        return getItemIdentifiersUntil(filters, "oai_dc", offset, length, setSpec, until);
    }

    @Override
    public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date until) throws OAIException {
        return getItemIdentifiersFromQuery(metadataPrefix, null, until, offset, length, setSpec);
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
        return getItemsFromQuery(metadataPrefix, null, null, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Date from) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, from);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from) throws OAIException {
        return getItemsFromQuery(metadataPrefix, null, from, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, setSpec);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec) throws OAIException {
        return getItemsFromQuery(metadataPrefix, setSpec, null, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, Date from, Date until) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, from, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from, Date until) throws OAIException {
        return getItemsFromQuery(metadataPrefix, null, from, until, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, setSpec, from);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from) throws OAIException {
        return getItemsFromQuery(metadataPrefix, setSpec, from, null, offset, length);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        return getItems(filters, "oai_dc", offset, length, setSpec, from, until);
    }

    @Override
    public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from, Date until) throws OAIException {
        return getItemsFromQuery(metadataPrefix, setSpec, from, until, offset, length);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, Date until) throws OAIException {
        return getItemsUntil(filters, "oai_dc", offset, length, until);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date until) throws OAIException {
        return getItemsFromQuery(metadataPrefix, null, null, until, offset, length);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, int offset, int length, String setSpec, Date until) throws OAIException {
        return getItemsUntil(filters, "oai_dc", offset, length, setSpec, until);
    }

    @Override
    public ListItemsResults getItemsUntil(List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date until) throws OAIException {
        return getItemsFromQuery(metadataPrefix, setSpec, null, until, offset, length);
    }
}
