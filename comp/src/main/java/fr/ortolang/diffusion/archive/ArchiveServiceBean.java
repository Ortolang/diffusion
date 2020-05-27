package fr.ortolang.diffusion.archive;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;

import fr.ortolang.diffusion.archive.exception.ArchiveServiceException;

import org.jboss.ejb3.annotation.SecurityDomain;

@Startup
@Local(ArchiveService.class)
@Singleton(name = ArchiveService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ArchiveServiceBean implements ArchiveService {

    private static final Logger LOGGER = Logger.getLogger(ArchiveServiceBean.class.getName());

    @Resource(mappedName = "java:jboss/exported/jms/topic/archive")
    private Topic archiveQueue;
    @Inject
    private JMSContext context;

    /**
     * Creates a new Archive service.
     */
    public ArchiveServiceBean() {
        // No need to initialize
    }

    public void checkArchivable(String key) throws ArchiveServiceException {
        sendMessage(key);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void sendMessage(String key) throws ArchiveServiceException {
        try {
            Message message = context.createMessage();
            message.setStringProperty("key", key);
            context.createProducer().send(archiveQueue, message);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "unable to send archive message", e);
            throw new ArchiveServiceException("unable to send archive message", e);
        }
    }

    @Override
    public String getServiceName() {
        return ArchiveService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }
    
}