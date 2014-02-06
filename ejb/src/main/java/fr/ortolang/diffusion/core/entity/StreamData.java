package fr.ortolang.diffusion.core.entity;

import javax.activation.DataHandler;

public class StreamData {
	
	private DataHandler data;

    public StreamData() {
    }

    public StreamData(DataHandler data) {
        this.data = data;
    }

    public DataHandler getData() {
    	return data;
    }

    public void setData(DataHandler data) {
        this.data = data;
    }
}
