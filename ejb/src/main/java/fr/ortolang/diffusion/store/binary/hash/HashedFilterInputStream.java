package fr.ortolang.diffusion.store.binary.hash;

import java.io.FilterInputStream;
import java.io.InputStream;

public abstract class HashedFilterInputStream extends FilterInputStream {

	protected HashedFilterInputStream(InputStream in) {
		super(in);
	}
	
	public abstract String getHash();
	
	

}
