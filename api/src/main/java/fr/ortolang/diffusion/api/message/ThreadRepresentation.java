package fr.ortolang.diffusion.api.message;

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fr.ortolang.diffusion.message.entity.Thread;

@XmlRootElement(name = "thread")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreadRepresentation {

    @XmlAttribute(name = "key")
    private String key;
    private String name;
    private String description;
    private Date lastActivity;
    private String workspace;

    public ThreadRepresentation() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public static ThreadRepresentation fromThread(Thread thread) {
        ThreadRepresentation representation = new ThreadRepresentation();
        representation.setKey(thread.getKey());
        representation.setName(thread.getName());
        representation.setDescription(thread.getDescription());
        representation.setLastActivity(thread.getLastActivity());
        representation.setWorkspace(thread.getWorkspace());
        return representation;
    }

}