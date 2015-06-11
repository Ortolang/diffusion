package fr.ortolang.diffusion.referentiel;

@SuppressWarnings("serial")
public class ReferentielServiceException extends Exception {

	public ReferentielServiceException() {
		super();
	}

	public ReferentielServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public ReferentielServiceException(String message) {
		super(message);
	}

	public ReferentielServiceException(Throwable cause) {
		super(cause);
	}
}
