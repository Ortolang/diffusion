package fr.ortolang.diffusion.tool;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.jgroups.util.UUID;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
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
import fr.ortolang.diffusion.tool.entity.Tool;
import fr.ortolang.diffusion.tool.invoke.ToolInvoker;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

@Local(ToolService.class)
@Stateless(name = ToolService.SERVICE_NAME)
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class ToolServiceBean implements ToolService {

	private Logger logger = Logger.getLogger(ToolServiceBean.class.getName());
	
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

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void declareTool(String key, String name, String description, String documentation, String invokerClass, String formConfig) throws ToolServiceException {
		logger.log(Level.INFO, "Declaring new tool");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			authorisation.checkAuthentified(caller);
			
			try {
				@SuppressWarnings("rawtypes")
				Class invoker = Class.forName(invokerClass);
				if ( !Arrays.asList(invoker.getInterfaces()).contains(ToolInvoker.class) ) {
					throw new ToolServiceException("unable to declare tool, invoker class must implement ToolInvoker");
				}
			} catch ( ClassNotFoundException e ) {
				logger.log(Level.INFO, "error : unable to declare tool, invoker class not found "+e);
				throw new ToolServiceException("unable to declare tool, invoker class not found", e);
			}

			String id = UUID.randomUUID().toString();
			Tool tool = new Tool();
			tool.setId(id);
			tool.setName(name);
			tool.setDescription(description);
			tool.setDocumentation(documentation);
			tool.setInvokerClass(invokerClass);
			tool.setFormConfig(formConfig);
			em.persist(tool);

			registry.register(key, new OrtolangObjectIdentifier(ToolService.SERVICE_NAME, Tool.OBJECT_TYPE, id), caller);
			authorisation.createPolicy(key, caller);

			notification.throwEvent(key, caller, Tool.OBJECT_TYPE, OrtolangEvent.buildEventType(ToolService.SERVICE_NAME, Tool.OBJECT_TYPE, "declare"), "");
		} catch (RegistryServiceException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | NotificationServiceException | AccessDeniedException e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured while declaring tool", e);
			throw new ToolServiceException("unable to declare tool", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<Tool> listTools() throws ToolServiceException {
		logger.log(Level.INFO, "Listing tools");
		try {
			TypedQuery<Tool> query = em.createNamedQuery("findAllTools", Tool.class);
			List<Tool> tools = query.getResultList();
			List<Tool> rtools = new ArrayList<Tool>();
			for (Tool tool : tools) {
				try {
					String ikey = registry.lookup(tool.getObjectIdentifier());
					tool.setKey(ikey);
					rtools.add(tool);
				} catch ( IdentifierNotRegisteredException e ) {
					logger.log(Level.FINE, "unregistered tool found in storage for id: " + tool.getId());
				}
			}
			return rtools;
		} catch ( RegistryServiceException e ) {
			logger.log(Level.SEVERE, "unexpected error occured while listing tools", e);
			throw new ToolServiceException("unable to list tools", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Tool readTool(String key) throws ToolServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Reading tool");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Tool.OBJECT_TYPE);
			
			Tool tool = em.find(Tool.class, identifier.getId());
			if ( tool == null )  {
				throw new ToolServiceException("unable to find a tool with id: " + identifier.getId());
			}
			tool.setKey(key);
						
			notification.throwEvent(key, caller, Tool.OBJECT_TYPE, OrtolangEvent.buildEventType(ToolService.SERVICE_NAME, Tool.OBJECT_TYPE, "read"), "");
			return tool;
		} catch ( RegistryServiceException | KeyNotFoundException | NotificationServiceException e ) {
			logger.log(Level.SEVERE, "unexpected error occured while reading tool", e);
			throw new ToolServiceException("unable to read tool", e);
		}
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public ToolInvokerResult invokeTool(String key, Map<String, String> params) throws ToolServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Invoking tool");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "invoke");
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Tool.OBJECT_TYPE);
			
			Tool tool = em.find(Tool.class, identifier.getId());
			if ( tool == null )  {
				throw new ToolServiceException("unable to find a tool with id: " + identifier.getId());
			}
						
			Class<?> invoker = Class.forName(tool.getInvokerClass());
			ToolInvoker iinvoker = (ToolInvoker) invoker.newInstance();
			ToolInvokerResult result = iinvoker.invoke(params);
			
			notification.throwEvent(key, caller, Tool.OBJECT_TYPE, OrtolangEvent.buildEventType(ToolService.SERVICE_NAME, Tool.OBJECT_TYPE, "invoke"), "");
			return result;
		} catch ( RegistryServiceException | MembershipServiceException | KeyNotFoundException | AuthorisationServiceException | NotificationServiceException | ClassNotFoundException | InstantiationException | IllegalAccessException e ) {
			logger.log(Level.SEVERE, "unexpected error occured while invoking tool", e);
			throw new ToolServiceException("unable to invoke tool", e);
		}
	}
	
	
	@Override
	public String getServiceName() {
		return ToolService.SERVICE_NAME;
	}

	@Override
	public String[] getObjectTypeList() {
		return ToolService.OBJECT_TYPE_LIST;
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

			if (!identifier.getService().equals(ToolService.SERVICE_NAME)) {
				throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
			}

			if (identifier.getType().equals(Tool.OBJECT_TYPE)) {
				return readTool(key);
			}

			throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
		} catch (ToolServiceException | RegistryServiceException e) {
			throw new OrtolangException("unable to find an object for key " + key);
		}
	}

	private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws ToolServiceException {
		if (!identifier.getService().equals(getServiceName())) {
			throw new ToolServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
		}

		if (!identifier.getType().equals(objectType)) {
			throw new ToolServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
		}
	}

	@Override
	public String getFormConfig(String key) throws ToolServiceException, AccessDeniedException {
		logger.log(Level.INFO, "Loading config form");
		try {
			String caller = membership.getProfileKeyForConnectedIdentifier();
			List<String> subjects = membership.getConnectedIdentifierSubjects();
			authorisation.checkPermission(key, subjects, "read");
			
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			checkObjectType(identifier, Tool.OBJECT_TYPE);
			
			Tool tool = em.find(Tool.class, identifier.getId());
			if ( tool == null )  {
				throw new ToolServiceException("unable to find a tool with id: " + identifier.getId());
			}
			InputStream is = getClass().getClassLoader().getResourceAsStream("tools/" + tool.getFormConfig());
			 
			/*** read from file ***/
			String jsonData = IOUtils.toString(is);
			if ( jsonData == null )  {
				throw new ToolServiceException("unable to find a config form for tool with id: " + identifier.getId());
			}
									
			notification.throwEvent(key, caller, Tool.OBJECT_TYPE, OrtolangEvent.buildEventType(ToolService.SERVICE_NAME, Tool.OBJECT_TYPE, "read"), "");
			return jsonData;
		} catch ( RegistryServiceException | MembershipServiceException | KeyNotFoundException | AuthorisationServiceException | NotificationServiceException | IOException e ) {
			throw new ToolServiceException("unable to load config form", e);
		}
	}

}
