package fr.ortolang.diffusion.registry;

@SuppressWarnings("serial")
public class BranchNotAllowedException extends Exception {

	public BranchNotAllowedException() {
		super();
	}

	public BranchNotAllowedException(String message) {
		super(message);
	}

	public BranchNotAllowedException(Throwable cause) {
		super(cause);
	}

	public BranchNotAllowedException(String message, Throwable cause) {
		super(message, cause);
	}

	public BranchNotAllowedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
