package fr.ortolang.diffusion.runtime.entity;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
import fr.ortolang.diffusion.runtime.process.ProcessState;

@Entity
@Table(indexes = { @Index(columnList = "initier") })
@NamedQueries({ 
	@NamedQuery(name = "findProcessInstancesForInitier", query = "select r from ProcessInstance r where r.initier = :initier"),
	@NamedQuery(name="findAllProcessInstances", query="select p from ProcessInstance p")
	})
@SuppressWarnings("serial")
public class ProcessInstance extends OrtolangObject {

	public static final String OBJECT_TYPE = "process";
	
	public static final String PARAM_START = "start";
	public static final String PARAM_STOP = "stop";
	

	@Id
	private String id;
	@Transient
	private String key;
	private String name;
	private String type;
	private String initier;
	private ProcessState state;
	private int currentStep;
	@ElementCollection(fetch=FetchType.EAGER)
	@Column(length=8000)
	private Map<String, String> params;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String log;

	public ProcessInstance() {
		params = new HashMap<String, String> ();
		log = "";
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

	public ProcessState getState() {
		return state;
	}

	public void setState(ProcessState state) {
		this.state = state;
	}

	public int getCurrentStep() {
		return currentStep;
	}

	public void setCurrentStep(int step) {
		this.currentStep = step;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	
	public void putParam(String key, String value) { 
		this.params.put(key, value);
	}
	
	public String getParam(String key) { 
		return this.params.get(key);
	}
	
	public void putAllParams(Map<String, String> params) {
		this.params.putAll(params);
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
		return new OrtolangObjectIdentifier(RuntimeService.SERVICE_NAME, ProcessInstance.OBJECT_TYPE, id);
	}

}
