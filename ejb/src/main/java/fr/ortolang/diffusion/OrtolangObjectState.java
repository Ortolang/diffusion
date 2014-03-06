package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangObjectState implements Serializable {

	private boolean locked;
	
	public OrtolangObjectState() {
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	@Override 
	public String toString() {
		if ( locked ) {
			return "LOCKED";
		}
		return "UNLOCKED";
	}

}
