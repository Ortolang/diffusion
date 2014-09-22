package fr.ortolang.diffusion.membership;

@SuppressWarnings("serial")
public class MembershipServiceException extends Exception {

	public MembershipServiceException() {
		super();
	}

	public MembershipServiceException(String message) {
		super(message);
	}

	public MembershipServiceException(Throwable cause) {
		super(cause);
	}

	public MembershipServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}