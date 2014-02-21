package fr.ortolang.diffusion;

import java.io.Serializable;

import fr.ortolang.diffusion.registry.entity.RegistryEntry;

@SuppressWarnings("serial")
public class OrtolangObjectState implements Serializable {

	private boolean locked;
	private boolean hidden;
	private boolean deleted;
	
	public OrtolangObjectState(boolean hidden, boolean locked, boolean deleted) {
		this.hidden = hidden;
		this.locked = locked;
		this.deleted = deleted;
	}

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	public static OrtolangObjectState fromEntry(RegistryEntry entry) {
		return new OrtolangObjectState(entry.isHidden(), entry.isLocked(), entry.isDeleted());
	}

}
