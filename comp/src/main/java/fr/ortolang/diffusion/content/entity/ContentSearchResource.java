package fr.ortolang.diffusion.content.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

@Entity
@NamedQueries({ 
    @NamedQuery(name = "findResources", query = "SELECT r FROM ContentSearchResource r"),
    @NamedQuery(name = "findResourcesByWorkspace", query = "SELECT r FROM ContentSearchResource r WHERE r.workspace = :workspace"),
    @NamedQuery(name = "countResources", query = "SELECT COUNT(r) FROM ContentSearchResource r")
    })
public class ContentSearchResource {

    @Id
    private String id;
    @Version
    private long version;

    private String workspace;
	private String pid;
	private String title;
	private String description;
	private String landingPageURI;
    @ElementCollection(fetch=FetchType.EAGER)
	private Set<String> documents;
	
	public ContentSearchResource() {
		documents = new HashSet<String>();
	}
	
	public ContentSearchResource(String id, String wskey, String title, String description) {
		this();
		
		this.id = id;
		this.workspace = wskey;
		this.title = title;
		this.description = description;
	}
	
	public boolean addDocument(String doc) {
		return documents.add(doc);
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
	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	public String getPid() {
		return pid;
	}
	public void setPid(String pid) {
		this.pid = pid;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLandingPageURI() {
		return landingPageURI;
	}
	public void setLandingPageURI(String landingPageURI) {
		this.landingPageURI = landingPageURI;
	}

	public Set<String> getDocuments() {
		return documents;
	}

	public void setDocuments(Set<String> documents) {
		this.documents = documents;
	}
	
}
