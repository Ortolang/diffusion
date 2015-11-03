package fr.ortolang.diffusion.core;

@SuppressWarnings("serial")
public class WorkspaceLockedException extends Exception {

    public WorkspaceLockedException() {
        super();
    }

    public WorkspaceLockedException(String message) {
        super(message);
    }

    public WorkspaceLockedException(Throwable cause) {
        super(cause);
    }

    public WorkspaceLockedException(String message, Throwable cause) {
        super(message, cause);
    }

}
