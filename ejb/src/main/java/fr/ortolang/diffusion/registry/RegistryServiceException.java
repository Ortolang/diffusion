package fr.ortolang.diffusion.registry;


@SuppressWarnings("serial")
public class RegistryServiceException extends Exception {

	public RegistryServiceException() {
		super();
	}

	public RegistryServiceException(String message) {
		super(message);
	}

	public RegistryServiceException(Throwable cause) {
		super(cause);
	}

	public RegistryServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}