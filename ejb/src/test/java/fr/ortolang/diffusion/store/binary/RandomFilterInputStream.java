package fr.ortolang.diffusion.store.binary;

import java.io.InputStream;
import java.util.UUID;

import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStream;

public class RandomFilterInputStream extends HashedFilterInputStream {

	protected RandomFilterInputStream(InputStream in) {
		super(in);
	}

	@Override
	public String getHash() {
		return UUID.randomUUID().toString();
	}

}