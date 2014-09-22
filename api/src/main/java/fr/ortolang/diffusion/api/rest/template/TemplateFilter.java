package fr.ortolang.diffusion.api.rest.template;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.tools.generic.EscapeTool;

import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;

@Provider
@Template
public class TemplateFilter implements ContainerResponseFilter {
	
	private static final String TEMPLATES_BASE = "templates";
	private static final String DECORATOR = "decorator.vm";
	
	private static Logger logger = Logger.getLogger(TemplateFilter.class.getName());
	
	public TemplateFilter() {
		Velocity.setProperty(Velocity.RESOURCE_LOADER, "classpath");
		Velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		Velocity.init();
	}
 
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		//TODO apply a dedicated template for error codes...
		if ( responseContext.getEntityAnnotations() != null ) {
			for (Annotation a : responseContext.getEntityAnnotations()) {
				if (a.annotationType() == Template.class) {
					if ( Arrays.asList(((Template) a).types()).contains(responseContext.getMediaType().toString()) ) {
						logger.log(Level.FINE, "Compatible Template annotation found, applying template");
						EscapeTool escape = new EscapeTool();
						VelocityContext ctx = new VelocityContext();
						ctx.put("template", TEMPLATES_BASE + "/" + ((Template) a).template());
						ctx.put("params", requestContext.getUriInfo().getQueryParameters());
						ctx.put("mediatype", responseContext.getMediaType().toString());
						ctx.put("base", DiffusionUriBuilder.getBaseUriBuilder().build().toString());
						ctx.put("entity", responseContext.getEntity());
						ctx.put("esc", escape);
						StringWriter writer = new StringWriter();
						Velocity.getTemplate(TEMPLATES_BASE + "/" + DECORATOR).merge(ctx, writer);
						responseContext.setEntity(writer.toString());
						break;
					};
				}
			}
		}
	}
	
}