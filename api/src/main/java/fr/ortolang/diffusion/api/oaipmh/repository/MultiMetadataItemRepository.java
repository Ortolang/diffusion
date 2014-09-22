package fr.ortolang.diffusion.api.oaipmh.repository;

import java.util.Date;
import java.util.List;

import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.NoMetadataFormatsException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.repository.ItemRepository;

public interface MultiMetadataItemRepository extends ItemRepository {

    public List<String> getListMetadataFormats(String identifier) throws IdDoesNotExistException, NoMetadataFormatsException, OAIException;
    
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length) throws OAIException;
	
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from)
			throws OAIException;
	
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec)
			throws OAIException;
	
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date from, Date until)
			throws OAIException;
			
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date from)
			throws OAIException;
			
	public ListItemIdentifiersResult getItemIdentifiers(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec,
			Date from, Date until) throws OAIException;
	
	public ListItemIdentifiersResult getItemIdentifiersUntil(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, Date until)
			throws OAIException;
	
	public ListItemIdentifiersResult getItemIdentifiersUntil(
			List<ScopedFilter> filters, String metadataPrefix, int offset, int length, String setSpec, Date until)
			throws OAIException;
	
	public Item getItem(String identifier, String metadataPrefix) throws IdDoesNotExistException,
				OAIException;

	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset, int length)
			throws OAIException;
	
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, Date from) throws OAIException;
	
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, String setSpec) throws OAIException;
	
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, Date from, Date until) throws OAIException;
	
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, String setSpec, Date from) throws OAIException;
	
	public ListItemsResults getItems(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, String setSpec, Date from, Date until) throws OAIException;
			
	public ListItemsResults getItemsUntil(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, Date until) throws OAIException;
	
	public ListItemsResults getItemsUntil(List<ScopedFilter> filters, String metadataPrefix, int offset,
			int length, String setSpec, Date until) throws OAIException;
}
