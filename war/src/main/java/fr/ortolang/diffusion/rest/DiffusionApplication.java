package fr.ortolang.diffusion.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import fr.ortolang.diffusion.rest.api.OrtolangObjectResource;

public class DiffusionApplication extends Application {
	
	private HashSet<Class<?>> classes = new HashSet<Class<?>>();

	public DiffusionApplication() {
		classes.add(OrtolangObjectResource.class);
	}

	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		HashSet<Object> set = new HashSet<Object>();
		return set;
	}
}