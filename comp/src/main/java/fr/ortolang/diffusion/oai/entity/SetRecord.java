package fr.ortolang.diffusion.oai.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

@Entity
@NamedQueries({
	@NamedQuery(name = "findAllSetRecordsBySet", query = "SELECT r FROM SetRecord r WHERE r.keySet = :keySet") 
})
public class SetRecord {

	@Id
	private String id;
	@Version
	private long version;

	private String keySet;
	private String record;
	
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
	public String getKeySet() {
		return keySet;
	}
	public void setKeySet(String keyset) {
		this.keySet = keyset;
	}
	public String getRecord() {
		return record;
	}
	public void setRecord(String record) {
		this.record = record;
	}
}
