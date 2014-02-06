package fr.ortolang.diffusion.rest;

public class LinkRepresentation {

	private String relation;
	private String reference;
	private String type;
	
	public LinkRepresentation() {
	}
	
	public LinkRepresentation(String relation, String reference, String type) {
		this.relation = relation;
		this.reference = reference;
		this.type = type;
	}

	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
