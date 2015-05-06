package fr.ortolang.diffusion.store;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class StoreWorkerJob implements Delayed {

	private String key;
	private String action;
	private long timestamp;

	public StoreWorkerJob(String key, String action, long timestamp) {
		this.key = key;
		this.action = action;
		this.timestamp = timestamp;
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

	@Override
	public int compareTo(Delayed obj) {
		if (obj.getClass() != StoreWorkerJob.class) {
			throw new IllegalArgumentException("Illegal comparision to non-StoreWorkerJob");
		}
		StoreWorkerJob other = (StoreWorkerJob) obj;
		return (int) (this.getTimestamp() - other.getTimestamp());
	}

	@Override
	public long getDelay(TimeUnit unit) {
		long diff = timestamp - System.currentTimeMillis();
		return unit.convert(diff, TimeUnit.MILLISECONDS);
	}

}