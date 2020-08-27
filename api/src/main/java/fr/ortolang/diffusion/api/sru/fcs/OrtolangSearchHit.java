package fr.ortolang.diffusion.api.sru.fcs;

public class OrtolangSearchHit {

	private String id;
	private String pid;
	private String name;
	private String fragment;
	
	public OrtolangSearchHit(String id, String pid, String name, String fragment) {
		this.id = id;
		this.pid = pid;
		this.name = name;
		this.fragment = fragment;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getTitle() {
		return name;
	}

	public void setTitle(String title) {
		this.name = title;
	}

	public String getFragment() {
		return fragment;
	}
	public void setFragment(String fragment) {
		this.fragment = fragment;
	}
	
	
}
