package fr.ortolang.diffusion.api.admin;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.store.handle.entity.Handle;

public class HandleRepresentation {

	private static final Logger LOGGER = Logger.getLogger(HandleRepresentation.class.getName());

	private String key;
	private String handle;
	private String url;

	public HandleRepresentation() {
	}

	public HandleRepresentation(String key, String handle, String url) {
		this.key = key;
		this.handle = handle;
		this.url = url;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public static HandleRepresentation fromHandle(Handle handle) {
		HandleRepresentation rep = new HandleRepresentation();
		try {
			rep.setHandle(handle.getHandleString());
			rep.setKey(handle.getKey());
			rep.setUrl(handle.getDataString());
		} catch (UnsupportedEncodingException e) {
			LOGGER.log(Level.SEVERE, "Handler Representation : " + e.getMessage(), e);
		}
		return rep;
	}

}
