package fr.ortolang.diffusion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class OrtolangJob implements Delayed {

	private String action;
	private String target;
	private long timestamp;
	private Map<String, Object> parameters;

	public OrtolangJob(String action, String target, long timestamp) {
		this(action, target, timestamp, new HashMap<String, Object> ());
	}
	
	public OrtolangJob(String action, String target, long timestamp, Map<String, Object> args) {
		this.action = action;
		this.target = target;
		this.timestamp = timestamp;
		this.parameters = args;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}
	
	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public Object getParameter(String name) {
		return parameters.get(name);
	}
	
	public boolean containsParameter(String name) {
		return parameters.containsKey(name);
	}

	@Override
	public int compareTo(Delayed obj) {
		if (obj.getClass() != OrtolangJob.class) {
			throw new IllegalArgumentException("Illegal comparision to non-OrtolangJob");
		}
		OrtolangJob other = (OrtolangJob) obj;
		return (int) (this.getTimestamp() - other.getTimestamp());
	}

	@Override
	public long getDelay(TimeUnit unit) {
		long diff = timestamp - System.currentTimeMillis();
		return unit.convert(diff, TimeUnit.MILLISECONDS);
	}

}