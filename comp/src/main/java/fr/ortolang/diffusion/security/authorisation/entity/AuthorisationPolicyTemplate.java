package fr.ortolang.diffusion.security.authorisation.entity;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries ({
	@NamedQuery(name="findAllAuthorisationPolicyTemplate", query="SELECT t FROM AuthorisationPolicyTemplate t")
})
@Cacheable(true)
public class AuthorisationPolicyTemplate {
	
	public static final String DEFAULT = "default";
	public static final String FORALL = "forall";
	public static final String AUTHENTIFIED = "authentified";
	public static final String ESR = "esr";
	public static final String RESTRICTED = "restricted";
	
	@Id
	private String name;
	private String description;
	private String template;
	
	public AuthorisationPolicyTemplate() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}
	
}
