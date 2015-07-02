package fr.ortolang.diffusion.ftp;

@SuppressWarnings("serial")
public class FtpServiceException extends Exception {

	public FtpServiceException() {
	}

	public FtpServiceException(String message) {
		super(message);
	}

	public FtpServiceException(Throwable cause) {
		super(cause);
	}

	public FtpServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public FtpServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
