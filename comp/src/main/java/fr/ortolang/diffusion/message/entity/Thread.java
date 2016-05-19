package fr.ortolang.diffusion.message.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(indexes={@Index(columnList="lastActivity", name="threadLastActivityIndex"), @Index(columnList="workspace", name="threadWorkspaceIndex")})
@NamedQueries(value= {
        @NamedQuery(name="findThreadsForWorkspace", query="SELECT t FROM Thread t WHERE t.workspace = :wskey ORDER BY t.lastActivity DESC")
})
@SuppressWarnings("serial")
public class Thread extends OrtolangObject {
    
    public static final String OBJECT_TYPE = "thread";
    
    @Id
    private String id;
    @Version
    private long version;
    @Transient
    private String key;
    @Column(length=1000)
    private String name;
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActivity;
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String description;
    private String workspace;
    
    public Thread() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Date lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    @Override
    public String getObjectKey() {
        return getKey();
    }
    
    @Override
    public String getObjectName() {
        return getName();
    }

    @Override
    public OrtolangObjectIdentifier getObjectIdentifier() {
        return new OrtolangObjectIdentifier(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, id);
    }

}
