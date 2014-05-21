package fr.ortolang.diffusion.workflow.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.workflow.WorkflowService;

@Entity
@Table(indexes = {@Index(columnList="initier")})
@NamedQueries({
	@NamedQuery(name="findProcessForInitier", query="select r from Process r where r.initier = :initier")
})
@SuppressWarnings("serial")
public class Process extends OrtolangObject {

	public static final String OBJECT_TYPE = "process";
	@Id
	private String id;
	@Transient
	private String key;
	private String name;
	private String type;
	private String initier;
	private String status;
	private String start;
	private String stop;
	@Lob
	private String log;

	public Process() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getInitier() {
		return initier;
	}

	public void setInitier(String initier) {
		this.initier = initier;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStart() {
		return start;
	}

	public void setStart(String start) {
		this.start = start;
	}

	public String getStop() {
		return stop;
	}

	public void setStop(String stop) {
		this.stop = stop;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public void addLogEntry(String entry) {
		this.log += entry + "\r\n";
	}

	@Override
	public String getObjectName() {
		return name;
	}

	@Override
	public String getObjectKey() {
		return key;
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(WorkflowService.SERVICE_NAME, Process.OBJECT_TYPE, id);
	}

}
