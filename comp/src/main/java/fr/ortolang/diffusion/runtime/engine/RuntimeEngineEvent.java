package fr.ortolang.diffusion.runtime.engine;

public class RuntimeEngineEvent {
	
	public enum Type {
		PROCESS_START,
		PROCESS_ERROR,
		PROCESS_COMPLETE,
		PROCESS_ACTIVITY_STARTED,
		PROCESS_ACTIVITY_STOPPED,
		PROCESS_LOG
	}

	public RuntimeEngineEvent() {
		// TODO Auto-generated constructor stub
	}

}
