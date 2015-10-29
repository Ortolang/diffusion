package fr.ortolang.diffusion.api.content;

import org.junit.Test;

import fr.ortolang.diffusion.api.content.ContentRepresentation;
import fr.ortolang.diffusion.template.TemplateEngine;
import fr.ortolang.diffusion.template.TemplateEngineException;


public class ContentTemplateTest {

	@Test
	public void TestCollectionTemplate() throws TemplateEngineException {
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext("/api");
		representation.setBase("/content");
		String result = TemplateEngine.getInstance().process("collection", representation);
		System.out.println(result);
	}
	
	@Test
	public void TestCollectionTemplate2() throws TemplateEngineException {
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext("/api");
		representation.setBase("/content");
		representation.setAlias("system");
		representation.setRoot("head");
		representation.setPath("/system/head");
		representation.setParentPath("/system");
		String result = TemplateEngine.getInstance().process("collection", representation);
		System.out.println(result);
	}

}
