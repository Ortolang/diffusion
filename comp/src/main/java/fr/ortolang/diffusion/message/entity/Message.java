package fr.ortolang.diffusion.message.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
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
@Table(indexes={@Index(columnList="date", name="messageDateIndex"), @Index(columnList="thread", name="messageThreadIndex")})
@NamedQueries({
        @NamedQuery(name = "countAllMessages", query = "SELECT count(m) FROM Message m"),
        @NamedQuery(name = "listAllMessages", query = "SELECT m FROM Message m ORDER BY m.date DESC"),
        @NamedQuery(name = "findThreadMessages", query = "SELECT m FROM Message m WHERE m.thread = :thread ORDER BY m.date DESC"),
        @NamedQuery(name = "countThreadMessages", query = "SELECT count(m) FROM Message m WHERE m.thread = :thread"),
        @NamedQuery(name = "deleteThreadMessages", query = "DELETE FROM Message m WHERE m.thread = :thread"),
        @NamedQuery(name = "findThreadMessagesAfterDate", query = "SELECT m FROM Message m WHERE m.thread = :thread AND m.date > :after ORDER BY m.date DESC"),
        @NamedQuery(name = "countThreadMessagesAfterDate", query = "SELECT count(m) FROM Message m WHERE m.thread = :thread AND m.date > :after"),
        @NamedQuery(name = "findMessagesByBinaryHash", query = "SELECT m FROM Message m, in (m.attachments) a WHERE a.hash = :hash")
        })
@SuppressWarnings("serial")
public class Message extends OrtolangObject {

    public static final String OBJECT_TYPE = "message";
    
    @Id
    private String id;
    @Version
    private long version;
    @Transient
    private String key;
    @Column(length=1000)
    private String title;
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String body;
    private String thread;
    private String parent;
    @ElementCollection(fetch=FetchType.EAGER)
    private Set<MessageAttachment> attachments;
    
    public Message() {
        attachments = new HashSet<MessageAttachment> ();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
        for (MessageAttachment attachment : attachments) {
            if ( attachment.getName().equals(name) ) {
                return true;
            }
        }
        return false;
    }
    
    public boolean containsAttachmentHash(String hash) {
        for (MessageAttachment attachment : attachments) {
            if ( attachment.getHash().equals(hash) ) {
                return true;
            }
        }
        return false;
    }
    
    public MessageAttachment findAttachmentByName(String name) {
        for (MessageAttachment attachment : attachments) {
            if ( attachment.getName().equals(name) ) {
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
        return getTitle();
    }

    @Override
    public OrtolangObjectIdentifier getObjectIdentifier() {
        return new OrtolangObjectIdentifier(MessageService.SERVICE_NAME, Message.OBJECT_TYPE, id);
    }
}
