package fr.ortolang.diffusion.workflow;

import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

@Stateless
@SecurityDomain("ortolang")
@RolesAllowed("user")
public class WorkflowServiceBean implements WorkflowService {
	
	private static Logger logger = Logger.getLogger(WorkflowServiceBean.class.getName());
	
	
	
	

}
