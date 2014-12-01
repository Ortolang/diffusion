package fr.ortolang.diffusion.tool.resource;

public class ToolDescription {

	private String name;
	private String description;
	private String documentation;

	public ToolDescription() {
	}

	public ToolDescription(String name, String description, String documentation) {
		this.name = name;
		this.description = description;
		this.documentation = documentation;
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

	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String documentation) {
		this.documentation = documentation;
	}

}
