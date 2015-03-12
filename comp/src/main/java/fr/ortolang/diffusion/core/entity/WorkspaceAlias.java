package fr.ortolang.diffusion.core.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class WorkspaceAlias {
	
	private static final String WORKSPACE_ID_PREFIX = "ortolang";
	
	@Id
	@SequenceGenerator(name = "WorkspaceIdSequence", sequenceName = "SEQ_WSALIAS_PK", allocationSize=1)
	@GeneratedValue(strategy=GenerationType.SEQUENCE, generator="WorkspaceIdSequence")
	private Long id;
	
	public WorkspaceAlias() {
	}
	
	public Long getId() {
		return id;
	}
	
	public String getValue() {
		return WORKSPACE_ID_PREFIX + getId();
	}

}
