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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.content.ContentSearchService;
import fr.ortolang.diffusion.content.entity.ContentSearchResource;
import fr.ortolang.diffusion.indexing.elastic.ElasticSearchService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;

@Local(SearchService.class)
@Stateless(name = SearchService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class SearchServiceBean implements SearchService {

	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private ElasticSearchService elasticService;
	@EJB
	private ContentSearchService contentService;
	
	
	public SearchServiceBean() {
	}
	
	public MembershipService getMembershipService() {
		return membership;
	}

	public void setMembershipService(MembershipService membership) {
		this.membership = membership;
	}

	public AuthorisationService getAuthorisationService() {
		return authorisation;
	}

	public void setAuthorisationService(AuthorisationService authorisation) {
		this.authorisation = authorisation;
	}
	
//	@Override
//	public List<OrtolangSearchResult> indexSearch(String query) throws SearchServiceException {
//		LOGGER.log(Level.FINE, "Performing index search with query: " + query);
//		try {
//			List<String> subjects = membership.getConnectedIdentifierSubjects();
//			List<OrtolangSearchResult> checkedResults = new ArrayList<OrtolangSearchResult>();
//			long timestamp = System.currentTimeMillis();
//			List<OrtolangSearchResult> results = indexStore.search(query);
//			LOGGER.log(Level.FINE, "Performed index search in : " + (System.currentTimeMillis()-timestamp));
//			for ( OrtolangSearchResult result : results ) {
//				try {
//					authorisation.checkPermission(result.getKey(), subjects, "read");
//					checkedResults.add(result);
//				} catch ( AccessDeniedException e ) {
//                    continue;
//				}
//			}
//			return checkedResults;
//		} catch ( IndexStoreServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException e ) {
//			throw new SearchServiceException("unable to perform index search", e);
//		}
//	}

	@Override
	public SearchResult search(SearchQuery query) {
		return elasticService.search(query);
	}

	@Override
	public SearchResult systemSearch(SearchQuery query) {
		return elasticService.systemSearch(query);
	}
	
	@Override
	public String get(String index, String type, String id) {
		return elasticService.get(index, type, id);
	}
	
	@Override
	public List<ContentSearchResource> listResources() {
		return contentService.listResource();
	}

	@Override
    public String getServiceName() {
        return SearchService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

}
