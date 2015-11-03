package fr.ortolang.diffusion.template;

@SuppressWarnings("serial")
public class TemplateEngineException extends Exception {

	public TemplateEngineException() {
	}

	public TemplateEngineException(String message) {
		super(message);
	}

	public TemplateEngineException(Throwable cause) {
		super(cause);
	}

	public TemplateEngineException(String message, Throwable cause) {
		super(message, cause);
	}

	public TemplateEngineException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
