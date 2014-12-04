package fr.ortolang.diffusion.tool.job.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

import org.hibernate.annotations.Type;


@Entity
@NamedQueries({ 
	@NamedQuery(name = "findAllJobs", query = "select j from ToolJob j") }
)
@SuppressWarnings("serial")
public class ToolJob implements Serializable {
	
	@Id
	private String id;
	@Version
	private long version;
	private String name;	
	@ElementCollection(fetch=FetchType.EAGER)
    @MapKeyColumn(name="key")
    @Column(name="value")
    @CollectionTable(name="parameters")
    Map<String, String> parameters = new HashMap<String, String>();
	private int priority;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String log;
	private ToolJobStatus status;

	public static final String OBJECT_TYPE = "tool job";	
	
	public ToolJob() {
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public ToolJobStatus getStatus() {
		return status;
	}

	public void setStatus(ToolJobStatus status) {
		this.status = status;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}
	
	public String getParameter(String key) {
		return parameters.get(key);
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

}
