package fr.ortolang.diffusion.storage;

import java.io.InputStream;

/**
 * <p>
 * A DigitalObject represents an entry in the Storage subsystem. 
 * </p> 
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public class DigitalObject {
	
	private InputStream data;
	
	public DigitalObject() {
	}
	
	public DigitalObject(InputStream data) {
		this.data = data;
	}
	
	public InputStream getData() {
		return data;
	}
	
	public void setData(InputStream data) {
		this.data = data;
	}	
		
}
