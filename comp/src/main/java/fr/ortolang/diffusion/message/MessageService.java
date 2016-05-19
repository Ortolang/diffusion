package fr.ortolang.diffusion.message;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import fr.ortolang.diffusion.OrtolangBinaryService;
import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.message.entity.Message;
import fr.ortolang.diffusion.message.entity.Thread;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

public interface MessageService extends OrtolangService, OrtolangBinaryService, OrtolangIndexableService {
    
    String SERVICE_NAME = "message";
    
    String INFO_MESSAGE_FEED_ALL = "threads.all";
    String INFO_MESSAGE_ALL = "messages.all";
    
    Thread createThread(String key, String wskey, String name, String description, boolean restricted) throws MessageServiceException, AccessDeniedException, KeyAlreadyExistsException;
    
    Thread readThread(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    List<String> findThreadsForWorkspace(String wskey) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    List<Message> browseThread(String key, int offset, int limit) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    List<Message> browseThreadSinceDate(String key, Date from) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void updateThread(String key, String name, String description) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void deleteThread(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    Message postMessage(String mfkey, String key, String parent, String title, String body) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    Message readMessage(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void updateMessage(String key, String title, String body) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void deleteMessage(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void addMessageAttachment(String key, String name, InputStream data) throws MessageServiceException, AccessDeniedException, KeyNotFoundException, DataCollisionException;
    
    void removeMessageAttachment(String key, String name) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    File getMessageAttachment(String key, String name) throws MessageServiceException, AccessDeniedException, KeyNotFoundException, DataNotFoundException;
    
}
