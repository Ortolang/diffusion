package fr.ortolang.diffusion.api.admin;

import fr.ortolang.diffusion.content.entity.ContentSearchResource;

public class ContentSearchResourceRepresentation {

	private String workspace;
	private String pid;
	private String title;
	private String description;
	private String landingPageURI;
	
	public ContentSearchResourceRepresentation() {
	}
	
	public static ContentSearchResourceRepresentation fromResource(ContentSearchResource res) {
		ContentSearchResourceRepresentation rep = new ContentSearchResourceRepresentation();
		rep.setWorkspace(res.getWorkspace());
		rep.setPid(res.getPid());
		rep.setTitle(res.getTitle());
		rep.setDescription(res.getDescription());
		rep.setLandingPageURI(res.getLandingPageURI());
		return rep;
	}
	
	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	public String getPid() {
		return pid;
	}
	public void setPid(String pid) {
		this.pid = pid;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLandingPageURI() {
		return landingPageURI;
	}
	public void setLandingPageURI(String landingPageURI) {
		this.landingPageURI = landingPageURI;
	}
	
	
}
