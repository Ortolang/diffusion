package fr.ortolang.diffusion.api.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import fr.ortolang.diffusion.api.rest.mapper.AccessDeniedExceptionMapper;
import fr.ortolang.diffusion.api.rest.mapper.BrowserServiceExceptionMapper;
import fr.ortolang.diffusion.api.rest.mapper.KeyAlreadyExistsExceptionMapper;
import fr.ortolang.diffusion.api.rest.mapper.KeyNotFoundExceptionMapper;
import fr.ortolang.diffusion.api.rest.mapper.PropertyNotFoundExceptionMapper;
import fr.ortolang.diffusion.api.rest.mapper.SearchServiceExceptionMapper;
import fr.ortolang.diffusion.api.rest.mapper.SecurityServiceExceptionMapper;
import fr.ortolang.diffusion.api.rest.object.ObjectResource;
import fr.ortolang.diffusion.api.rest.profile.ProfileResource;
import fr.ortolang.diffusion.api.rest.runtime.RuntimeResource;
import fr.ortolang.diffusion.api.rest.template.TemplateFilter;
import fr.ortolang.diffusion.api.rest.tool.ToolResource;
import fr.ortolang.diffusion.api.rest.workspace.WorkspaceResource;

@ApplicationPath("/rest/*")
public class DiffusionApplication extends Application {
	
	private HashSet<Class<?>> classes = new HashSet<Class<?>>();

	public DiffusionApplication() {
		classes.add(ObjectResource.class);
		classes.add(WorkspaceResource.class);
		classes.add(ProfileResource.class);
		classes.add(RuntimeResource.class);
		classes.add(ToolResource.class);
	}

	@Override
	public Set<Class<?>> getClasses() {
		return classes;
	}

	@Override
	public Set<Object> getSingletons() {
		HashSet<Object> set = new HashSet<Object>();
		set.add(new TemplateFilter());
		set.add(new AccessDeniedExceptionMapper());
		set.add(new BrowserServiceExceptionMapper());
		set.add(new KeyAlreadyExistsExceptionMapper());
		set.add(new KeyNotFoundExceptionMapper());
		set.add(new PropertyNotFoundExceptionMapper());
		set.add(new SearchServiceExceptionMapper());
		set.add(new SecurityServiceExceptionMapper());
		return set;
	}
}