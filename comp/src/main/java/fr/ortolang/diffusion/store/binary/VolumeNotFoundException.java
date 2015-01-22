package fr.ortolang.diffusion.store.binary;

@SuppressWarnings("serial")
public class VolumeNotFoundException extends Exception {

	public VolumeNotFoundException() {
		super();
	}

	public VolumeNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public VolumeNotFoundException(String message) {
		super(message);
	}

	public VolumeNotFoundException(Throwable cause) {
		super(cause);
	}

}
