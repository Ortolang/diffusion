package fr.ortolang.diffusion.rest;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

@Provider
@Template
public class TemplateFilter implements ContainerResponseFilter {
	
	private Logger logger = Logger.getLogger(TemplateFilter.class.getName());
	
	public TemplateFilter() {
		Velocity.setProperty(Velocity.RESOURCE_LOADER, "classpath");
		Velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		Velocity.init();
	}
 
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		logger.log(Level.INFO, "Template Filter.");
		for (Annotation a : responseContext.getEntityAnnotations()) {
			if (a.annotationType() == Template.class) {
				//TODO include MediaType in template annotation to check if template is compatible
				String template = ((Template) a).value();
				logger.log(Level.INFO, "Found template annotation with name:" + template);
				String render = render(template, responseContext.getEntity());
				responseContext.setEntity(render);
				break;
			}
		}
	}
	
	private String render(String template, Object entity) {
		VelocityContext ctx = new VelocityContext();
		ctx.put("entity", entity);
		StringWriter writer = new StringWriter();
		Velocity.getTemplate(template).merge(ctx, writer);
		return writer.toString();
	}
	
}