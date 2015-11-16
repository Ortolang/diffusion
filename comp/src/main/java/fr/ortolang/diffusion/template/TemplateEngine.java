package fr.ortolang.diffusion.template;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;


public class TemplateEngine {
	
	private static class TemplateEngineHolder {		
		private final static Map<ClassLoader, TemplateEngine> instances = new HashMap<ClassLoader, TemplateEngine> ();
	}
	
	private Configuration cfg;
 
	private TemplateEngine(ClassLoader cl) {
	    cfg = new Configuration(Configuration.VERSION_2_3_22);
        cfg.setClassLoaderForTemplateLoading(cl, "templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setSharedVariable("formatsize", new FormatSizeMethod());
        cfg.setSharedVariable("fileicon", new FileIconMethod());
	}

	public static TemplateEngine getInstance(ClassLoader cl) {
	    if ( !TemplateEngineHolder.instances.containsKey(cl) ) {
	        TemplateEngineHolder.instances.put(cl, new TemplateEngine(cl));
	    }
		return TemplateEngineHolder.instances.get(cl);
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
