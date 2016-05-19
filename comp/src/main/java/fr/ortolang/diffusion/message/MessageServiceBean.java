package fr.ortolang.diffusion.message;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangEvent.ArgumentsBuilder;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.event.EventService;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.message.entity.Message;
import fr.ortolang.diffusion.message.entity.MessageAttachment;
import fr.ortolang.diffusion.message.entity.Thread;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;

@Local(MessageService.class)
@Stateless(name = MessageService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class MessageServiceBean implements MessageService {

    private static final Logger LOGGER = Logger.getLogger(MessageServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { Thread.OBJECT_TYPE, Message.OBJECT_TYPE };
    private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { { Thread.OBJECT_TYPE, "read,update,post,delete" }, { Message.OBJECT_TYPE, "read,update,delete" } };

    @EJB
    private RegistryService registry;
    @EJB
    private BinaryStoreService binarystore;
    @EJB
    private MembershipService membership;
    @EJB
    private CoreService core;
    @EJB
    private EventService events;
    @EJB
    private AuthorisationService authorisation;
    @EJB
    private IndexingService indexing;
    @EJB
    private NotificationService notification;
    @EJB
    private SecurityService security;
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @Resource
    private SessionContext ctx;

    public MessageServiceBean() {
    }

    public RegistryService getRegistryService() {
        return registry;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registry = registryService;
    }

    public BinaryStoreService getBinaryStoreService() {
        return binarystore;
    }

    public void setBinaryStoreService(BinaryStoreService binaryStoreService) {
        this.binarystore = binaryStoreService;
    }

    public NotificationService getNotificationService() {
        return notification;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notification = notificationService;
    }

    public MembershipService getMembershipService() {
        return membership;
    }

    public void setMembershipService(MembershipService membership) {
        this.membership = membership;
    }

    public AuthorisationService getAuthorisationService() {
        return authorisation;
    }

    public CoreService getCoreService() {
        return core;
    }

    public void setCoreService(CoreService core) {
        this.core = core;
    }

    public void setAuthorisationService(AuthorisationService authorisation) {
        this.authorisation = authorisation;
    }

    public IndexingService getIndexingService() {
        return indexing;
    }

    public void setIndexingService(IndexingService indexing) {
        this.indexing = indexing;
    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEntityManager() {
        return this.em;
    }

    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext() {
        return this.ctx;
    }

    /* Thread */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Thread createThread(String key, String wskey, String name, String description, boolean restricted) throws MessageServiceException, AccessDeniedException,
            KeyAlreadyExistsException {
        LOGGER.log(Level.FINE, "creating thread [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);

            String id = UUID.randomUUID().toString();
            Thread thread = new Thread();
            thread.setId(id);
            thread.setKey(key);
            thread.setName(name);
            thread.setDescription(description);
            thread.setWorkspace(wskey);
            thread.setLastActivity(new Date());
            em.persist(thread);

            registry.register(key, thread.getObjectIdentifier(), caller);

            Workspace ws = core.readWorkspace(wskey);
            Map<String, List<String>> trules = new HashMap<String, List<String>>();
            if (!restricted) {
                trules.put(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY, Arrays.asList("read", "post"));
            } else {
                trules.put(ws.getMembers(), Arrays.asList("read", "post"));
                trules.put(MembershipService.MODERATOR_GROUP_KEY, Arrays.asList("read", "post"));
            }
            authorisation.createPolicy(key, caller);
            authorisation.setPolicyRules(key, trules);

            indexing.index(key);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("wskey", wskey).addArgument("name", name);
            notification.throwEvent(key, caller, Thread.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "create"), argsBuilder.build());
            
            argsBuilder = new ArgumentsBuilder(2).addArgument("key", key).addArgument("name", name);
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "create"), argsBuilder.build());
            
            return thread;
        } catch (KeyAlreadyExistsException e) {
            ctx.setRollbackOnly();
            throw e;
        } catch (KeyNotFoundException | AuthorisationServiceException | MembershipServiceException | NotificationServiceException | IndexingServiceException | RegistryServiceException
                | IdentifierAlreadyRegisteredException | CoreServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while creating thread", e);
            throw new MessageServiceException("unable to create thread with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Thread readThread(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "reading thread [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Thread.OBJECT_TYPE);
            authorisation.checkPermission(key, subjects, "read");

            Thread thread = em.find(Thread.class, identifier.getId());
            if (thread == null) {
                throw new MessageServiceException("unable to load thread with id [" + identifier.getId() + "] from storage");
            }
            thread.setKey(key);
            return thread;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while reading thread", e);
            throw new MessageServiceException("unable to read thread with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> findThreadsForWorkspace(String wskey) throws MessageServiceException, AccessDeniedException {
        LOGGER.log(Level.FINE, "finding threads for workspace [" + wskey + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);

            List<String> keys = new ArrayList<String>();
            TypedQuery<Thread> query = em.createNamedQuery("findThreadsForWorkspace", Thread.class).setParameter("wskey", wskey);
            List<Thread> threads = query.getResultList();
            for (Thread thread : threads) {
                OrtolangObjectIdentifier identifier = thread.getObjectIdentifier();
                try {
                    keys.add(registry.lookup(identifier));
                } catch (IdentifierNotRegisteredException e) {
                    LOGGER.log(Level.FINE, "a thread with an unregistered identifier has be found (probably deleted) : " + identifier);
                }
            }

            return keys;
        } catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException | KeyNotFoundException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during finding threads for workspace: " + wskey, e);
            throw new MessageServiceException("unable to find threads for workspace: " + wskey, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Message> browseThread(String key, int offset, int limit) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "browsing messages for thread: " + key);
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Thread.OBJECT_TYPE);
            authorisation.checkPermission(key, subjects, "read");
            
            TypedQuery<Message> query = em.createNamedQuery("findThreadMessages", Message.class).setParameter("thread", key).setFirstResult(offset).setMaxResults(limit);
            List<Message> msgs = query.getResultList();
            loadKeyFromRegistry(msgs);
            return msgs;
        } catch (MembershipServiceException | KeyNotFoundException | AuthorisationServiceException | RegistryServiceException e) {
            throw new MessageServiceException("unable to browse messages", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Message> browseThreadSinceDate(String key, Date date) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "browsing messages for thread:" + key + " since date: " + date);
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Thread.OBJECT_TYPE);
            authorisation.checkPermission(key, subjects, "read");
            
            TypedQuery<Message> query = em.createNamedQuery("findThreadMessagesSinceDate", Message.class).setParameter("thread", key).setParameter("date", date);
            List<Message> msgs = query.getResultList();
            loadKeyFromRegistry(msgs);
            return msgs;
        } catch (MembershipServiceException | KeyNotFoundException | AuthorisationServiceException | RegistryServiceException e) {
            throw new MessageServiceException("unable to browse messages", e);
        }
    }
    
    private void loadKeyFromRegistry(List<Message> msgs) throws RegistryServiceException {
        ListIterator<Message> iter = msgs.listIterator();
        while(iter.hasNext()){
            Message msg = iter.next();
            try {
                String mkey = registry.lookup(msg.getObjectIdentifier());
                msg.setKey(mkey);
            } catch ( IdentifierNotRegisteredException e ) {
                LOGGER.log(Level.WARNING, "found a message that is not bound in registry, maybe orphean, should clean it !!, identifier: " + msg.getObjectIdentifier() );                    
                iter.remove();
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateThread(String key, String name, String description) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "updating thread for key [" + key + "] and name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Thread.OBJECT_TYPE);
            Thread thread = em.find(Thread.class, identifier.getId());
            if (thread == null) {
                throw new MessageServiceException("unable to find a thread for id " + identifier.getId());
            }
            thread.setName(name);
            thread.setDescription(description);
            thread.setLastActivity(new Date());
            em.merge(thread);

            registry.update(key);
            indexing.index(key);

            notification.throwEvent(key, caller, Thread.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "update"));
            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("key", key).addArgument("name", name);
            notification.throwEvent(thread.getWorkspace(), caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "update"), argsBuilder.build());

        } catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | KeyLockedException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MessageServiceException("unable to update thread with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteThread(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "deleting thread for key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "delete");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Thread.OBJECT_TYPE);
            Thread thread = em.find(Thread.class, identifier.getId());
            if (thread == null) {
                throw new MessageServiceException("unable to find a thread for id " + identifier.getId());
            }
            em.remove(thread);
            registry.delete(key);
            indexing.remove(key);
            
            List<Message> msgs = em.createNamedQuery("findThreadMessages", Message.class).setParameter("thread", key).getResultList();
            for ( Message msg : msgs ) {
                try {
                    String mkey = registry.lookup(msg.getObjectIdentifier());
                    registry.delete(mkey);
                    indexing.remove(mkey);
                    notification.throwEvent(mkey, caller, Message.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Message.OBJECT_TYPE, "delete"));
                } catch ( IdentifierNotRegisteredException e ) {
                    LOGGER.log(Level.FINE, "found message that does not exists in registry for identifier [" + msg.getObjectIdentifier() + "]");
                }
            }
            em.createNamedQuery("deleteThreadMessages", Message.class).setParameter("thread", key).executeUpdate();
            
            notification.throwEvent(key, caller, Thread.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "delete"));
            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("key", key).addArgument("name", thread.getName());
            notification.throwEvent(thread.getWorkspace(), caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "delete"), argsBuilder.build());

        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MessageServiceException("unable to delete thread with key [" + key + "]", e);
        }
    }

    /* Message */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Message postMessage(String tkey, String key, String parent, String title, String body) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "posting message into thread with key [" + tkey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(tkey);
            checkObjectType(identifier, Thread.OBJECT_TYPE);
            Thread thread = em.find(Thread.class, identifier.getId());
            if (thread == null) {
                throw new MessageServiceException("unable to find a thread for id " + identifier.getId());
            }
            authorisation.checkPermission(tkey, subjects, "post");
            thread.setLastActivity(new Date());
            em.merge(thread);
            registry.update(tkey);
            
            if (parent != null && parent.length() > 0) {
                OrtolangObjectIdentifier pidentifier = registry.lookup(parent);
                checkObjectType(pidentifier, Message.OBJECT_TYPE);
            }

            String id = UUID.randomUUID().toString();
            Message message = new Message();
            message.setId(id);
            message.setKey(key);
            message.setDate(new Date());
            message.setThread(tkey);
            if (parent != null && parent.length() > 0) {
                message.setParent(parent);
            }
            message.setTitle(title);
            message.setBody(body);
            em.persist(message);

            registry.register(key, message.getObjectIdentifier(), caller);

            authorisation.createPolicy(key, caller);
            Map<String, List<String>> mrules = authorisation.getPolicyRules(tkey);
            authorisation.setPolicyRules(key, mrules);

            indexing.index(key);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(4).addArgument("key", key).addArgument("title", title).addArgument("body", body);
            if (parent != null && parent.length() > 0) {
                argsBuilder.addArgument("parent", parent);
            }
            notification.throwEvent(tkey, caller, Thread.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "post"), argsBuilder.build());
            notification.throwEvent(thread.getWorkspace(), caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "post"), argsBuilder.build());
            notification.throwEvent(key, caller, Message.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Message.OBJECT_TYPE, "create"), null);
            
            return message;
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | KeyAlreadyExistsException
                | IdentifierAlreadyRegisteredException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MessageServiceException("unable to post message in thread with key [" + tkey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Message readMessage(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "reading message [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Message.OBJECT_TYPE);
            authorisation.checkPermission(key, subjects, "read");

            Message message = em.find(Message.class, identifier.getId());
            if (message == null) {
                throw new MessageServiceException("unable to load message with id [" + identifier.getId() + "] from storage");
            }
            message.setKey(key);
            return message;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while reading message", e);
            throw new MessageServiceException("unable to read message with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateMessage(String key, String title, String body) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "updating message for key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Message.OBJECT_TYPE);
            Message message = em.find(Message.class, identifier.getId());
            if (message == null) {
                throw new MessageServiceException("unable to find a message for id " + identifier.getId());
            }
            message.setTitle(title);
            message.setBody(body);
            em.merge(message);

            registry.update(key);
            indexing.index(key);
            
            notification.throwEvent(key, caller, Message.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Message.OBJECT_TYPE, "update"));
        } catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | KeyLockedException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MessageServiceException("unable to update message with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteMessage(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "deleting message for key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "delete");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Message.OBJECT_TYPE);
            Message message = em.find(Message.class, identifier.getId());
            if (message == null) {
                throw new MessageServiceException("unable to load message with id [" + identifier.getId() + "] from storage");
            }
            em.remove(message);
            registry.delete(key);
            indexing.remove(key);

            notification.throwEvent(key, caller, Message.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Message.OBJECT_TYPE, "delete"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MessageServiceException("unable to delete message with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addMessageAttachment(String key, String name, InputStream data) throws MessageServiceException, AccessDeniedException, KeyNotFoundException, DataCollisionException {
        LOGGER.log(Level.FINE, "adding attachment to message with key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Message.OBJECT_TYPE);
            Message message = em.find(Message.class, identifier.getId());
            if (message == null) {
                throw new MessageServiceException("unable to find a message for id " + identifier.getId());
            }
            String hash = binarystore.put(data);
            String type = binarystore.type(hash, name);
            long size = binarystore.size(hash);
            message.addAttachments(new MessageAttachment(name, type, size, hash));
            em.merge(message);

            registry.update(key);
            indexing.index(key);
            notification.throwEvent(key, caller, Message.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Message.OBJECT_TYPE, "add-attachment"));
        } catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | KeyLockedException | BinaryStoreServiceException | IndexingServiceException | DataNotFoundException e) {
            ctx.setRollbackOnly();
            throw new MessageServiceException("unable to add attachment to message with key [" + key + "]", e);
        }
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeMessageAttachment(String key, String name) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "removing attachment to message with key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Message.OBJECT_TYPE);
            Message message = em.find(Message.class, identifier.getId());
            if (message == null) {
                throw new MessageServiceException("unable to find a message for id " + identifier.getId());
            }
            if ( !message.containsAttachmentName(name) ) {
                ctx.setRollbackOnly();
                throw new MessageServiceException("no attachment found with name [" + name + "] for message with key [" + key + "]");
            }
            message.removeAttachment(name);
            em.merge(message);

            registry.update(key);
            indexing.index(key);
            notification.throwEvent(key, caller, Message.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Message.OBJECT_TYPE, "remove-attachment"));
        } catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | KeyLockedException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MessageServiceException("unable to remove attachment from message with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public File getMessageAttachment(String key, String name) throws MessageServiceException, AccessDeniedException, KeyNotFoundException, DataNotFoundException {
        LOGGER.log(Level.FINE, "getting attachment of message with key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Message.OBJECT_TYPE);
            Message message = em.find(Message.class, identifier.getId());
            if (message == null) {
                throw new MessageServiceException("unable to find a message for id " + identifier.getId());
            }
            if ( !message.containsAttachmentName(name) ) {
                throw new MessageServiceException("no attachment found with name [" + name + "] for message with key: " + key);
            }
            
            notification.throwEvent(key, caller, Message.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Message.OBJECT_TYPE, "download-attachment"));
            return binarystore.getFile(message.findAttachmentByName(name).getHash());
        } catch (NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException | BinaryStoreServiceException e) {
            throw new MessageServiceException("unable to get attachment from message with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IndexablePlainTextContent getIndexablePlainTextContent(String key) throws OrtolangException, NotIndexableContentException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(MessageService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }
            IndexablePlainTextContent content = new IndexablePlainTextContent();
            
            if (identifier.getType().equals(Thread.OBJECT_TYPE)) {
                Thread mf = em.find(Thread.class, identifier.getId());
                if (mf == null) {
                    throw new OrtolangException("unable to load thread with id [" + identifier.getId() + "] from storage");
                }
                if (mf.getName() != null) {
                    content.setName(mf.getName());
                    content.addContentPart(mf.getName());
                }
                if (mf.getDescription() != null) {
                    content.addContentPart(mf.getDescription());
                }
            }
            
            if (identifier.getType().equals(Message.OBJECT_TYPE)) {
                Message message = em.find(Message.class, identifier.getId());
                if (message == null) {
                    throw new OrtolangException("unable to load message with id [" + identifier.getId() + "] from storage");
                }
                if (message.getTitle() != null) {
                    content.setName(message.getTitle());
                    content.addContentPart(message.getTitle());
                }
                if (message.getBody() != null && message.getBody().length() > 0) {
                    content.addContentPart(message.getBody());
                }
            }

            return content;
        } catch (KeyNotFoundException | RegistryServiceException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IndexableJsonContent getIndexableJsonContent(String key) throws OrtolangException, NotIndexableContentException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);

            if (!identifier.getService().equals(MembershipService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            IndexableJsonContent content = new IndexableJsonContent();

            if (identifier.getType().equals(Message.OBJECT_TYPE)) {
                Message message = em.find(Message.class, identifier.getId());
                if (message == null) {
                    throw new OrtolangException("unable to load message with id [" + identifier.getId() + "] from storage");
                }
                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("key", key);
                builder.add("title", message.getTitle());
                builder.add("body", message.getBody());
                builder.add("feed", message.getThread());
                builder.add("parent", message.getParent());
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (MessageAttachment attachment : message.getAttachments()) {
                    arrayBuilder.add(attachment.getName());
                }
                builder.add("attachments", arrayBuilder);
                content.put(Message.OBJECT_TYPE, builder.build().toString());
            }

            if (identifier.getType().equals(Thread.OBJECT_TYPE)) {
                Thread mf = em.find(Thread.class, identifier.getId());
                if (mf == null) {
                    throw new OrtolangException("unable to load thread with id [" + identifier.getId() + "] from storage");
                }
                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("key", key);
                builder.add("name", mf.getName());
                builder.add("description", mf.getName());
                builder.add("workspace", mf.getWorkspace());
                content.put(Thread.OBJECT_TYPE, builder.build().toString());
            }
            return content;
        } catch (KeyNotFoundException | RegistryServiceException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String>();
        try {
            infos.put(INFO_MESSAGE_FEED_ALL, Long.toString(registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE), null)));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_MESSAGE_FEED_ALL, e);
        }
        try {
            infos.put(INFO_MESSAGE_ALL, Long.toString(registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(MessageService.SERVICE_NAME, Message.OBJECT_TYPE), null)));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_MESSAGE_ALL, e);
        }
        return infos;
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        for (int i = 0; i < OBJECT_PERMISSIONS_LIST.length; i++) {
            if (OBJECT_PERMISSIONS_LIST[i][0].equals(type)) {
                return OBJECT_PERMISSIONS_LIST[i][1].split(",");
            }
        }
        throw new OrtolangException("Unable to find object permissions list for object type : " + type);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OrtolangObject findObject(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);

            if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            switch (identifier.getType()) {
            case Thread.OBJECT_TYPE:
                return readThread(key);
            case Message.OBJECT_TYPE:
                return readMessage(key);
            }

            throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
        } catch (MessageServiceException | RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OrtolangObject> findObjectByBinaryHash(String hash) throws OrtolangException {
        LOGGER.log(Level.FINE, "finding objects that use the binary hash: [" + hash + "]");
        try {
            TypedQuery<Message> query = em.createNamedQuery("findMessagesByBinaryHash", Message.class).setParameter("hash", hash);
            List<Message> messages = query.getResultList();
            for (Message message : messages) {
                message.setKey(registry.lookup(message.getObjectIdentifier()));
            }
            List<OrtolangObject> oobjects = new ArrayList<OrtolangObject>();
            oobjects.addAll(messages);
            return oobjects;
        } catch (Exception e) {
            throw new OrtolangException("unable to find an object for hash " + hash);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        LOGGER.log(Level.FINE, "calculating size for object with key [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            OrtolangObjectIdentifier midentifier = registry.lookup(key);
            if (!midentifier.getService().equals(MessageService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + midentifier + " does not refer to service " + getServiceName());
            }
            OrtolangObjectSize ortolangObjectSize = new OrtolangObjectSize();
            switch (midentifier.getType()) {
                case Thread.OBJECT_TYPE: {
                    authorisation.checkPermission(key, subjects, "read");
                    TypedQuery<Long> query = em.createNamedQuery("countThreadMessages", Long.class).setParameter("thread", key);
                    ortolangObjectSize.addElement(Thread.OBJECT_TYPE, query.getSingleResult());
                    break;
                }
                case Message.OBJECT_TYPE: {
                    Message message = em.find(Message.class, midentifier.getId());
                    ortolangObjectSize.addElement(Message.OBJECT_TYPE, message.getTitle().length() + message.getBody().length());
                    break;
                }
            }
            return ortolangObjectSize;
        } catch (MembershipServiceException | RegistryServiceException | AuthorisationServiceException | AccessDeniedException | KeyNotFoundException e) {
            LOGGER.log(Level.SEVERE, "unexpected error while calculating object size", e);
            throw new OrtolangException("unable to calculate size for object with key [" + key + "]", e);
        }
    }

    private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws MessageServiceException {
        if (!identifier.getService().equals(getServiceName())) {
            throw new MessageServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
        }

        if (!identifier.getType().equals(objectType)) {
            throw new MessageServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
        }
    }

}
