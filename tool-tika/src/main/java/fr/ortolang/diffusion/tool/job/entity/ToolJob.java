package fr.ortolang.diffusion.tool.job.entity;

import java.io.Serializable;

public class ToolJob implements Serializable {
	
	private String id;
	private String name;
	private String priority;
	private ToolJobStatus status;

	public ToolJob() {
	}

}
