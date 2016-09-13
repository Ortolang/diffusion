package fr.ortolang.diffusion.store.handle;

import java.util.List;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.store.handle.entity.Handle;

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


public interface HandleStoreService extends OrtolangService {
	
	String SERVICE_NAME = "handle-store";
	
	String INFO_TOTAL_SIZE = "size.total";
	
	Handle readHandle(String handle) throws HandleStoreServiceException, HandleNotFoundException;
	
	void recordHandle(String handle, String key, String url) throws HandleStoreServiceException;
	
	void dropHandle(String handle) throws HandleStoreServiceException;
	
	List<Handle> listHandleValues(String handle) throws HandleStoreServiceException, HandleNotFoundException;
	
	List<String> listHandlesForKey(String key) throws HandleStoreServiceException;
	
	List<Handle> findHandlesByName(int offset, int limit, String handleFilter) throws HandleStoreServiceException;
    
    List<Handle> findHandlesByValue(int offset, int limit, String valueFilter) throws HandleStoreServiceException;
    
}
