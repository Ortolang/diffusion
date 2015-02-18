package fr.ortolang.diffusion.template;

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

import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

public class TemplateEngine {

	static {
		Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new TemplateEngineLogger());
		Velocity.setProperty(Velocity.RESOURCE_LOADER, "classpath");
		Velocity.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		Velocity.init();
	}

	private TemplateEngine() {
	}

	public static boolean evaluate(Map<String, Object> context, Writer out, String logTag, String instring) {
		VelocityContext ctx = new VelocityContext(context);
		return Velocity.evaluate(ctx, out, logTag, instring);
	}

	public static boolean evaluate(Map<String, Object> context, Writer out, String logTag, InputStreamReader instream) {
		VelocityContext ctx = new VelocityContext(context);
		return Velocity.evaluate(ctx, out, logTag, instream);
	}

	public static boolean mergeTemplate(String templateName, Map<String, Object> context, Writer writer) {
		VelocityContext ctx = new VelocityContext(context);
		return Velocity.mergeTemplate(templateName, System.getProperty("file.encoding"), ctx, writer);
	}

}
