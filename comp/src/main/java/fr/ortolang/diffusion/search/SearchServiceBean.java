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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.index.IndexStoreServiceException;
import fr.ortolang.diffusion.store.json.JsonStoreService;
import fr.ortolang.diffusion.store.json.JsonStoreServiceException;

@Local(SearchService.class)
@Stateless(name = SearchService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class SearchServiceBean implements SearchService {

	private static final Logger LOGGER = Logger.getLogger(SearchServiceBean.class.getName());
	
	private static final String[] OBJECT_TYPE_LIST = new String[] { };
	private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };
	
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private IndexStoreService indexStore;
	@EJB
	private JsonStoreService jsonStore;
	@Resource
	private SessionContext ctx;
	
	public SearchServiceBean() {
	}
	
	public IndexStoreService getIndexStoreService() {
		return indexStore;
	}

	public void setIndexStoreService(IndexStoreService store) {
		this.indexStore = store;
	}

	public JsonStoreService getJsonStoreService() {
		return jsonStore;
	}

	public void setJsonStoreService(JsonStoreService jsonStore) {
		this.jsonStore = jsonStore;
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
	
	public void setSessionContext(SessionContext ctx) {
		this.ctx = ctx;
	}

	public SessionContext getSessionContext() {
		return this.ctx;
	}
	
	@Override
	public String getServiceName() {
		return SearchService.SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return OBJECT_TYPE_LIST;
	}

	@Override
	public String[] getObjectPermissionsList(String type) throws OrtolangException {
		return OBJECT_PERMISSIONS_LIST;
	}

	@Override
	public OrtolangObject findObject(String key) throws OrtolangException {
		throw new OrtolangException("This service does not manage any object");
	}

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("This service does not manage any object");
    }

    @Override
	public List<OrtolangSearchResult> indexSearch(String query) throws SearchServiceException {
		LOGGER.log(Level.FINE, "Performing index search with query: " + query);
		try {
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			List<OrtolangSearchResult> checkedResults = new ArrayList<OrtolangSearchResult>();
			for ( OrtolangSearchResult result : indexStore.search(query) ) {
				try {
					authorisation.checkPermission(result.getKey(), subjects, "read");
					checkedResults.add(result);
				} catch ( AccessDeniedException e ) {
				}
			}
			return checkedResults;
		} catch ( IndexStoreServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException e ) {
			throw new SearchServiceException("unable to perform index search", e);
		}
	}
	
	@Override
	public List<String> jsonSearch(String query) throws SearchServiceException {
		LOGGER.log(Level.FINE, "Performing semantic search with query: " + query);
		try {
			List<String> result = jsonStore.search(query);
			return result;
		} catch ( JsonStoreServiceException e ) {
			throw new SearchServiceException("unable to perform json search", e);
		}
	}

}
