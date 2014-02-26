package fr.ortolang.diffusion.indexing;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;

@Local(IndexingService.class)
@Stateless(name = IndexingService.SERVICE_NAME)
public class IndexingServiceBean implements IndexingService {
	
	private Logger logger = Logger.getLogger(IndexingServiceBean.class.getName());
	
	@Resource(mappedName = "jms/topic/indexing")
	private Topic indexingTopic;
	@Inject
	private JMSContext context;
	
	public IndexingServiceBean() {
	}

	@Override
	public void index(String key) throws IndexingServiceException {
		sendMessage("index", key);
	}

	@Override
	public void reindex(String key) throws IndexingServiceException {
		sendMessage("reindex", key);
	}

	@Override
	public void remove(String key) throws IndexingServiceException {
		sendMessage("remove", key);
	}

	private void sendMessage(String action, String key) throws IndexingServiceException {
		try {
			Message message = context.createMessage();
			message.setStringProperty("action", action);
			message.setStringProperty("key", key);
			context.createProducer().send(indexingTopic, message);                   
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable to send indexing message", e);
			throw new IndexingServiceException("unable to send indexing message", e);
		}
	}
}
