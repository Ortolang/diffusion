package fr.ortolang.diffusion.form;

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

import fr.ortolang.diffusion.*;
import fr.ortolang.diffusion.form.entity.Form;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.*;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Local(FormService.class)
@Stateless(name = FormService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class FormServiceBean implements FormService {

	private static final Logger LOGGER = Logger.getLogger(FormServiceBean.class.getName());

	private static final String[] OBJECT_TYPE_LIST = new String[] { Form.OBJECT_TYPE };
	private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ Form.OBJECT_TYPE, "read,update,delete" }};
	
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
		LOGGER.log(Level.INFO, "Listing all forms");
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
					LOGGER.log(Level.FINE, "unregistered form found in storage for id: " + form.getId());
				}
			}
			return rforms;
		} catch (RegistryServiceException e) {
			LOGGER.log(Level.SEVERE, "unexpected error occured while listing forms", e);
			throw new FormServiceException("unable to list forms", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createForm(String key, String name, String definition) throws FormServiceException, KeyAlreadyExistsException, AccessDeniedException {
		LOGGER.log(Level.FINE, "creating form for key [" + key + "] and name [" + name + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkAuthentified(subjects);

			Form form = new Form();
			form.setId(UUID.randomUUID().toString());
			form.setName(name);
			form.setDefinition(definition);
			em.persist(form);

			registry.register(key, form.getObjectIdentifier(), caller);

			authorisation.createPolicy(key, caller);

			notification.throwEvent(key, caller, Form.OBJECT_TYPE, OrtolangEvent.buildEventType(FormService.SERVICE_NAME, Form.OBJECT_TYPE, "create"));
		} catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException e) {
			ctx.setRollbackOnly();
			throw new FormServiceException("unable to create form with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Form readForm(String key) throws FormServiceException, KeyNotFoundException {
		LOGGER.log(Level.FINE, "reading form for key [" + key + "]");
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Form.OBJECT_TYPE);
			Form form = em.find(Form.class, identifier.getId());
			if (form == null) {
				throw new FormServiceException("unable to find a form for id " + identifier.getId());
			}
			form.setKey(key);

			return form;
		} catch (RegistryServiceException e) {
			throw new FormServiceException("unable to read the form with key [" + key + "]", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void updateForm(String key, String name, String definition) throws FormServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "updating form for key [" + key + "]");
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

			notification.throwEvent(key, caller, Form.OBJECT_TYPE, OrtolangEvent.buildEventType(FormService.SERVICE_NAME, Form.OBJECT_TYPE, "update"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new FormServiceException("error while trying to update the form with key [" + key + "]");
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void deleteForm(String key) throws FormServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.FINE, "deleting form for key [" + key + "]");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "delete");

			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Form.OBJECT_TYPE);
			registry.delete(key);
			notification.throwEvent(key, caller, Form.OBJECT_TYPE, OrtolangEvent.buildEventType(FormService.SERVICE_NAME, Form.OBJECT_TYPE, "delete"));
		} catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
			ctx.setRollbackOnly();
			throw new FormServiceException("unable to delete form with key [" + key + "]", e);
		}
	}

	@Override
	public String getServiceName() {
		return SERVICE_NAME;
	}
	
	@Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
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
	public OrtolangObject findObject(String key) throws OrtolangException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);

			if (!identifier.getService().equals(FormService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(Form.OBJECT_TYPE)) {
				return readForm(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (FormServiceException | RegistryServiceException | KeyNotFoundException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

    // @TODO implement getSize
    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        return null;
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
