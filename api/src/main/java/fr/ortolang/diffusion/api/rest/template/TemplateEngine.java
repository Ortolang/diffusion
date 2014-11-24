package fr.ortolang.diffusion.api.rest.template;

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
