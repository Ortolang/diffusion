package fr.ortolang.diffusion.tool.job;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import fr.ortolang.diffusion.tool.ToolConfig;
import fr.ortolang.diffusion.tool.invoke.ToolJobInvocationResult;
import fr.ortolang.diffusion.tool.invoke.ToolJobInvocation;
import fr.ortolang.diffusion.tool.job.entity.ToolJob;


@MessageDriven(name = "ToolJobQueueMDB", activationConfig = { 
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/queue/jobs"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
		})
public class ToolJobInvocationListener implements MessageListener {

	private Logger logger = Logger.getLogger(ToolJobInvocationListener.class.getName());
	
	@EJB
	private ToolJobService tjob;	

	@EJB
	private ToolJobInvocation invoker;
	
	public ToolJobInvocationListener() { }

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void onMessage(Message message) {
		try {
            logger.log(Level.INFO, "## Queue: I received a Message at " + new Date());
            String id = message.getStringProperty("id");
            process(id);
        } catch (JMSException | ToolJobException | SecurityException | IOException e) {
        	logger.log(Level.SEVERE, "An exception prevented job processing : " + e.getMessage(), e); 
        	e.printStackTrace();
		}	
	}	
	
	/**
	 * Process job with given id
	 * @param id
	 * @throws ToolJobException 
	 * @throws IOException 
	 * @throws SecurityException 
	 */
	public void process(String id) throws SecurityException, IOException, ToolJobException {
		// Start logging job execution
		String base = ToolConfig.getInstance().getProperty("tool.working.space.path");
		String logFileName = ToolConfig.getInstance().getProperty("tool.log.filename");
		Path logFile = Paths.get(base, id, logFileName);

		logger.log(Level.INFO, "Starting job process for id : " + id);
		ToolJob job = tjob.read(id);
        try {
			System.out.println("Job Details: ");
	        System.out.println(job.getId());
	        System.out.println(job.getOwner());
	        System.out.println(job.getParameters().toString());            
	        System.out.println(job.getPriority());
	        System.out.println(job.getStatus());
	        
	        logger.log(Level.INFO, "Invocation called...");
	        job.setStart(System.currentTimeMillis());
			ToolJobInvocationResult result = invoker.invoke(job, logFile);
			logger.log(Level.INFO, "Invocation finished");
			job.setStop(System.currentTimeMillis());
			File f;
	        String str = "";
			switch(result.getStatus()){
	        case SUCCESS :
		        logger.log(Level.INFO, "Invocation successful : setting job as completed");
		        str = new String(Files.readAllBytes(logFile));
		        System.out.println(str);
	            tjob.complete(id, str);
	        	break;
	        case ERROR : 
		        logger.log(Level.INFO, "Invocation failed, setting job as interrupted with error");
		        str = new String(Files.readAllBytes(logFile));
		        tjob.error(id, str);
	        	break;
	        }
	        
		} catch (ToolJobException | IOException e) {
			logger.log(Level.SEVERE, "Invocation failed : " + e.getMessage(), e); 
			job.setStop(System.currentTimeMillis());
			String str = new String(Files.readAllBytes(logFile));
	        tjob.error(id, str);
			e.printStackTrace();
		}	
	}

}
