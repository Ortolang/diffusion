package fr.ortolang.diffusion.tool.job;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import fr.ortolang.diffusion.tool.job.entity.ToolJob;
import fr.ortolang.diffusion.tool.job.entity.ToolJobStatus;


@MessageDriven(name = "ToolJobQueueMDB", activationConfig = { 
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/queue/jobs")
		})
public class ToolJobInvocationListener implements MessageListener {

	private Logger logger = Logger.getLogger(ToolJobInvocationListener.class.getName());

	@EJB
	private ToolJobService tjob;
	
	public ToolJobInvocationListener() { }

	@Override
	public void onMessage(Message message) {
		try {
            logger.log(Level.INFO, "Queue: I received a Message at " + new Date());
            String id = message.getStringProperty("id");
            ToolJob job = tjob.read(id);
            job.setStatus(ToolJobStatus.RUNNING);
            System.out.println("Job Details: ");
            System.out.println(job.getId());
            System.out.println(job.getName());
            System.out.println(job.getParameters().toString());            
            System.out.println(job.getPriority());
            System.out.println(job.getStatus());
            String log = execute(job);
            tjob.complete(id, log);
 
        } catch (JMSException | ToolJobException e) {
            e.printStackTrace();
        }	
	}

	private String execute(ToolJob job) {
		// TODO Auto-generated method stub
		return null;		
	}

}
