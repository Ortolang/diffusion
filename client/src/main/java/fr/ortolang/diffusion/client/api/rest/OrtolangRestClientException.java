package fr.ortolang.diffusion.client.api.rest;

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
