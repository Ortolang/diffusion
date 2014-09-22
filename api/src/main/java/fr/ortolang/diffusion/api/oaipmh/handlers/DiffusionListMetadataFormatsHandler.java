package fr.ortolang.diffusion.api.oaipmh.handlers;

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

import fr.ortolang.diffusion.api.oaipmh.handlers.helpers.MultiMetadataItemRepositoryHelper;
import fr.ortolang.diffusion.api.oaipmh.repository.MultiMetadataItemRepository;

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
