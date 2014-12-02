package fr.ortolang.diffusion.form;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.form.entity.Form;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

@Local(FormService.class)
@Stateless(name = FormService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class FormServiceBean implements FormService {

	private Logger logger = Logger.getLogger(FormServiceBean.class.getName());

	@EJB
	private RegistryService registry;
	@EJB
	private NotificationService notification;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;
	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;
	@Resource
	private SessionContext ctx;

	public FormServiceBean() {
	}

	public RegistryService getRegistryService() {
		return registry;
	}

	public void setRegistryService(RegistryService registry) {
		this.registry = registry;
	}

	public NotificationService getNotificationService() {
		return notification;
	}

	public void setNotificationService(NotificationService notification) {
		this.notification = notification;
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

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<Form> listForms() throws FormServiceException {
		logger.log(Level.INFO, "Listing all forms");
		try {
			TypedQuery<Form> query = em.createNamedQuery("findAllForms", Form.class);
			List<Form> forms = query.getResultList();
			List<Form> rforms = new ArrayList<Form>();
			for (Form form : forms) {
				try {
					String ikey = registry.lookup(form.getObjectIdentifier());
					form.setKey(ikey);
					rforms.add(form);
				} catch (IdentifierNotRegisteredException e) {
					logger.log(Level.FINE, "unregistered form found in storage for id: " + form.getId());
				}
			}
			return rforms;
		} catch (RegistryServiceException e) {
			logger.log(Level.SEVERE, "unexpected error occured while listing forms", e);
			throw new FormServiceException("unable to list forms", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createForm(String key, String name, String definition) throws FormServiceException, KeyAlreadyExistsException, AccessDeniedException {
		logger.log(Level.FINE, "creating form for key [" + key + "] and name [" + name + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);

			Form form = new Form();
			form.setId(UUID.randomUUID().toString());
			form.setName(name);
			form.setDefinition(definition);
			em.persist(form);

			registry.register(key, form.getObjectIdentifier(), caller);

			authorisation.createPolicy(key, caller);

			notification.throwEvent(key, caller, Form.OBJECT_TYPE, OrtolangEvent.buildEventType(FormService.SERVICE_NAME, Form.OBJECT_TYPE, "create"), "");
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException e) {
			ctx.setRollbackOnly();
			throw new FormServiceException("unable to create form with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Form readForm(String key) throws FormServiceException, KeyNotFoundException {
		logger.log(Level.FINE, "reading form for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Form.OBJECT_TYPE);
			Form form = em.find(Form.class, identifier.getId());
			if (form == null) {
				throw new FormServiceException("unable to find a form for id " + identifier.getId());
			}
			form.setKey(key);

			notification.throwEvent(key, caller, Form.OBJECT_TYPE, OrtolangEvent.buildEventType(FormService.SERVICE_NAME, Form.OBJECT_TYPE, "read"), "");
			return form;
		} catch (RegistryServiceException | NotificationServiceException e) {
			throw new FormServiceException("unable to read the form with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateForm(String key, String name, String definition) throws FormServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "updating form for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "update");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Form.OBJECT_TYPE);
			Form form = em.find(Form.class, identifier.getId());
			if (form == null) {
				throw new FormServiceException("unable to find a form for id " + identifier.getId());
			}
			form.setName(name);
			form.setDefinition(definition);
			em.merge(form);

			registry.update(key);

			notification.throwEvent(key, caller, Form.OBJECT_TYPE, OrtolangEvent.buildEventType(FormService.SERVICE_NAME, Form.OBJECT_TYPE, "update"), "");
		} catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new FormServiceException("error while trying to update the form with key [" + key + "]");
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteForm(String key) throws FormServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.FINE, "deleting form for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Form.OBJECT_TYPE);
			registry.delete(key);
			notification.throwEvent(key, caller, Form.OBJECT_TYPE, OrtolangEvent.buildEventType(FormService.SERVICE_NAME, Form.OBJECT_TYPE, "delete"), "");
		} catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new FormServiceException("unable to delete form with key [" + key + "]", e);
		}
	}

	@Override
	public String getServiceName() {
		return SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return OBJECT_TYPE_LIST;
	}

	@Override
	public String[] getObjectPermissionsList(String type) throws OrtolangException {
		for (int i = 0; i < OBJECT_PERMISSIONS_LIST.length; i++) {
			if (OBJECT_PERMISSIONS_LIST[i][0].equals(type)) {
				return OBJECT_PERMISSIONS_LIST[i][1].split(",");
			}
		}
		throw new OrtolangException("Unable to find object permissions list for object type : " + type);
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public OrtolangObject findObject(String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(FormService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(Form.OBJECT_TYPE)) {
				return readForm(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (FormServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws FormServiceException {
		if (!identifier.getService().equals(getServiceName())) {
			throw new FormServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
		}

		if (!identifier.getType().equals(objectType)) {
			throw new FormServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
		}
	}

}