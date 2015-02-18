package fr.ortolang.diffusion.api.rest.template;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.velocity.tools.generic.EscapeTool;

import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;

@Provider
@Template
public class TemplateFilter implements ContainerResponseFilter {
	
	private static final String TEMPLATES_BASE = "templates";
	private static final String DECORATOR = "decorator.vm";
	
	private static Logger logger = Logger.getLogger(TemplateFilter.class.getName());
	
	public TemplateFilter() {
		
	}
 
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		//TODO apply a dedicated template for error codes...
		if ( responseContext.getStatus() == Response.Status.OK.getStatusCode() && responseContext.getEntityAnnotations() != null ) {
			for (Annotation a : responseContext.getEntityAnnotations()) {
				if (a.annotationType() == Template.class) {
					if ( Arrays.asList(((Template) a).types()).contains(responseContext.getMediaType().toString()) ) {
						logger.log(Level.FINE, "Compatible Template annotation found, applying template");
						EscapeTool escape = new EscapeTool();
						Map<String, Object> ctx = new HashMap<String, Object> ();
						ctx.put("template", TEMPLATES_BASE + "/" + ((Template) a).template());
						ctx.put("params", requestContext.getUriInfo().getQueryParameters());
						ctx.put("mediatype", responseContext.getMediaType().toString());
						ctx.put("base", DiffusionUriBuilder.getBaseUriBuilder().build().toString());
						ctx.put("entity", responseContext.getEntity());
						ctx.put("esc", escape);
						StringWriter writer = new StringWriter();
						TemplateEngine.mergeTemplate(TEMPLATES_BASE + "/" + DECORATOR, ctx, writer);
						responseContext.setEntity(writer.toString());
						break;
					};
				}
			}
		}
	}
	
}