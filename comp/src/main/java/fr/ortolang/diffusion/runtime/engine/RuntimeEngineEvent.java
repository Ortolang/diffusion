package fr.ortolang.diffusion.runtime.engine;

import java.io.Serializable;

@SuppressWarnings("serial")
public class RuntimeEngineEvent implements Serializable {
	
	public static final String MESSAGE_PROPERTY_NAME = "event_object";

	public enum Type {
		PROCESS_START, PROCESS_ABORT, PROCESS_COMPLETE, PROCESS_ACTIVITY_STARTED, PROCESS_ACTIVITY_PROGRESS, PROCESS_ACTIVITY_COMPLETED, PROCESS_ACTIVITY_ERROR, PROCESS_LOG
	}

	private long timestamp;
	private Type type;
	private String pid;
	private String activityName;
	private int activityProgress;
	private String message;

	private RuntimeEngineEvent() {
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getActivityName() {
		return activityName;
	}

	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}

	public int getActivityProgress() {
		return activityProgress;
	}

	public void setActivityProgress(int activityProgress) {
		this.activityProgress = activityProgress;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public static RuntimeEngineEvent createProcessStartEvent(String pid) {
		RuntimeEngineEvent event = new RuntimeEngineEvent();
		event.setTimestamp(System.currentTimeMillis());
		event.setType(Type.PROCESS_START);
		event.setPid(pid);
		return event;
	}
	
	public static RuntimeEngineEvent createProcessAbortEvent(String pid, String message) {
		RuntimeEngineEvent event = new RuntimeEngineEvent();
		event.setTimestamp(System.currentTimeMillis());
		event.setType(Type.PROCESS_ABORT);
		event.setPid(pid);
		event.setMessage(message);
		return event;
	}
	
	public static RuntimeEngineEvent createProcessCompleteEvent(String pid) {
		RuntimeEngineEvent event = new RuntimeEngineEvent();
		event.setTimestamp(System.currentTimeMillis());
		event.setType(Type.PROCESS_COMPLETE);
		event.setPid(pid);
		return event;
	}
	
	public static RuntimeEngineEvent createProcessLogEvent(String pid, String message) {
		RuntimeEngineEvent event = new RuntimeEngineEvent();
		event.setTimestamp(System.currentTimeMillis());
		event.setType(Type.PROCESS_LOG);
		event.setPid(pid);
		event.setMessage(message);
		return event;
	}
	
	public static RuntimeEngineEvent createProcessActivityStartEvent(String pid, String name, String message) {
		RuntimeEngineEvent event = new RuntimeEngineEvent();
		event.setTimestamp(System.currentTimeMillis());
		event.setType(Type.PROCESS_ACTIVITY_STARTED);
		event.setPid(pid);
		event.setActivityName(name);
		event.setActivityProgress(0);
		event.setMessage(message);
		return event;
	}
	
	public static RuntimeEngineEvent createProcessActivityCompleteEvent(String pid, String name, String message) {
		RuntimeEngineEvent event = new RuntimeEngineEvent();
		event.setTimestamp(System.currentTimeMillis());
		event.setType(Type.PROCESS_ACTIVITY_COMPLETED);
		event.setPid(pid);
		event.setActivityName(name);
		event.setActivityProgress(100);
		event.setMessage(message);
		return event;
	}
	
	public static RuntimeEngineEvent createProcessActivityErrorEvent(String pid, String name, String message) {
		RuntimeEngineEvent event = new RuntimeEngineEvent();
		event.setTimestamp(System.currentTimeMillis());
		event.setType(Type.PROCESS_ACTIVITY_ERROR);
		event.setPid(pid);
		event.setActivityName(name);
		event.setMessage(message);
		return event;
	}
	
	public static RuntimeEngineEvent createProcessActivityProgressEvent(String pid, String name, String message, int progression) {
		RuntimeEngineEvent event = new RuntimeEngineEvent();
		event.setTimestamp(System.currentTimeMillis());
		event.setType(Type.PROCESS_ACTIVITY_PROGRESS);
		event.setPid(pid);
		event.setActivityName(name);
		event.setActivityProgress(progression);
		event.setMessage(message);
		return event;
	}

}
