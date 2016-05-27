package fr.ortolang.diffusion.api.message;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.message.entity.Message;
import fr.ortolang.diffusion.message.entity.MessageAttachment;

@XmlRootElement(name = "message")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageRepresentation {

    @XmlAttribute(name = "key")
    private String key;
    private String title;
    private String body;
    private Date date;
    private String thread;
    private String parent;
    private String author;
    private Set<MessageAttachment> attachments;

    public MessageRepresentation() {
        attachments = Collections.emptySet();
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Set<MessageAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(Set<MessageAttachment> attachments) {
        this.attachments = attachments;
    }

    public static MessageRepresentation fromMessage(Message message) {
        MessageRepresentation representation = new MessageRepresentation();
        representation.setKey(message.getKey());
        representation.setThread(message.getThread());
        representation.setTitle(message.getTitle());
        representation.setBody(message.getBody());
        representation.setDate(message.getDate());
        representation.setParent(message.getParent());
        representation.setAttachments(message.getAttachments());
        return representation;
    }
    
    public static MessageRepresentation fromMessageAndInfos(Message message, OrtolangObjectInfos infos) {
        MessageRepresentation representation = fromMessage(message);
        representation.setAuthor(infos.getAuthor());
        return representation;
    }

}