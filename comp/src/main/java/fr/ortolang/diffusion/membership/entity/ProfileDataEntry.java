package fr.ortolang.diffusion.membership.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ProfileDataEntry {
	
	public enum ProfileDataEntryType {
		BOOLEAN,
		STRING,
		TEXT,
		EMAIL,
		ADDRESS,
		TEL,
	}

	private String key;
	private ProfileDataEntryType type;
	private boolean visible;
	@Column(length = 7500)
	private String value;

	public ProfileDataEntry() {
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public ProfileDataEntryType getType() {
		return type;
	}

	public void setType(ProfileDataEntryType type) {
		this.type = type;
	}

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
