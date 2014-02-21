package fr.ortolang.diffusion.registry.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@SuppressWarnings("serial")
public class RegistryTag implements Serializable {

	@Id
	private String name;
	private int weight;

	public RegistryTag(String name) {
		this.name = name;
		weight = 0;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	public void increase() {
		this.weight++;
	}
	
	public void decrease() {
		if ( this.weight > 0 ) {
			weight--;
		}
	}

}
