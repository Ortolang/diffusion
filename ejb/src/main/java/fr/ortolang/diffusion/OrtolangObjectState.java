package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangObjectState implements Serializable {

	private boolean deleted;
	private boolean hidden;
	private String lock;
	private String status;

	public OrtolangObjectState() {
	}
	
	public OrtolangObjectState(boolean deleted, boolean hidden, String lock, String status) {
		this.deleted = deleted;
		this.hidden = hidden;
		this.lock = lock;
		this.status = status;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public String getLock() {
		return lock;
	}

	public void setLock(String lock) {
		this.lock = lock;
	}

	public boolean isLocked() {
		return lock != null && lock.length() > 0;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if ( isLocked() ) {
			buffer.append("LOCKED by (" + getLock() + "), ");
		} else {
			buffer.append("UNLOCKED, ");
		}
		if ( isHidden() ) {
			buffer.append("HIDDEN, ");
		} else {
			buffer.append("VISIBLE, ");
		}
		if ( isDeleted() ) {
			buffer.append("DELETED, ");
		} else {
			buffer.append("ACTIVE, ");
		}
		buffer.append("STATUS (" + getStatus() + ")");
		return buffer.toString();
	}

	public enum Status {
		DRAFT, WAITING, PUBLISHED
	}

}