package fr.ortolang.diffusion.api.content;

import org.junit.Test;

import fr.ortolang.diffusion.api.content.ContentRepresentation;
import fr.ortolang.diffusion.api.template.TemplateEngine;
import fr.ortolang.diffusion.api.template.TemplateEngineException;


public class ContentTemplateTest {

	@Test
	public void TestCollectionTemplate() throws TemplateEngineException {
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext("/api");
		representation.setBase("/rest/content");
		String result = TemplateEngine.getInstance().process("collection", representation);
		System.out.println(result);
	}
	
	@Test
	public void TestCollectionTemplate2() throws TemplateEngineException {
		ContentRepresentation representation = new ContentRepresentation();
		representation.setContext("/api");
		representation.setBase("/rest/content");
		representation.setAlias("system");
		representation.setSnapshot("head");
		representation.setPath("/system/head");
		representation.setParentPath("/system");
		String result = TemplateEngine.getInstance().process("collection", representation);
		System.out.println(result);
	}

}