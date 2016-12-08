package fr.ortolang.diffusion.oai.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Version;

@Entity
@NamedQueries({
	@NamedQuery(name = "listAllSets", query = "SELECT s FROM Set s"),
	@NamedQuery(name = "listAllSetsWithSpec", query = "SELECT s FROM Set s WHERE s.spec = :spec"),
	@NamedQuery(name = "countSets", query = "SELECT COUNT(s) FROM Set s")
})
public class Set {

	@Id
	private String spec;
	@Version
	private long version;

	private String name;
	
	public Set() {
	}
	
	public Set(String spec, String name) {
		this.spec = spec;
		this.name = name;
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

	public String getSpec() {
		return spec;
	}

	public void setSpec(String spec) {
		this.spec = spec;
	}
}
