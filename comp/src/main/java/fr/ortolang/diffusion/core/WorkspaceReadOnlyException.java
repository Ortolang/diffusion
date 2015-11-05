package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class WorkspaceReadOnlyException extends Exception {

    public WorkspaceReadOnlyException() {
        super();
    }

    public WorkspaceReadOnlyException(String message) {
        super(message);
    }

    public WorkspaceReadOnlyException(Throwable cause) {
        super(cause);
    }

    public WorkspaceReadOnlyException(String message, Throwable cause) {
        super(message, cause);
    }

}
