package fr.ortolang.diffusion.membership.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ProfileKey {

	@Column(name="key", length=2500)
	private String key;
	@Column(name="password", length=2500)
	private String password;
	
	public ProfileKey() {
	}

	public ProfileKey(String key, String password) {
		super();
		this.key = key;
		this.password = password;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
