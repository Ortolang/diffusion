package fr.ortolang.diffusion;

public class OrtolangObjectPid {

	private Type type;
	private String name;
	private String key;
	private String target;
	
	public OrtolangObjectPid() {
		super();
	}

	public OrtolangObjectPid(Type type, String name, String key, String target) {
		super();
		this.type = type;
		this.name = name;
		this.key = key;
		this.target = target;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
	
	@Override
	public String toString() {
		return "OrtolangObjectPID{" +
				"key='" + key + '\'' +
				", type=" + type +
				", name=" + name +
				", target=" + target +
				'}';
	}
	
	public enum Type {
		HANDLE
	}
}
