package fr.ortolang.diffusion.oaipmh.handlers;

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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.lyncode.xoai.dataprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.dataprovider.exceptions.CannotDisseminateFormatException;
import com.lyncode.xoai.dataprovider.exceptions.CannotDisseminateRecordException;
import com.lyncode.xoai.dataprovider.exceptions.DoesNotSupportSetsException;
import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.NoMatchesException;
import com.lyncode.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.handlers.VerbHandler;
import com.lyncode.xoai.dataprovider.handlers.helpers.ItemHelper;
import com.lyncode.xoai.dataprovider.handlers.helpers.ResumptionTokenHelper;
import com.lyncode.xoai.dataprovider.handlers.helpers.SetRepositoryHelper;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.dataprovider.parameters.OAICompiledRequest;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.model.oaipmh.About;
import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.model.oaipmh.ListRecords;
import com.lyncode.xoai.model.oaipmh.Metadata;
import com.lyncode.xoai.model.oaipmh.Record;
import com.lyncode.xoai.model.oaipmh.ResumptionToken;

import fr.ortolang.diffusion.oaipmh.handlers.helpers.MultiMetadataItemRepositoryHelper;
import fr.ortolang.diffusion.oaipmh.repository.MultiMetadataItemRepository;

public class DiffusionListRecordsHandler extends VerbHandler<ListRecords> {
	
	private static final Logger LOGGER = Logger.getLogger(DiffusionListRecordsHandler.class.getName());
	
	private final MultiMetadataItemRepositoryHelper itemRepositoryHelper;
    private final SetRepositoryHelper setRepositoryHelper;

    public DiffusionListRecordsHandler(Context context, Repository repository) {
        super(context, repository);
        
        this.itemRepositoryHelper = new MultiMetadataItemRepositoryHelper((MultiMetadataItemRepository) getRepository().getItemRepository());
        this.setRepositoryHelper = new SetRepositoryHelper(getRepository().getSetRepository());
    }

    @Override
    public ListRecords handle(OAICompiledRequest parameters) throws OAIException, HandlerException {
    	ListRecords res = new ListRecords();
        int length = getRepository().getConfiguration().getMaxListRecords();

        if (parameters.hasSet() && !getRepository().getSetRepository().supportSets())
            throw new DoesNotSupportSetsException();

        LOGGER.log(Level.FINEST, "Getting items from data source");
        int offset = getOffset(parameters);
        ListItemsResults result;
        if (!parameters.hasSet()) {
            if (parameters.hasFrom() && !parameters.hasUntil())
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getFrom());
            else if (!parameters.hasFrom() && parameters.hasUntil())
                result = itemRepositoryHelper.getItemsUntil(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getUntil());
            else if (parameters.hasFrom() && parameters.hasUntil())
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getFrom(), parameters.getUntil());
            else
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix());
        } else {
            if (!setRepositoryHelper.exists(getContext(), parameters.getSet()))
                throw new NoMatchesException();
            if (parameters.hasFrom() && !parameters.hasUntil())
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getSet(), parameters.getFrom());
            else if (!parameters.hasFrom() && parameters.hasUntil())
                result = itemRepositoryHelper.getItemsUntil(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getSet(), parameters.getUntil());
            else if (parameters.hasFrom() && parameters.hasUntil())
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getSet(), parameters.getFrom(),
                        parameters.getUntil());
            else
                result = itemRepositoryHelper.getItems(getContext(), offset,
                        length, parameters.getMetadataPrefix(),
                        parameters.getSet());
        }
        LOGGER.log(Level.FINEST, "Items retrieved from data source");

        List<Item> results = result.getResults();
        if (results.isEmpty()) throw new NoMatchesException();
        LOGGER.log(Level.FINEST, "Now adding records to the OAI-PMH Output");
        for (Item i : results)
            res.withRecord(this.createRecord(parameters, i));


        ResumptionToken.Value currentResumptionToken = new ResumptionToken.Value();
        if (parameters.hasResumptionToken()) {
            currentResumptionToken = parameters.getResumptionToken();
        } else if (result.hasMore()) {
            currentResumptionToken = parameters.extractResumptionToken();
        }

        ResumptionTokenHelper resumptionTokenHelper = new ResumptionTokenHelper(currentResumptionToken,
                getRepository().getConfiguration().getMaxListRecords());
        res.withResumptionToken(resumptionTokenHelper.resolve(result.hasMore()));

        return res;
    }


    private int getOffset(OAICompiledRequest parameters) {
        if (!parameters.hasResumptionToken())
            return 0;
        if (parameters.getResumptionToken().getOffset() == null)
            return 0;
        return parameters.getResumptionToken().getOffset().intValue();
    }

    private Record createRecord(OAICompiledRequest parameters, Item item)
            throws BadArgumentException, CannotDisseminateRecordException,
            OAIException, NoMetadataFormatsException, CannotDisseminateFormatException {
        Header header = new Header();
        Record record = new Record().withHeader(header);
        header.withIdentifier(item.getIdentifier());

        ItemHelper itemHelperWrap = new ItemHelper(item);

        header.withDatestamp(item.getDatestamp());
        for (Set set : itemHelperWrap.getSets(getContext(), getRepository().getFilterResolver()))
            header.withSetSpec(set.getSpec());
        if (item.isDeleted())
            header.withStatus(Header.Status.DELETED);

        if (!item.isDeleted()) {
            Metadata metadata = item.getMetadata();

            record.withMetadata(metadata);

            LOGGER.log(Level.FINEST, "Outputting ItemAbout");
            if (item.getAbout() != null) {
                for (About about : item.getAbout()) {
                    record.withAbout(about);
                }
            }
        }
        return record;
    }
}
