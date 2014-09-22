package fr.ortolang.diffusion.api.oaipmh.handlers.helpers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.lyncode.xoai.dataprovider.exceptions.CannotDisseminateFormatException;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.Scope;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.MetadataFormat;

import fr.ortolang.diffusion.api.oaipmh.repository.MultiMetadataItemRepository;

public class MultiMetadataItemRepositoryHelper {
	private MultiMetadataItemRepository itemRepository;

    public MultiMetadataItemRepositoryHelper(MultiMetadataItemRepository itemRepository) {
        super();
        this.itemRepository = itemRepository;
    }

    public List<String> getListMetadataFormats(String identifier) throws IdDoesNotExistException, NoMetadataFormatsException, OAIException {
    	return itemRepository.getListMetadataFormats(identifier);
    }
    
    public ListItemIdentifiersResult getItemIdentifiers(Context context,
                                                        int offset, int length, String metadataPrefix)
            throws CannotDisseminateFormatException, OAIException {
        return itemRepository.getItemIdentifiers(getScopedFilters(context, metadataPrefix), metadataPrefix, offset, length);
    }

    public ListItemIdentifiersResult getItemIdentifiers(Context context,
                                                        int offset, int length, String metadataPrefix, Date from)
            throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        return itemRepository.getItemIdentifiers(filters, metadataPrefix, offset, length, from);
    }

    private List<ScopedFilter> getScopedFilters(Context context, String metadataPrefix) throws CannotDisseminateFormatException {
        List<ScopedFilter> filters = new ArrayList<ScopedFilter>();
        if (context.hasCondition())
            filters.add(new ScopedFilter(context.getCondition(), Scope.Context));

        MetadataFormat metadataFormat = context.formatForPrefix(metadataPrefix);
        if(metadataFormat==null)
        		throw new CannotDisseminateFormatException();
        
        if (metadataFormat.hasCondition())
            filters.add(new ScopedFilter(metadataFormat.getCondition(), Scope.MetadataFormat));
        return filters;
    }

    public ListItemIdentifiersResult getItemIdentifiersUntil(
            Context context, int offset, int length, String metadataPrefix,
            Date until) throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        return itemRepository.getItemIdentifiersUntil(filters, metadataPrefix, offset, length, until);
    }

    public ListItemIdentifiersResult getItemIdentifiers(Context context,
                                                        int offset, int length, String metadataPrefix, Date from, Date until)
            throws CannotDisseminateFormatException, OAIException {
        return itemRepository.getItemIdentifiers(getScopedFilters(context, metadataPrefix), metadataPrefix, offset, length, from, until);
    }

    public ListItemIdentifiersResult getItemIdentifiers(Context context,
                                                        int offset, int length, String metadataPrefix, String setSpec)
            throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        if (context.isStaticSet(setSpec)) {
            filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
            return itemRepository.getItemIdentifiers(filters, metadataPrefix, offset, length);
        } else
            return itemRepository.getItemIdentifiers(filters, metadataPrefix, offset, length, setSpec);
    }

    public ListItemIdentifiersResult getItemIdentifiers(Context context,
                                                        int offset, int length, String metadataPrefix, String setSpec,
                                                        Date from) throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        if (context.isStaticSet(setSpec)) {
            filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
            return itemRepository.getItemIdentifiers(filters, metadataPrefix, offset, length, from);
        } else
            return itemRepository.getItemIdentifiers(filters, metadataPrefix, offset, length, setSpec,
                    from);
    }

    public ListItemIdentifiersResult getItemIdentifiersUntil(
            Context context, int offset, int length, String metadataPrefix,
            String setSpec, Date until) throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        if (context.isStaticSet(setSpec)) {
            filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
            return itemRepository.getItemIdentifiersUntil(filters, metadataPrefix, offset, length, until);
        } else
            return itemRepository.getItemIdentifiersUntil(filters, metadataPrefix, offset, length,
                    setSpec, until);
    }

    public ListItemIdentifiersResult getItemIdentifiers(Context context,
                                                        int offset, int length, String metadataPrefix, String setSpec,
                                                        Date from, Date until) throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        if (context.isStaticSet(setSpec)) {
            filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
            return itemRepository
                    .getItemIdentifiers(filters, metadataPrefix, offset, length, from, until);
        } else
            return itemRepository.getItemIdentifiers(filters, metadataPrefix, offset, length, setSpec,
                    from, until);
    }

    public ListItemsResults getItems(Context context, int offset,
                                     int length, String metadataPrefix)
            throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        return itemRepository.getItems(filters, metadataPrefix, offset, length);
    }

    public ListItemsResults getItems(Context context, int offset,
                                     int length, String metadataPrefix, Date from)
            throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        return itemRepository.getItems(filters, metadataPrefix, offset, length, from);
    }

    public ListItemsResults getItemsUntil(Context context, int offset,
                                          int length, String metadataPrefix, Date until)
            throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        return itemRepository.getItemsUntil(filters, metadataPrefix, offset, length, until);
    }

    public ListItemsResults getItems(Context context, int offset,
                                     int length, String metadataPrefix, Date from, Date until)
            throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        return itemRepository.getItems(filters, metadataPrefix, offset, length, from, until);
    }

    public ListItemsResults getItems(Context context, int offset,
                                     int length, String metadataPrefix, String setSpec)
            throws CannotDisseminateFormatException, OAIException {

        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        if (context.isStaticSet(setSpec)) {
            filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
            return itemRepository.getItems(filters, metadataPrefix, offset, length);
        } else
            return itemRepository.getItems(filters, metadataPrefix, offset, length, setSpec);
    }

    public ListItemsResults getItems(Context context, int offset,
                                     int length, String metadataPrefix, String setSpec, Date from)
            throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        if (context.isStaticSet(setSpec)) {
            filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
            return itemRepository.getItems(filters, metadataPrefix, offset, length, from);
        } else
            return itemRepository.getItems(filters, metadataPrefix, offset, length, setSpec, from);
    }

    public ListItemsResults getItemsUntil(Context context, int offset,
                                          int length, String metadataPrefix, String setSpec, Date until)
            throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        if (context.isStaticSet(setSpec)) {
            filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
            return itemRepository.getItemsUntil(filters, metadataPrefix, offset, length, until);
        } else
            return itemRepository.getItemsUntil(filters, metadataPrefix, offset, length, setSpec, until);
    }

    public ListItemsResults getItems(Context context, int offset,
                                     int length, String metadataPrefix, String setSpec, Date from,
                                     Date until) throws CannotDisseminateFormatException, OAIException {
        List<ScopedFilter> filters = getScopedFilters(context, metadataPrefix);
        if (context.isStaticSet(setSpec)) {
            filters.add(new ScopedFilter(context.getSet(setSpec).getCondition(), Scope.Set));
            return itemRepository.getItems(filters, metadataPrefix, offset, length, from, until);
        } else
            return itemRepository.getItems(filters, metadataPrefix, offset, length, setSpec, from, until);
    }

    public Item getItem(Context context, String identifier, String metadataPrefix) throws IdDoesNotExistException, CannotDisseminateFormatException, OAIException {

        MetadataFormat metadataFormat = context.formatForPrefix(metadataPrefix);
        if(metadataFormat==null)
        		throw new CannotDisseminateFormatException();
        
        return itemRepository.getItem(identifier, metadataPrefix);
    }
}
