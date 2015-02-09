package fr.ortolang.diffusion;

import java.io.Serializable;

@SuppressWarnings("serial")
public class OrtolangObjectState implements Serializable {

	private boolean hidden;
	private String lock;
	private String status;
	private long lastModification;

	public OrtolangObjectState() {
	}
	
	public OrtolangObjectState(boolean hidden, String lock, String status, long lastModification) {
		this.hidden = hidden;
		this.lock = lock;
		this.status = status;
		this.lastModification = lastModification;
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
	
	public long getLastModification() {
		return lastModification;
	}

	public void setLastModification(long lastModification) {
		this.lastModification = lastModification;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if ( isLocked() ) {
			buffer.append("locked by (" + getLock() + "), ");
		} else {
			buffer.append("unlocked, ");
		}
		if ( isHidden() ) {
			buffer.append("hidden, ");
		} else {
			buffer.append("visible, ");
		}
		buffer.append("status (" + getStatus() + "), ");
		buffer.append("last modified on " + lastModification);
		return buffer.toString();
	}

	public enum Status {
		DRAFT ("draft"), 
		REVIEW ("review"),
		PUBLISHED ("published");
		
		private String value;
		
		private Status(String value) {
			this.value = value;
		}
		
		public String value() {
			return value;
		}
	}

}