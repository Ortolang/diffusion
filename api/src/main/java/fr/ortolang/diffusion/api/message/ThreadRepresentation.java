package fr.ortolang.diffusion.api.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.message.entity.Thread;

@XmlRootElement(name = "thread")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreadRepresentation {

    @XmlAttribute(name = "key")
    private String key;
    private String author;
    private String name;
    private String description;
    private long creation;
    private long lastActivity;
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

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getCreation() {
        return creation;
    }

    public void setCreation(long creation) {
        this.creation = creation;
    }

    public static ThreadRepresentation fromThread(Thread thread) {
        ThreadRepresentation representation = new ThreadRepresentation();
        representation.setKey(thread.getKey());
        representation.setName(thread.getName());
        representation.setDescription(thread.getDescription());
        representation.setLastActivity(thread.getLastActivity().getTime());
        representation.setWorkspace(thread.getWorkspace());
        return representation;
    }
    
    public static ThreadRepresentation fromThreadAndInfos(Thread thread, OrtolangObjectInfos infos) {
        ThreadRepresentation representation = fromThread(thread);
        representation.setAuthor(infos.getAuthor());
        representation.setCreation(infos.getCreationDate());
        return representation;
    }

}