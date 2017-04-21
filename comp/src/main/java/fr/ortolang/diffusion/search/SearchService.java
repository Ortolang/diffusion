package fr.ortolang.diffusion.search;

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
import java.util.Map;

import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.OrtolangService;

public interface SearchService extends OrtolangService {
	
	String SERVICE_NAME = "search";
	
//	List<OrtolangSearchResult> indexSearch(String query) throws SearchServiceException;
	
//	List<String> jsonSearch(String query) throws SearchServiceException;

//    List<String> findCollections(Map<String, String> fieldsProjection, String content, String group, String limit, String orderProp, String orderDir, Map<String, Object> fieldsMap) throws SearchServiceException;

//    int countCollections(Map<String, Object> fieldsMap) throws SearchServiceException;

//	String getCollection(String key) throws SearchServiceException;
	
//	List<String> findProfiles(String content, Map<String, String> fieldsProjection) throws SearchServiceException;
	
//	String getProfile(String key) throws SearchServiceException;
	
//	List<String> findWorkspaces(String content, Map<String, String> fieldsProjection, String group, String limit, String orderProp, String orderDir, Map<String, Object> fieldsMap) throws SearchServiceException;
	
//	String getWorkspace(String wsalias) throws SearchServiceException;
	
//	int countWorkspaces(Map<String, Object> fieldsMap) throws SearchServiceException;

//	List<String> findEntities(String content, Map<String, String> fieldsProjection) throws SearchServiceException;
	
//	String getEntity(String id) throws SearchServiceException;
	
	SearchResult search(SearchQuery query);
	SearchResult systemSearch(SearchQuery query);
	
	String get(String index, String type, String id);

//	List<OrtolangSearchResult> searchText(String text) throws SearchServiceException;
}
