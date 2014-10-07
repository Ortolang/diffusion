package fr.ortolang.diffusion.api.rest.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.object.ObjectRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

@Path("/plugins")
@Produces({ MediaType.APPLICATION_JSON })
public class PluginResource {

	private Logger logger = Logger.getLogger(PluginResource.class.getName());

	@Context
	private UriInfo uriInfo;
	private static Map<String, PluginRepresentation> plugins;
	
	static {
		plugins = new HashMap<String, PluginRepresentation>();
		PluginRepresentation treeTagger = new PluginRepresentation("treetagger", 
				"TreeTagger", 
				"A language independent part-of-speech tagger");
		treeTagger.setDetail("The TreeTagger is a tool for annotating text with part-of-speech and lemma information. "
				+ "It was developed by Helmut Schmid in the TC project at the Institute for Computational Linguistics of the University of Stuttgart. "
				+ "The TreeTagger has been successfully used to tag German, English, French, Italian, Dutch, Spanish, Bulgarian, Russian, Portuguese, "
				+ "Galician, Chinese, Swahili, Slovak, Latin, Estonian, Polish and old French texts and is adaptable to other languages if a lexicon "
				+ "and a manually tagged training corpus are available. "
				+ "The TreeTagger can also be used as a chunker for English, German, and French. ");
		treeTagger.setUrl("http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/");
		treeTagger.setFormats(Arrays.asList("text/plain"));
		plugins.put(treeTagger.getKey(), treeTagger);;
	}
	
	public PluginResource() {
	}
	
	@GET
	@Template( template="plugins/list.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response list() {
		logger.log(Level.INFO, "list availables plugins");
		
		GenericCollectionRepresentation<PluginRepresentation> representation = new GenericCollectionRepresentation<PluginRepresentation> ();
		for(Entry<String, PluginRepresentation> plugin : plugins.entrySet()) {
		    representation.addEntry(plugin.getValue());
		}
		representation.setOffset(0);
		representation.setSize(plugins.size());
		representation.setLimit(plugins.size());
		
		Response response = Response.ok(representation).build();
		return response;
	}
	
	@GET
	@Path("/{key}")
	@Template( template="plugins/detail.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response get(@PathParam(value = "key") String key) {
		logger.log(Level.INFO, "get plugin for key: " + key);
				
		PluginRepresentation representation = plugins.get(key);
		
		return Response.ok(representation).build();
	}
	
	@SuppressWarnings("unused")
	private void invokePlugin() {
		
	}
	
}