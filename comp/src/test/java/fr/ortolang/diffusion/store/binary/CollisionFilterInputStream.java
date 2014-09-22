package fr.ortolang.diffusion.store.binary;

import java.io.InputStream;

import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStream;

public class CollisionFilterInputStream extends HashedFilterInputStream {

	protected CollisionFilterInputStream(InputStream in) {
		super(in);
	}

	@Override
	public String getHash() {
		return "1234567890abcefgh";
	}

}
