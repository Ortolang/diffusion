package fr.ortolang.diffusion.api.oaipmh.handlers;

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
