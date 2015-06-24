package fr.ortolang.diffusion.api.organization;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.referentiel.entity.Organization;

@XmlRootElement(name = "group")
public class OrganizationRepresentation {

    @XmlAttribute(name = "key")
    private String key;

    private String id;
    private String name;
	private String fullname;
	private String acronym;
	private String city;
	private String country;
	private String homepage;
	private String img;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFullname() {
		return fullname;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getAcronym() {
		return acronym;
	}

	public void setAcronym(String acronym) {
		this.acronym = acronym;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	public String getImg() {
		return img;
	}

	public void setImg(String img) {
		this.img = img;
	}

	public static OrganizationRepresentation fromOrganization(Organization org) {
		OrganizationRepresentation representation = new OrganizationRepresentation();
		representation.setId(org.getId());
		representation.setKey(org.getKey());
		representation.setName(org.getName());
		representation.setFullname(org.getFullname());
		representation.setAcronym(org.getAcronym());
		representation.setCity(org.getCity());
		representation.setCountry(org.getCountry());
		representation.setHomepage(org.getHomepage());
		representation.setImg(org.getImg());
		
		return representation;
	}

}
