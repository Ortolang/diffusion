package fr.ortolang.diffusion.search;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.index.IndexStoreServiceException;
import fr.ortolang.diffusion.store.triple.TripleStoreService;
import fr.ortolang.diffusion.store.triple.TripleStoreServiceException;

@Local(SearchService.class)
@Stateless(name = SearchService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class SearchServiceBean implements SearchService {

	private Logger logger = Logger.getLogger(SearchServiceBean.class.getName());
	
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@EJB
	private IndexStoreService indexStore;
	@EJB
	private TripleStoreService tripleStore;
	@EJB
	private NotificationService notification;
	@Resource
	private SessionContext ctx;
	
	public SearchServiceBean() {
	}
	
	public NotificationService getNotificationService() {
		return notification;
	}

	public void setNotificationService(NotificationService notificationService) {
		this.notification = notificationService;
	}

	public IndexStoreService getIndexStoreService() {
		return indexStore;
	}

	public void setIndexStoreService(IndexStoreService store) {
		this.indexStore = store;
	}

	public TripleStoreService getTripleStoreService() {
		return tripleStore;
	}

	public void setTripleStoreService(TripleStoreService store) {
		this.tripleStore = store;
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
		return SearchService.OBJECT_TYPE_LIST;
	}

	@Override
	public String[] getObjectPermissionsList(String type) throws OrtolangException {
		return new String[] {};
	}

	@Override
	public OrtolangObject findObject(String key) throws OrtolangException {
		throw new OrtolangException("This service does not manage any object");
	}

	@Override
	public List<OrtolangSearchResult> indexSearch(String query) throws SearchServiceException {
		logger.log(Level.FINE, "Performing index search with query: " + query);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			List<OrtolangSearchResult> checkedResults = new ArrayList<OrtolangSearchResult>();
			for ( OrtolangSearchResult result : indexStore.search(query) ) {
				try {
					authorisation.checkPermission(result.getKey(), subjects, "read");
					checkedResults.add(result);
				} catch ( AccessDeniedException e ) {
				}
			}
			notification.throwEvent("", caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(SearchService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "index-search"), "query=" + query);
			return checkedResults;
		} catch ( IndexStoreServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | NotificationServiceException e ) {
			throw new SearchServiceException("unable to perform index search", e);
		}
	}
	
	@Override
	public String semanticSearch(String query, String languageResult) throws SearchServiceException {
		logger.log(Level.FINE, "Performing semantic search with query: " + query);
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			String result = tripleStore.query("SPARQL", query, languageResult);
			notification.throwEvent("", caller, OrtolangObject.OBJECT_TYPE, OrtolangEvent.buildEventType(SearchService.SERVICE_NAME, OrtolangObject.OBJECT_TYPE, "triple-search"), "query=" + query);
			return result;
		} catch ( TripleStoreServiceException | NotificationServiceException e ) {
			throw new SearchServiceException("unable to perform semantic search", e);
		}
	}

}
