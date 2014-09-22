package fr.ortolang.diffusion.runtime.process;

import fr.ortolang.diffusion.runtime.task.Task;

public class ProcessStep {

	private String name;
	private Class<? extends Task> taskClass;

	public ProcessStep() {
	}

	public ProcessStep(String name, Class<? extends Task> taskClass) {
		this.name = name;
		this.taskClass = taskClass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getTaskClass() {
		return taskClass;
	}

	public void setTaskClass(Class<? extends Task> taskClass) {
		this.taskClass = taskClass;
	}

}
