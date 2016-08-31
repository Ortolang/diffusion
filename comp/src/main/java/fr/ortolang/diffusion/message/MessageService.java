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
import java.util.Date;
import java.util.List;
import java.util.Map;

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
    
    Thread createThread(String key, String wskey, String title, String body, boolean restricted, Map<String, InputStream> attachments) throws MessageServiceException, AccessDeniedException, KeyAlreadyExistsException;
    
    Thread readThread(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    List<String> findThreadsForWorkspace(String wskey) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    List<Message> browseThread(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    List<Message> browseThreadSinceDate(String key, Date from) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void updateThread(String key, String title, String answer) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void deleteThread(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void markThreadAsAnswered(String tkey, String akey) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    Message postMessage(String tkey, String key, String parent, String body) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;

    Message postMessage(String tkey, String key, String parent, String body, Map<String, InputStream> attachments) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;

    Message readMessage(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void updateMessage(String key, String body) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;

    void updateMessage(String key, String body, Map<String, InputStream> attachments, String[] removedAttachments) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;

    void deleteMessage(String key) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    void addMessageAttachment(String key, String name, InputStream data) throws MessageServiceException, AccessDeniedException, KeyNotFoundException, DataCollisionException;
    
    void removeMessageAttachment(String key, String name) throws MessageServiceException, AccessDeniedException, KeyNotFoundException;
    
    File getMessageAttachment(String key, String name) throws MessageServiceException, AccessDeniedException, KeyNotFoundException, DataNotFoundException;
    
}
