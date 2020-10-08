package fr.ortolang.diffusion.search;

public class Highlight {

	public static final int HIGHLIGHT_FRAGMENTSIZE = 150;
	public static final int HIGHLIGHT_DEFAULT_NUMFRAGMENT = 1;
	public static final int HIGHLIGHT_MAX_NUMFRAGMENT = 1000;
	public static final int HIGHLIGHT_PREFERRED_NUMFRAGMENT = 25;

	private String[] fields;
	private String[] postTags;
	private String[] preTags;
	private Integer fragmentSize;
	private Integer numOfFragments;
	
	public Highlight() {
		fragmentSize = HIGHLIGHT_FRAGMENTSIZE;
		numOfFragments = HIGHLIGHT_DEFAULT_NUMFRAGMENT;
	}
	
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
	public Integer getFragmentSize() {
		return fragmentSize;
	}
	public void setFragmentSize(Integer fragmentSize) {
		this.fragmentSize = fragmentSize;
	}
	
	public Integer getNumOfFragments() {
		return numOfFragments;
	}
	public void setNumOfFragments(Integer numOfFragments) {
		this.numOfFragments = numOfFragments;
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
	public Highlight fragmentSize(Integer fragmentSize) {
		this.fragmentSize = fragmentSize;
		return this;
	}
	public Highlight numOfFragments(Integer numOfFragments) {
		if (numOfFragments > HIGHLIGHT_MAX_NUMFRAGMENT) {
			this.numOfFragments = HIGHLIGHT_MAX_NUMFRAGMENT;
		} else {
			this.numOfFragments = numOfFragments;
		}
		return this;
	}
}
