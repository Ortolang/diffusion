package fr.ortolang.diffusion.core.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class WorkspaceId {
	
	private static final String WORKSPACE_ID_PREFIX = "ortolang";
	
	@Id
	@SequenceGenerator(name = "WorkspaceIdSequence", sequenceName = "SEQ_WSID_PK", allocationSize=1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="WorkspaceIdSequence")
	private Long id;
	
	public WorkspaceId() {
	}
	
	public Long getId() {
		return id;
	}
	
	public void setLong(Long id) {
		this.id = id;
	}
	
	public String getValue() {
		return WORKSPACE_ID_PREFIX + getId();
	}

}
