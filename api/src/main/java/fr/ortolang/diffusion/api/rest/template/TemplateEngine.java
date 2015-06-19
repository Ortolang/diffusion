package fr.ortolang.diffusion.api.rest.template;

import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;


public class TemplateEngine {
	
	private static class TemplateEngineHolder {		
		private final static TemplateEngine instance = new TemplateEngine();
	}
	
	private Configuration cfg;
 
	private TemplateEngine() {
		cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setClassLoaderForTemplateLoading(TemplateEngine.class.getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}

	public static TemplateEngine getInstance() {
		return TemplateEngineHolder.instance;
	}
	
	public String process(String name, Object model) throws TemplateEngineException {
		try {
			Template temp = cfg.getTemplate(name + ".ftl");
			StringWriter out = new StringWriter();
	        temp.process(model, out);
	        return out.toString();
		} catch ( IOException | TemplateException e ) {
			throw new TemplateEngineException("unable to process template " + name, e);
		}
	}


}
