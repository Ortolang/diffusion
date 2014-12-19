package fr.ortolang.diffusion.indexing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;

import org.jboss.ejb3.annotation.SecurityDomain;

@Local(IndexingService.class)
@Stateless(name = IndexingService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class IndexingServiceBean implements IndexingService {
	
	private Logger logger = Logger.getLogger(IndexingServiceBean.class.getName());
	
	@Resource(mappedName = "java:jboss/exported/jms/topic/indexing")
	private Topic indexingTopic;
	@Inject
	private JMSContext context;
	
	public IndexingServiceBean() {
	}

	@Override
	public void index(String key, IndexingContext indexingContext) throws IndexingServiceException {
		sendMessage("index", key, indexingContext);
	}

	@Override
	public void reindex(String key, IndexingContext indexingContext) throws IndexingServiceException {
		sendMessage("reindex", key, indexingContext);
	}

	@Override
	public void remove(String key, IndexingContext indexingContext) throws IndexingServiceException {
		sendMessage("remove", key, indexingContext);
	}

	private void sendMessage(String action, String key, IndexingContext indexingContext) throws IndexingServiceException {
		try {
			Message message = context.createMessage();
			message.setStringProperty("action", action);
			message.setStringProperty("key", key);
			message.setStringProperty("root", indexingContext.getRoot());
			message.setStringProperty("path", indexingContext.getPath());
			message.setStringProperty("name", indexingContext.getName());
			context.createProducer().send(indexingTopic, message);                   
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable to send indexing message", e);
			throw new IndexingServiceException("unable to send indexing message", e);
		}
	}
}
