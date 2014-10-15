package fr.ortolang.diffusion.api.rest.runtime;

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.runtime.entity.ProcessTask;

@XmlRootElement(name = "workflow-task")
public class ProcessTaskRepresentation {

	@XmlAttribute
	private String id;
	private String name;
	private String description;
	private String owner;
	private String assignee;
	private String category;
	private Date creationDate;
	private Date dueDate;
	private int priority;
	private boolean suspended;

	public ProcessTaskRepresentation() {
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getAssignee() {
		return assignee;
	}

	public void setAssignee(String assignee) {
		this.assignee = assignee;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean isSuspended() {
		return suspended;
	}

	public void setSuspended(boolean suspended) {
		this.suspended = suspended;
	}

	public static ProcessTaskRepresentation fromProcessTask(ProcessTask task) {
		ProcessTaskRepresentation representation = new ProcessTaskRepresentation();
		representation.setId(task.getId());
		representation.setName(task.getName());
		representation.setDescription(task.getDescription());
		representation.setOwner(task.getOwner());
		representation.setAssignee(task.getAssignee());
		representation.setCategory(task.getCategory());
		representation.setCreationDate(task.getCreationDate());
		representation.setDueDate(task.getDueDate());
		representation.setPriority(task.getPriority());
		representation.setSuspended(task.isSuspended());
		return representation;
	}

}