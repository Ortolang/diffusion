package fr.ortolang.diffusion.membership.entity;


public class ProfileInfo {

	private String name;
	private String valuesList;
	private boolean visible;

	public ProfileInfo() {
	}

	public ProfileInfo(String name, String valuesList, boolean visible) {
		this.name = name;
		this.valuesList = valuesList;
		this.visible = visible;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValuesList() {
		return valuesList;
	}

	public void setValuesList(String valuesList) {
		this.valuesList = valuesList;
	}
	
	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

}
