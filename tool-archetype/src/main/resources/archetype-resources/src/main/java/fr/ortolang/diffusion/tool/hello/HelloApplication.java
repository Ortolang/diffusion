package fr.ortolang.diffusion.tool.hello;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import fr.ortolang.diffusion.api.rest.form.FormResource;
import fr.ortolang.diffusion.api.rest.filter.ContentTypeSetterPreProcessorInterceptor;
import fr.ortolang.diffusion.api.rest.mapper.*;
import fr.ortolang.diffusion.api.rest.object.ObjectResource;
import fr.ortolang.diffusion.api.rest.profile.ProfileResource;
import fr.ortolang.diffusion.api.rest.runtime.RuntimeResource;
import fr.ortolang.diffusion.api.rest.template.TemplateFilter;
import fr.ortolang.diffusion.api.rest.tool.ToolResource;
import fr.ortolang.diffusion.api.rest.workspace.WorkspaceResource;

@ApplicationPath("/*")
public class HelloApplication extends Application {
	
}