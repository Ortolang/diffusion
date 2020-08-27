package fr.ortolang.diffusion.search;

public class Highlight {

	private String[] fields;
	private String[] postTags;
	private String[] preTags;
	
	public String[] getFields() {
		return fields;
	}
	public void setFields(String[] fields) {
		this.fields = fields;
	}
	public String[] getPostTags() {
		return postTags;
	}
	public void setPostTags(String[] postTags) {
		this.postTags = postTags;
	}
	public String[] getPreTags() {
		return preTags;
	}
	public void setPreTags(String[] preTags) {
		this.preTags = preTags;
	}
	
	public static Highlight highlight() {
		return new Highlight();
	}
	public Highlight field(String field) {
		this.fields = new String[] {field};
		return this;
	}
	public Highlight fields(String[] fields) {
		this.fields = fields;
		return this;
	}
	public Highlight preTag(String preTag) {
		this.preTags = new String[] {preTag};
		return this;
	}
	public Highlight postTag(String postTag) {
		this.postTags = new String[] {postTag};
		return this;
	}
}
