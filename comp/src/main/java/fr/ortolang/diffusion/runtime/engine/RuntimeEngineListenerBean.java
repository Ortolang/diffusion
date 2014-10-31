package fr.ortolang.diffusion.runtime.engine;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;


@MessageDriven(name = "RuntimeEngineQueueMDB", activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/queue/runtime"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@SecurityDomain("ortolang")
public class RuntimeEngineListenerBean implements MessageListener {
	
	private Logger logger = Logger.getLogger(RuntimeEngineListenerBean.class.getName());
	
	@Override
	@PermitAll
	public void onMessage(Message message) {
		try {
			logger.log(Level.WARNING, "message received: " + message.getJMSType());
		} catch (JMSException e) {
			logger.log(Level.SEVERE, "unable to read received message", e);
		}
	}

}