package fr.ortolang.diffusion.api.ssh.shell;

@SuppressWarnings("serial")
public class SSHShellException extends Exception {

	public SSHShellException() {
		super();
	}

	public SSHShellException(String message, Throwable cause) {
		super(message, cause);
	}

	public SSHShellException(String message) {
		super(message);
	}

	public SSHShellException(Throwable cause) {
		super(cause);
	}

}
