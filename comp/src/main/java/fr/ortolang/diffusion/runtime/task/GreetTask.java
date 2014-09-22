package fr.ortolang.diffusion.runtime.task;

import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.membership.MembershipService;

public class GreetTask extends Task {
	
	public static final String GREETER_PARAM = "greeter";
	
	private Logger logger = Logger.getLogger(GreetTask.class.getName());
	private MembershipService memberhsip;
	
	public GreetTask() {
		super();
	}
	
	public MembershipService getMembershipService() throws Exception {
		if ( memberhsip == null ) {
			memberhsip = (MembershipService) OrtolangServiceLocator.findService(MembershipService.SERVICE_NAME);
		}
		return memberhsip;
	}
	
	@Override
	public TaskState execute() {
		try {
			this.log("Starting greet task");
			if ( getParams().containsKey(GREETER_PARAM) && getParam(GREETER_PARAM).length() > 0 ) {
				logger.log(Level.INFO, "Hello " + getParam(GREETER_PARAM) + " !!");
				this.log("Hello " + getParam(GREETER_PARAM) + " !!");
			} else {
				String profile = getMembershipService().getProfileKeyForConnectedIdentifier();
				String fullname = getMembershipService().readProfile(profile).getFullname();
				logger.log(Level.INFO, "Hello " + fullname + "!!");
				this.log("Hello " + fullname + " !!");
			}
			this.log("Greet task ended");
			return TaskState.COMPLETED;
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "error during task execution" + e);
			this.log("unexpected error occured during task execution: " + e.getMessage() + ", see server log for further details");
			return TaskState.ERROR;
		}
	}

}