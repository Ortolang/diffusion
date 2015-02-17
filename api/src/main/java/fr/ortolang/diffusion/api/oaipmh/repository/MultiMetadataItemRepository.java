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
