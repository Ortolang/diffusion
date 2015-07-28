package fr.ortolang.diffusion.store.handle.entity;

import java.io.Serializable;
import java.util.Arrays;

@SuppressWarnings("serial")
public class HandlePK implements Serializable {
	
	private byte[] handle;
	private int index;
	
	public HandlePK() {
	}

	public HandlePK(byte[] handle, int index) {
		super();
		this.handle = handle;
		this.index = index;
	}

	public byte[] getHandle() {
		return handle;
	}

	public void setHandle(byte[] handle) {
		this.handle = handle;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(handle);
		result = prime * result + index;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HandlePK other = (HandlePK) obj;
		if (!Arrays.equals(handle, other.handle))
			return false;
		if (index != other.index)
			return false;
		return true;
	}
	
	

}
