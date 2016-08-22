package fr.ortolang.diffusion.message;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2016 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
    private AuthorisationService authorisation;
    @EJB
    private IndexingService indexing;
    @EJB
    private NotificationService notification;
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
    public Thread createThread(String key, String wskey, String title, String body, boolean restricted) throws MessageServiceException, AccessDeniedException,
            KeyAlreadyExistsException {
        LOGGER.log(Level.FINE, "creating thread [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);

            String mkey = UUID.randomUUID().toString();
            
            Thread thread = new Thread();
            thread.setId(UUID.randomUUID().toString());
            thread.setKey(key);
            thread.setTitle(title);
            thread.setWorkspace(wskey);
            thread.setQuestion(mkey);
            thread.setLastActivity(new Date());
            em.persist(thread);
            registry.register(key, thread.getObjectIdentifier(), caller);
            
            Message question = new Message();
            question.setId(UUID.randomUUID().toString());
            question.setBody(body);
            question.setThread(key);
            question.setDate(new Date());
            question.setKey(mkey);
            em.persist(question);
            registry.register(mkey, question.getObjectIdentifier(), caller);
            
            Workspace ws = core.readWorkspace(wskey);
            Map<String, List<String>> trules = new HashMap<String, List<String>>();
            if (!restricted) {
                trules.put(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY, Arrays.asList("read", "post"));
                trules.put(MembershipService.MODERATORS_GROUP_KEY, Arrays.asList("read", "update", "delete", "post"));
            } else {
                trules.put(ws.getMembers(), Arrays.asList("read", "post"));
                trules.put(MembershipService.MODERATORS_GROUP_KEY, Arrays.asList("read", "update", "delete", "post"));
                trules.put(MembershipService.PUBLISHERS_GROUP_KEY, Arrays.asList("read", "post"));
                trules.put(MembershipService.REVIEWERS_GROUP_KEY, Arrays.asList("read", "post"));
            }
            authorisation.createPolicy(key, caller);
            authorisation.setPolicyRules(key, trules);
            
            authorisation.createPolicy(mkey, caller);
            authorisation.setPolicyRules(mkey, trules);

            indexing.index(key);
            indexing.index(mkey);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("wskey", wskey).addArgument("title", title);
            notification.throwEvent(key, caller, Thread.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "create"), argsBuilder.build());

            argsBuilder = new ArgumentsBuilder(2).addArgument("key", key).addArgument("title", title);
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
    public List<Message> browseThread(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "browsing messages for thread: " + key);
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Thread.OBJECT_TYPE);
            authorisation.checkPermission(key, subjects, "read");

            TypedQuery<Message> query = em.createNamedQuery("findThreadMessages", Message.class).setParameter("thread", key);
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

            TypedQuery<Message> query = em.createNamedQuery("findThreadMessagesAfterDate", Message.class).setParameter("thread", key).setParameter("after", date);
            List<Message> messages = query.getResultList();
            loadKeyFromRegistry(messages);
            return messages;
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
    public void updateThread(String key, String title) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "updating thread for key [" + key + "]");
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
            thread.setTitle(title);
            thread.setLastActivity(new Date());
            em.merge(thread);

            registry.update(key);
            indexing.index(key);

            notification.throwEvent(key, caller, Thread.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "update"));
            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("key", key).addArgument("title", title);
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
            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("key", key).addArgument("title", thread.getTitle());
            notification.throwEvent(thread.getWorkspace(), caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "delete"), argsBuilder.build());

        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MessageServiceException("unable to delete thread with key [" + key + "]", e);
        }
    }
    
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void markThreadAsAnswered(String tkey, String mkey) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "marking thread as answered for key [" + tkey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(tkey, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(tkey);
            checkObjectType(identifier, Thread.OBJECT_TYPE);
            Thread thread = em.find(Thread.class, identifier.getId());
            if (thread == null) {
                throw new MessageServiceException("unable to find a thread for id " + identifier.getId());
            }
            
            OrtolangObjectIdentifier midentifier = registry.lookup(mkey);
            checkObjectType(midentifier, Message.OBJECT_TYPE);
            Message message = em.find(Message.class, midentifier.getId());
            if (message == null) {
                throw new MessageServiceException("unable to find a message for id " + midentifier.getId());
            }
            
            thread.setAnswer(mkey);
            thread.setLastActivity(new Date());
            em.merge(thread);
            
            registry.update(tkey);
            indexing.index(tkey);
            
            notification.throwEvent(tkey, caller, Thread.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "answered"));
            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("tkey", tkey).addArgument("title", thread.getTitle());
            notification.throwEvent(thread.getWorkspace(), caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, "answered"), argsBuilder.build());

        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new MessageServiceException("unable to mark thread as answered for thread with key [" + tkey + "]", e);
        }
    }


    /* Message */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Message postMessage(String tkey, String key, String parent, String body) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
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
            message.setBody(body);
            em.persist(message);

            registry.register(key, message.getObjectIdentifier(), caller);

            authorisation.createPolicy(key, caller);
            Map<String, List<String>> mrules = authorisation.getPolicyRules(tkey);
            authorisation.setPolicyRules(key, mrules);

            indexing.index(key);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder().addArgument("key", key).addArgument("body", body).addArgument("thread-title", thread.getTitle());
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
    public void updateMessage(String key, String body) throws MessageServiceException, AccessDeniedException, KeyNotFoundException {
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
                Thread thread = em.find(Thread.class, identifier.getId());
                if (thread == null) {
                    throw new OrtolangException("unable to load thread with id [" + identifier.getId() + "] from storage");
                }
                if (thread.getTitle() != null) {
                    content.setName(thread.getTitle());
                    content.addContentPart(thread.getTitle());
                }
            }

            if (identifier.getType().equals(Message.OBJECT_TYPE)) {
                Message message = em.find(Message.class, identifier.getId());
                if (message == null) {
                    throw new OrtolangException("unable to load message with id [" + identifier.getId() + "] from storage");
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

            if (!identifier.getService().equals(MessageService.SERVICE_NAME)) {
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
                builder.add("body", message.getBody());
                builder.add("thread", message.getThread());
                if (message.getParent() != null) {
                    builder.add("parent", message.getParent());
                }
                JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                for (MessageAttachment attachment : message.getAttachments()) {
                    arrayBuilder.add(attachment.getName());
                }
                builder.add("attachments", arrayBuilder);
                content.put(Message.OBJECT_TYPE, builder.build().toString());
            }

            if (identifier.getType().equals(Thread.OBJECT_TYPE)) {
                Thread thread = em.find(Thread.class, identifier.getId());
                if (thread == null) {
                    throw new OrtolangException("unable to load thread with id [" + identifier.getId() + "] from storage");
                }
                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("key", key);
                builder.add("title", thread.getTitle());
                builder.add("workspace", thread.getWorkspace());
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
                ortolangObjectSize.addElement(Message.OBJECT_TYPE, message.getBody().length());
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
