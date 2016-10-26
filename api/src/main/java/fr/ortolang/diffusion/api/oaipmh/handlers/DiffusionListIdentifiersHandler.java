package fr.ortolang.diffusion.api.oaipmh.handlers;

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

import com.lyncode.xoai.dataprovider.exceptions.DoesNotSupportSetsException;
import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.InternalOAIException;
import com.lyncode.xoai.dataprovider.exceptions.NoMatchesException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.handlers.VerbHandler;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.model.ItemIdentifier;
import com.lyncode.xoai.dataprovider.model.MetadataFormat;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.dataprovider.parameters.OAICompiledRequest;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.model.oaipmh.ListIdentifiers;
import com.lyncode.xoai.model.oaipmh.ResumptionToken;

import fr.ortolang.diffusion.api.oaipmh.handlers.helpers.MultiMetadataItemRepositoryHelper;
import fr.ortolang.diffusion.api.oaipmh.handlers.helpers.ResumptionTokenHelper;
import fr.ortolang.diffusion.api.oaipmh.repository.MultiMetadataItemRepository;

public class DiffusionListIdentifiersHandler extends VerbHandler<ListIdentifiers> {
    private final MultiMetadataItemRepositoryHelper itemRepositoryHelper;

    public DiffusionListIdentifiersHandler(Context context, Repository repository) {
        super(context, repository);
        this.itemRepositoryHelper = new MultiMetadataItemRepositoryHelper((MultiMetadataItemRepository) repository.getItemRepository());
    }


    @Override
    public ListIdentifiers handle(OAICompiledRequest parameters) throws OAIException, HandlerException {
        ListIdentifiers result = new ListIdentifiers();

        if (parameters.hasSet() && !getRepository().getSetRepository().supportSets())
            throw new DoesNotSupportSetsException();

        int length = getRepository().getConfiguration().getMaxListIdentifiers();
        int offset = getOffset(parameters);
        ListItemIdentifiersResult listItemIdentifiersResult;
        if (!parameters.hasSet()) {
            if (parameters.hasFrom() && !parameters.hasUntil())
                listItemIdentifiersResult = itemRepositoryHelper.getItemIdentifiers(getContext(), offset, length,
                        parameters.getMetadataPrefix(), parameters.getFrom());
            else if (!parameters.hasFrom() && parameters.hasUntil())
                listItemIdentifiersResult = itemRepositoryHelper.getItemIdentifiersUntil(getContext(), offset, length,
                        parameters.getMetadataPrefix(), parameters.getUntil());
            else if (parameters.hasFrom() && parameters.hasUntil())
                listItemIdentifiersResult = itemRepositoryHelper.getItemIdentifiers(getContext(), offset, length,
                        parameters.getMetadataPrefix(), parameters.getFrom(),
                        parameters.getUntil());
            else
                listItemIdentifiersResult = itemRepositoryHelper.getItemIdentifiers(getContext(), offset, length,
                        parameters.getMetadataPrefix());
        } else {
            if (!getRepository().getSetRepository().exists(parameters.getSet()) && !getContext().hasSet(parameters.getSet()))
                throw new NoMatchesException();

            if (parameters.hasFrom() && !parameters.hasUntil())
                listItemIdentifiersResult = itemRepositoryHelper.getItemIdentifiers(getContext(), offset, length,
                        parameters.getMetadataPrefix(), parameters.getSet(),
                        parameters.getFrom());
            else if (!parameters.hasFrom() && parameters.hasUntil())
                listItemIdentifiersResult = itemRepositoryHelper.getItemIdentifiersUntil(getContext(), offset, length,
                        parameters.getMetadataPrefix(), parameters.getSet(),
                        parameters.getUntil());
            else if (parameters.hasFrom() && parameters.hasUntil())
                listItemIdentifiersResult = itemRepositoryHelper.getItemIdentifiers(getContext(), offset, length,
                        parameters.getMetadataPrefix(), parameters.getSet(),
                        parameters.getFrom(), parameters.getUntil());
            else
                listItemIdentifiersResult = itemRepositoryHelper.getItemIdentifiers(getContext(), offset, length,
                        parameters.getMetadataPrefix(), parameters.getSet());
        }

        List<ItemIdentifier> itemIdentifiers = listItemIdentifiersResult.getResults();
        if (itemIdentifiers.isEmpty()) throw new NoMatchesException();

        for (ItemIdentifier itemIdentifier : itemIdentifiers)
            result.getHeaders().add(createHeader(parameters, itemIdentifier));

        ResumptionToken.Value currentResumptionToken = new ResumptionToken.Value();
        if (parameters.hasResumptionToken()) {
            currentResumptionToken = parameters.getResumptionToken();
        } else if (listItemIdentifiersResult.hasMore()) {
            currentResumptionToken = parameters.extractResumptionToken();
        }

        ResumptionTokenHelper resumptionTokenHelper = new ResumptionTokenHelper(currentResumptionToken,
                getRepository().getConfiguration().getMaxListIdentifiers()).withTotalResults(listItemIdentifiersResult.getTotal());
        System.out.println();
        result.withResumptionToken(resumptionTokenHelper.resolve(listItemIdentifiersResult.hasMore()));

        return result;
    }

    private int getOffset(OAICompiledRequest parameters) {
        if (!parameters.hasResumptionToken())
            return 0;
        if (parameters.getResumptionToken().getOffset() == null)
            return 0;
        return parameters.getResumptionToken().getOffset().intValue();
    }


    private Header createHeader(OAICompiledRequest parameters, ItemIdentifier itemIdentifier) {
        MetadataFormat format = getContext().formatForPrefix(parameters
                .getMetadataPrefix());
        if (!itemIdentifier.isDeleted() && !canDisseminate(itemIdentifier, format))
            throw new InternalOAIException("The item repository is currently providing items which cannot be disseminated with format "+format.getPrefix());

        Header header = new Header();
        header.withDatestamp(itemIdentifier.getDatestamp());
        header.withIdentifier(itemIdentifier.getIdentifier());
        if (itemIdentifier.isDeleted())
            header.withStatus(Header.Status.DELETED);

        for (Set set : getContext().getSets())
            if (set.getCondition().getFilter(getRepository().getFilterResolver()).isItemShown(itemIdentifier))
                header.withSetSpec(set.getSpec());

        for (Set set : itemIdentifier.getSets())
            header.withSetSpec(set.getSpec());

        return header;
    }

    private boolean canDisseminate(ItemIdentifier itemIdentifier, MetadataFormat format) {
        return !format.hasCondition() ||
                format.getCondition().getFilter(getRepository().getFilterResolver()).isItemShown(itemIdentifier);
    }
}
