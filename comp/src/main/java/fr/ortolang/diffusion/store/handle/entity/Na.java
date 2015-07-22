package fr.ortolang.diffusion.store.handle.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="nas")
@SuppressWarnings("serial")
public class Na implements Serializable{

	@Id
	private byte[] na;
	
	public Na() {
	}
	
	public byte[] getNa() {
		return na;
	}
	
	public void setNa(byte[] na) {
		this.na = na;
	}
}
