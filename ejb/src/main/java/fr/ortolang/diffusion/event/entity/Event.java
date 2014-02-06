package fr.ortolang.diffusion.event.entity;

import java.io.Serializable;
import java.util.Date;

import fr.ortolang.diffusion.OrtolangEvent;

@SuppressWarnings("serial")
public class Event extends OrtolangEvent implements Serializable {

	private Long id;
	private String fromObject;
	private String throwedBy;
	private String objectType;
	private Date date;
	private String type;
	private String args;

	public Event() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFromObject() {
		return fromObject;
	}

	public void setFromObject(String fromObject) {
		this.fromObject = fromObject;
	}

	public String getThrowedBy() {
		return throwedBy;
	}

	public void setThrowedBy(String throwedBy) {
		this.throwedBy = throwedBy;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getArguments() {
		return args;
	}

	public void setArguments(String args) {
		this.args = args;
	}
}
