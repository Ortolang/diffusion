package fr.ortolang.diffusion.message.entity;

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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Type;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.message.MessageService;

@Entity
@Table(indexes = { @Index(columnList = "date", name = "messageDateIndex"), @Index(columnList = "thread", name = "messageThreadIndex") })
@NamedQueries({ @NamedQuery(name = "countAllMessages", query = "SELECT count(m) FROM Message m"), @NamedQuery(name = "listAllMessages", query = "SELECT m FROM Message m ORDER BY m.date DESC"),
        @NamedQuery(name = "findThreadMessages", query = "SELECT m FROM Message m WHERE m.thread = :thread ORDER BY m.date DESC"),
        @NamedQuery(name = "countThreadMessages", query = "SELECT count(m) FROM Message m WHERE m.thread = :thread"),
        @NamedQuery(name = "deleteThreadMessages", query = "DELETE FROM Message m WHERE m.thread = :thread"),
        @NamedQuery(name = "findThreadMessagesAfterDate", query = "SELECT m FROM Message m WHERE m.thread = :thread AND m.date > :after ORDER BY m.date DESC"),
        @NamedQuery(name = "countThreadMessagesAfterDate", query = "SELECT count(m) FROM Message m WHERE m.thread = :thread AND m.date > :after"),
        @NamedQuery(name = "findMessagesByBinaryHash", query = "SELECT m FROM Message m, in (m.attachments) a WHERE a.hash = :hash") })
@SuppressWarnings("serial")
public class Message extends OrtolangObject {

    public static final String OBJECT_TYPE = "message";

    @Id
    private String id;
    @Version
    private long version;
    @Transient
    private String key;
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;
    @Temporal(TemporalType.TIMESTAMP)
    private Date edit;
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String body;
    private String thread;
    private String parent;
    @ElementCollection(fetch = FetchType.EAGER)
    private Set<MessageAttachment> attachments;

    public Message() {
        attachments = new HashSet<MessageAttachment>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getThread() {
        return thread;
    }

    public void setThread(String thread) {
        this.thread = thread;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getEdit() {
        return edit;
    }

    public void setEdit(Date edit) {
        this.edit = edit;
    }

    public Set<MessageAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Set<MessageAttachment> attachments) {
        this.attachments = attachments;
    }

    public boolean addAttachments(MessageAttachment attachment) {
        return attachments.add(attachment);
    }

    public boolean removeAttachment(String name) {
        return attachments.remove(findAttachmentByName(name));
    }

    public boolean containsAttachment(MessageAttachment attachment) {
        return attachments.contains(attachment);
    }

    public boolean containsAttachmentName(String name) {
        return findAttachmentByName(name) != null;
    }

    public boolean containsAttachmentHash(String hash) {
        return findAttachmentByHash(hash) != null;
    }

    public MessageAttachment findAttachmentByName(String name) {
        for (MessageAttachment attachment : attachments) {
            if (attachment.getName().equals(name)) {
                return attachment;
            }
        }
        return null;
    }

    public MessageAttachment findAttachmentByHash(String hash) {
        for (MessageAttachment attachment : attachments) {
            if (attachment.getHash().equals(hash)) {
                return attachment;
            }
        }
        return null;
    }

    @Override
    public String getObjectKey() {
        return getKey();
    }

    @Override
    public String getObjectName() {
        int i;
        if (getBody().length() < 25) {
            i = getBody().length();
        } else {
            for (i = 25; i < Math.min(50, getBody().length()); i++) {
                if (getBody().charAt(i) == ' ') {
                    break;
                }
            }
        }
        return getBody().substring(0, i).replaceAll("\r", "").replace("\n", "");
    }

    @Override
    public OrtolangObjectIdentifier getObjectIdentifier() {
        return new OrtolangObjectIdentifier(MessageService.SERVICE_NAME, Message.OBJECT_TYPE, id);
    }
}
