package fr.ortolang.diffusion.api.client;

@Deprecated
@SuppressWarnings("serial")
public class OrtolangRestClientException  extends Exception {

	public OrtolangRestClientException() {
		super();
	}

	public OrtolangRestClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public OrtolangRestClientException(String message) {
		super(message);
	}

	public OrtolangRestClientException(Throwable cause) {
		super(cause);
	}

}
