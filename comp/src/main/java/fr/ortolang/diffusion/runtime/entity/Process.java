package fr.ortolang.diffusion.runtime.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.runtime.RuntimeService;

@Entity
@Table(indexes = { @Index(columnList = "initier,state") })
@NamedQueries({ @NamedQuery(name = "findAllProcesses", query = "select p from Process p"),
		@NamedQuery(name = "findProcessByState", query = "select p from Process p where p.state = :state"),
		@NamedQuery(name = "findProcessByInitier", query = "select p from Process p where p.initier = :initier"),
		@NamedQuery(name = "findProcessByIniterAndState", query = "select p from Process p where p.state = :state and p.initier = :initier") })
@SuppressWarnings("serial")
public class Process extends OrtolangObject {

	public static final String OBJECT_TYPE = "process";

	public static enum State {
		PENDING, SUBMITED, RUNNING, SUSPENDED, ABORTED, COMPLETED
	}

	@Id
	private String id;
	@Transient
	private String key;
	private String name;
	private String type;
	private String initier;
	private State state;
	private int progress;
	private String activity;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String log;

	public Process() {
		log = "";
		progress = 0;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInitier() {
		return initier;
	}

	public void setInitier(String initier) {
		this.initier = initier;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public void appendLog(String log) {
		this.log += log + "\r\n";
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}

	@Override
	public String getObjectName() {
		return getName();
	}

	@Override
	public String getObjectKey() {
		return getKey();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, id);
	}
}
