package fr.ortolang.diffusion.tool.job;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Queue;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.io.FileUtils;

import fr.ortolang.diffusion.tool.ToolConfig;
import fr.ortolang.diffusion.tool.job.entity.ToolJob;
import fr.ortolang.diffusion.tool.job.entity.ToolJobStatus;
import fr.ortolang.diffusion.tool.job.ToolJobService;

@Local(ToolJobService.class)
@Stateless(name = ToolJobService.SERVICE_NAME)
public class ToolJobServiceBean implements ToolJobService {

	private Logger logger = Logger.getLogger(ToolJobServiceBean.class.getName());
	
	@Resource(mappedName = "java:jboss/exported/jms/queue/jobs")
	private Queue toolJobQueue;
	@Inject
	private JMSContext context;
	@PersistenceContext(unitName = "ortolangPU")
	private EntityManager em;
	@Resource
	private SessionContext ctx;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	public ToolJobServiceBean() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<ToolJob> list() throws ToolJobException {
		logger.log(Level.INFO, "Listing tool jobs");
		try {
			TypedQuery<ToolJob> query = em.createNamedQuery("findAllJobs", ToolJob.class);
			logger.log(Level.INFO, query.toString());
			List<ToolJob> jobs = query.getResultList();
			logger.log(Level.INFO, jobs.toString());
			return jobs;
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "unexpected error occured while listing jobs", e);
			throw new ToolJobException("unable to list jobs", e);
		}
	}

	@Override
	public ToolJob read(String id) throws ToolJobException {
		logger.log(Level.INFO, "Reading tool job");
		try {
			ToolJob job = em.find(ToolJob.class, id);
			if ( job == null )  {
				throw new ToolJobException("unable to find a tool with id: " + id);
			}			
			return job;
		} catch ( Exception e ) {
			logger.log(Level.SEVERE, "unexpected error occured while reading tool job", e);
			throw new ToolJobException("unable to read tool job", e);
		}
	}

	@Override
	public void submit(String name, int priority, Map<String, String> parameters) throws ToolJobException, IOException {
		logger.log(Level.INFO, "Submitting new tool job");
		String id = java.util.UUID.randomUUID().toString();
		
		try {
			// Instantiate a new job
			ToolJob job = new ToolJob();
			job.setId(id);
			job.setName(name);
			job.setParameters(parameters);
			job.setPriority(priority);
			job.setStatus(ToolJobStatus.PENDING);
			
			// Create a directory
			if ( ToolConfig.getInstance().getProperty("tool.working.space.path") != null ) {
				String base = ToolConfig.getInstance().getProperty("tool.working.space.path");
				File directory = new File(base + "/" + id);
				directory.mkdir();
				logger.log(Level.INFO, "Base working space path set to: " + base + "/" + id);
			} else {
				throw new ToolJobException("base working space path not found in configuration");
			}
			
			// Send the job, and persist it
			send(job);
			em.persist(job);
					
		} catch (Exception e) {
			ctx.setRollbackOnly();
			delete(id);
			logger.log(Level.SEVERE, "unexpected error occured while submitting tool job", e);
			throw new ToolJobException("unable to submit tool job", e);
		}
	}

	@Override
	public void abort(String id) throws ToolJobException {
		logger.log(Level.INFO, "Aborting tool job");
		
		try {
			ToolJob job = em.find(ToolJob.class, id);
			job.setStatus(ToolJobStatus.CANCELED);			
			//delete(id);
			em.persist(job);
					
		} catch (Exception e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured while aborting tool job", e);
			throw new ToolJobException("unable to abort tool job", e);
		}
				
	}

	@Override
	public void complete(String id, String log) throws ToolJobException {
		logger.log(Level.INFO, "Completing execution of a tool job");
		
		try {
			ToolJob job = em.find(ToolJob.class, id);
			job.setStatus(ToolJobStatus.COMPLETED);
			job.setLog(log);
			
			// schedule deletion of the file
			final String jobID = id;
			Runnable scheduleDelete = new Runnable() {
		        public void run() {
		            try {
						delete(jobID);
					} catch (IOException | ToolJobException e) {
						logger.log(Level.SEVERE, e.getMessage() + " : " + e.getStackTrace());						
					}
		        }
		    };
		    if ( ToolConfig.getInstance().getProperty("tool.deletion.delay.days") != null ) {
		    	long delay =  Long.parseLong(ToolConfig.getInstance().getProperty("tool.deletion.delay.days"));
				scheduler.schedule(scheduleDelete, delay, TimeUnit.DAYS);		    	
		    } else {
		    	// 5 days by default
				scheduler.schedule(scheduleDelete, 5, TimeUnit.DAYS);
		    }

			em.persist(job);
					
		} catch (Exception e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured while completing execution of tool job", e);
			throw new ToolJobException("unable to complete execution of tool job", e);
		}
	}

	@Override
	public void error(String id, String log) throws ToolJobException {
		logger.log(Level.INFO, "Error during execution of a tool job");
		
		try {
			ToolJob job = em.find(ToolJob.class, id);
			job.setStatus(ToolJobStatus.ERROR);	
			job.setLog(log);
			//delete(id);
			em.persist(job);
					
		} catch (Exception e) {
			ctx.setRollbackOnly();
			logger.log(Level.SEVERE, "unexpected error occured while signaling execution's error of tool job", e);
			throw new ToolJobException("unable to signal execution's error of tool job", e);
		}
	}

	@Override
	public void delete(String id) throws IOException, ToolJobException {		 
        if ( ToolConfig.getInstance().getProperty("tool.working.space.path") != null ) {
			String base = ToolConfig.getInstance().getProperty("tool.working.space.path");
			File directory = new File(base + "/" + id);
			
			if(!directory.exists()){	 
				logger.log(Level.INFO, "Directory " + base + "/" + id + " does not exist.");
 
		    }else{ 
		    	if(directory.isDirectory()){		    		 
		    		FileUtils.deleteDirectory(directory);
					logger.log(Level.INFO, "Directory " + directory.getAbsolutePath() + " is deleted.");
		    	}else{ // useful ?
		    		directory.delete();
					logger.log(Level.INFO, "File " + directory.getAbsolutePath() + " is deleted.");
		    	}		       
		    }
			 
		} else {
			logger.log(Level.SEVERE, "base working space path not found in configuration.");
			throw new ToolJobException("unable to delete working space of tool job.");
		}		
	}

	@Override
	public void extend(String id) {
		// TODO update scheduler
		
	}
	
	/**
	 * Send a message to a queue
	 * @param job
	 * @throws ToolJobException
	 */
	public void send(ToolJob job) throws ToolJobException {
		try {
			if ( job != null ) {
				Message message = context.createMessage();
				message.setStringProperty("id", job.getId());   
				message.setJMSPriority(job.getPriority());
				context.createProducer().send(toolJobQueue, message);    
			}  
		} catch (Exception e) {
			logger.log(Level.SEVERE, "unable to send queue job message", e);
			throw new ToolJobException("unable to send queue job message", e);
		}
	}
	
}
