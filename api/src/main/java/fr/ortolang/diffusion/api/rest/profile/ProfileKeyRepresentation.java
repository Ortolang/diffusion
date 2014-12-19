package fr.ortolang.diffusion.api.rest.profile;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "public-key")
public class ProfileKeyRepresentation {
	
	private String publicKey;

	public String getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
	
}
