package fr.ortolang.diffusion.api.oaipmh.handlers;

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

import fr.ortolang.diffusion.api.oaipmh.handlers.helpers.MultiMetadataItemRepositoryHelper;
import fr.ortolang.diffusion.api.oaipmh.repository.MultiMetadataItemRepository;

public class DiffusionListRecordsHandler extends VerbHandler<ListRecords> {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	
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

        logger.log(Level.FINEST, "Getting items from data source");
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
        logger.log(Level.FINEST, "Items retrieved from data source");

        List<Item> results = result.getResults();
        if (results.isEmpty()) throw new NoMatchesException();
        logger.log(Level.FINEST, "Now adding records to the OAI-PMH Output");
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

            logger.log(Level.FINEST, "Outputting ItemAbout");
            if (item.getAbout() != null) {
                for (About about : item.getAbout()) {
                    record.withAbout(about);
                }
            }
        }
        return record;
    }
}
