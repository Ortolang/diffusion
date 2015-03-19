package fr.ortolang.diffusion.membership.entity;

public enum ProfileDataVisibility {
	EVERYBODY (0),
	FRIENDS (1),
	NOBODY (2);
	
	private int value;
	
	private ProfileDataVisibility(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
