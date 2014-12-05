package fr.ortolang.diffusion.tool.job;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.tool.ToolConfig;
import fr.ortolang.diffusion.tool.client.OrtolangDiffusionRestClient;
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
	
	/**
	 * Get an ortolang dataobject with given key
	 * @param key
	 * @return Path Path of the file on the server
	 * @throws IOException
	 * @throws ToolJobException 
	 */
	private Path getDataObject(ToolJob job) throws IOException, ToolJobException {
		String key = job.getParameter("input");
		
		String url = ToolConfig.getInstance().getProperty("ortolang.diffusion.host.url") + ToolConfig.getInstance().getProperty("ortolang.diffusion.rest.url");
		// Temporary hack
		String username = ToolConfig.getInstance().getProperty("ortolang.diffusion.username");
		String password = ToolConfig.getInstance().getProperty("ortolang.diffusion.password");
		
		OrtolangDiffusionRestClient client = new OrtolangDiffusionRestClient(url, username, password);
		
		if(client.objectExists(key)){
			JsonObject object = client.getObject(key);
			String fileName = object.getJsonObject("object").getString("name");
			logger.log(Level.INFO, fileName);
			InputStream input = client.downloadObject(key);
			logger.log(Level.INFO, input.toString());
			String base = ToolConfig.getInstance().getProperty("tool.working.space.path");
	        Path path = Paths.get(base, job.getId(), fileName);
			Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
	        
			return path;
		} else {
			throw new ToolJobException("unable to get dataobject with key " + key);
		}
	}

	/**
	 * Execute tool job
	 * @param job
	 * @return
	 * @throws ToolJobException
	 */
	private String execute(ToolJob job) throws ToolJobException {
		
		try {
			Path pathFile = getDataObject(job);	        
			return "everything's ok!";
		} catch (IOException e) {
			String log = e.getMessage(); 
			tjob.error(job.getId(), log);
			e.printStackTrace();
			return log;
		}
	}

}
