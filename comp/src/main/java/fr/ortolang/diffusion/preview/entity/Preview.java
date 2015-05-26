package fr.ortolang.diffusion.preview.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@NamedQueries({
	@NamedQuery(name = "findExistingPreviews", query = "SELECT p FROM Preview p WHERE p.key IN :keysList") 
})
public class Preview {
	
	public static final String MIMETYPE = "image/jpeg";
	public static final String SMALL = "small";
	public static final String LARGE = "large";
	public static final int SMALL_PREVIEW_WIDTH = 120;
	public static final int SMALL_PREVIEW_HEIGHT = 120;
	public static final int LARGE_PREVIEW_WIDTH = 300;
	public static final int LARGE_PREVIEW_HEIGHT = 300;

	@Id
	private String key;
	private String small;
	private String large;
	private long generationDate;

	public Preview() {
	}

	public Preview(String key, String small, String large, long generationDate) {
		super();
		this.key = key;
		this.small = small;
		this.large = large;
		this.generationDate = generationDate;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getLarge() {
		return large;
	}

	public void setLarge(String large) {
		this.large = large;
	}

	public String getSmall() {
		return small;
	}

	public void setSmall(String small) {
		this.small = small;
	}

	public long getGenerationDate() {
		return generationDate;
	}

	public void setGenerationDate(long generationDate) {
		this.generationDate = generationDate;
	}

}
