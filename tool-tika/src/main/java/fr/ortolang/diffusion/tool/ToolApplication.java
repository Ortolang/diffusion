package fr.ortolang.diffusion.tool;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import fr.ortolang.diffusion.tool.resource.ToolResource;

@ApplicationPath("/*")
public class ToolApplication extends Application {
	
	private HashSet<Class<?>> classes = new HashSet<Class<?>>();

	public ToolApplication() {
		classes.add(ToolResource.class);
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