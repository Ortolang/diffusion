package fr.ortolang.diffusion.membership.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;

@Embeddable
public class ProfileData {

	private String name;
	@ElementCollection
	private List<ProfileDataEntry> entries = new ArrayList<ProfileDataEntry> ();
	private boolean visible;

	public ProfileData() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public List<ProfileDataEntry> getEntries() {
		return entries;
	}

	public void setEntries(List<ProfileDataEntry> entries) {
		this.entries = entries;
	}
	
}
