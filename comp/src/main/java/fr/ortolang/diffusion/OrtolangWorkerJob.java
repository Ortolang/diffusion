package fr.ortolang.diffusion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class OrtolangWorkerJob implements Delayed {

	private String key;
	private String action;
	private long timestamp;
	private Map<String, Object> args;

	public OrtolangWorkerJob(String key, String action, long timestamp) {
		this(key, action, timestamp, new HashMap<String, Object> ());
	}
	
	public OrtolangWorkerJob(String key, String action, long timestamp, Map<String, Object> args) {
		this.key = key;
		this.action = action;
		this.timestamp = timestamp;
		this.args = args;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public Map<String, Object> getArgs() {
		return args;
	}
	
	public void setArgs(Map<String, Object> args) {
		this.args = args;
	}
	
	public Object getArg(String name) {
		return args.get(name);
	}
	
	public boolean containsArg(String name) {
		return args.containsKey(name);
	}

	@Override
	public int compareTo(Delayed obj) {
		if (obj.getClass() != OrtolangWorkerJob.class) {
			throw new IllegalArgumentException("Illegal comparision to non-StoreWorkerJob");
		}
		OrtolangWorkerJob other = (OrtolangWorkerJob) obj;
		return (int) (this.getTimestamp() - other.getTimestamp());
	}

	@Override
	public long getDelay(TimeUnit unit) {
		long diff = timestamp - System.currentTimeMillis();
		return unit.convert(diff, TimeUnit.MILLISECONDS);
	}

}