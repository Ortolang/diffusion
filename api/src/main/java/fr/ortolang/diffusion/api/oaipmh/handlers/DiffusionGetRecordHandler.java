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

import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.handlers.VerbHandler;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.dataprovider.parameters.OAICompiledRequest;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.model.oaipmh.About;
import com.lyncode.xoai.model.oaipmh.GetRecord;
import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.model.oaipmh.Metadata;
import com.lyncode.xoai.model.oaipmh.Record;

import fr.ortolang.diffusion.api.oaipmh.handlers.helpers.MultiMetadataItemRepositoryHelper;
import fr.ortolang.diffusion.api.oaipmh.repository.MultiMetadataItemRepository;

/**
 * Same as GetRecordHandle from XOAI but drop the XSL transformation of the metadata.
 * 
 * @author cyril
 *
 */
public class DiffusionGetRecordHandler extends VerbHandler<GetRecord> {

	private final MultiMetadataItemRepositoryHelper itemRepositoryHelper;
	
	public DiffusionGetRecordHandler(Context context, Repository repository) {
        super(context, repository);

        this.itemRepositoryHelper = new MultiMetadataItemRepositoryHelper((MultiMetadataItemRepository) repository.getItemRepository());
    }

    @Override
    public GetRecord handle(OAICompiledRequest parameters) throws OAIException, HandlerException {
        Header header = new Header();
        Record record = new Record().withHeader(header);
        GetRecord result = new GetRecord(record);

        Item item = itemRepositoryHelper.getItem(getContext(), parameters.getIdentifier(), parameters.getMetadataPrefix());

        if(item==null)
        	throw new OAIException();
        
        header.withIdentifier(item.getIdentifier());
        header.withDatestamp(item.getDatestamp());

        for (Set set : getContext().getSets())
            if (set.getCondition().getFilter(getRepository().getFilterResolver()).isItemShown(item))
                header.withSetSpec(set.getSpec());

        for (Set set : item.getSets())
            header.withSetSpec(set.getSpec());

        if (item.isDeleted())
            header.withStatus(Header.Status.DELETED);

        if (!item.isDeleted()) {
            Metadata metadata = item.getMetadata();

            record.withMetadata(metadata);

            if (item.getAbout() != null) {
                for (About about : item.getAbout())
                    record.withAbout(about);
            }
        }
        return result;
    }
}
