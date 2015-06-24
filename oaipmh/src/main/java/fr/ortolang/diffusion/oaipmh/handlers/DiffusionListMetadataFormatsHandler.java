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

import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.InternalOAIException;
import com.lyncode.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.handlers.VerbHandler;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.model.MetadataFormat;
import com.lyncode.xoai.dataprovider.parameters.OAICompiledRequest;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.model.oaipmh.ListMetadataFormats;

import fr.ortolang.diffusion.oaipmh.handlers.helpers.MultiMetadataItemRepositoryHelper;
import fr.ortolang.diffusion.oaipmh.repository.MultiMetadataItemRepository;

public class DiffusionListMetadataFormatsHandler extends VerbHandler<ListMetadataFormats> {
	private MultiMetadataItemRepositoryHelper itemRepositoryHelper;

    public DiffusionListMetadataFormatsHandler(Context context, Repository repository) {
        super(context, repository);
        itemRepositoryHelper = new MultiMetadataItemRepositoryHelper((MultiMetadataItemRepository) repository.getItemRepository());

        // Static validation
        if (getContext().getMetadataFormats() == null ||
                getContext().getMetadataFormats().isEmpty())
            throw new InternalOAIException("The context must expose at least one metadata format");
    }


    @Override
    public ListMetadataFormats handle(OAICompiledRequest params) throws OAIException, HandlerException {
        ListMetadataFormats result = new ListMetadataFormats();

        if (params.hasIdentifier()) {
            List<String> metadataFormats = itemRepositoryHelper.getListMetadataFormats(params.getIdentifier());
            
            if (metadataFormats.isEmpty())
                throw new NoMetadataFormatsException();
            
            for (String metadataPrefix : metadataFormats) {
            	
            	MetadataFormat metadataFormat = getContext().formatForPrefix(metadataPrefix);
            	
            	if(metadataFormat!=null) {
	                com.lyncode.xoai.model.oaipmh.MetadataFormat format = new com.lyncode.xoai.model.oaipmh.MetadataFormat()
	                    .withMetadataPrefix(metadataFormat.getPrefix())
	                    .withMetadataNamespace(metadataFormat.getNamespace())
	                    .withSchema(metadataFormat.getSchemaLocation());
	                result.withMetadataFormat(format);
            	} else
            		throw new NoMetadataFormatsException();
            }
        } else {
            for (MetadataFormat metadataFormat : getContext().getMetadataFormats()) {
                com.lyncode.xoai.model.oaipmh.MetadataFormat format = new com.lyncode.xoai.model.oaipmh.MetadataFormat()
                        .withMetadataPrefix(metadataFormat.getPrefix())
                        .withMetadataNamespace(metadataFormat.getNamespace())
                        .withSchema(metadataFormat.getSchemaLocation());
                result.withMetadataFormat(format);
            }
        }

        return result;
    }
}
