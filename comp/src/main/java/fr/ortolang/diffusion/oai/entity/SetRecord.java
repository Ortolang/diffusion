package fr.ortolang.diffusion.oai.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

@Entity
@NamedQueries({
	@NamedQuery(name = "findAllSetRecordsBySetSpec", query = "SELECT r FROM SetRecord r WHERE r.setSpec = :setSpec") 
})
public class SetRecord {

	@Id
	private String id;
	@Version
	private long version;

	private String setSpec;
	private String recordId;
	
	public SetRecord(String id, String setSpec, String recordId) {
		super();
		this.id = id;
		this.setSpec = setSpec;
		this.setRecordId(recordId);
	}
	public SetRecord() {
		super();
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
	public String getSetSpec() {
		return setSpec;
	}
	public void setSetSpec(String setSpec) {
		this.setSpec = setSpec;
	}
	public String getRecordId() {
		return recordId;
	}
	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}
}
