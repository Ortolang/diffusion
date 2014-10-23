package fr.ortolang.diffusion.api.rest.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.object.ObjectRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.api.rest.workspace.WorkspaceElementFormRepresentation;
import fr.ortolang.diffusion.api.rest.workspace.WorkspaceResource;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.MetadataSource;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Path("/plugins")
@Produces({ MediaType.APPLICATION_JSON })
public class PluginResource {

	private Logger logger = Logger.getLogger(PluginResource.class.getName());

	@EJB
	private BrowserService browser;
	@EJB
	private SearchService search;
	@EJB
	private SecurityService security;
	@EJB
	private CoreService core;
	
	@Context
	private UriInfo uriInfo;
	private static Map<String, PluginRepresentation> plugins;
	private static File result;
	
	static {
		//FIXME ne pas laisser ce code en dur
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

		JsonArray configJSON = Json.createArrayBuilder()
				    .add(Json.createObjectBuilder()
				      .add("key", "txt-input").add("type", "dataobject").add("label", "Fichier à annoter").add("placeholder","Nom du fichier à annoter").add("required", true).add("description", "Entrez le nom du fichier texte sur lequel vous souhaitez appliquer Tree Tagger."))
				    .add(Json.createObjectBuilder()
				      .add("key", "lg-input").add("type", "select").add("label", "Langue du texte").add("required", true).add("description", "Selectionnez la langue du fichier à annoter.")
				      	.add("default", Json.createObjectBuilder().add("id", "french").add("name", "Français"))
				      	.add("options", Json.createArrayBuilder().add(Json.createObjectBuilder().add("id", "french").add("name", "Français")).add(Json.createObjectBuilder().add("id", "english").add("name", "Anglais")).add(Json.createObjectBuilder().add("id", "german").add("name", "Allemand"))))
				    .add(Json.createObjectBuilder()
				      .add("key", "txt-output").add("type", "text").add("label", "Fichier cible").add("placeholder","Nom du fichier cible").add("required", false).add("description", "Entrez le nom que vous souhaitez donner au fichier renvoyé par Tree Tagger."))
				    .build();
		treeTagger.setConfigForm(configJSON);
		
		plugins.put(treeTagger.getKey(), treeTagger);
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
	
	@GET
	@Path("/{key}/config")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response getConfig(@PathParam(value = "key") String key) {
		logger.log(Level.INFO, "get plugin config for key: " + key);
		PluginRepresentation plugin = plugins.get(key);
		JsonArray representation = plugin.getConfigForm();		
		return Response.ok(representation).build();
	}
	
	@POST
	@Path("/{key}/config-new")
	@Consumes("application/json")
	public Response postConfig(@PathParam(value = "key") String key, JsonObject form) throws IOException {
		logger.log(Level.INFO, "post plugin config for key: " + key);
		logger.log(Level.INFO, "config: " + form.toString());
		plugins.get(key).setConfig(form);
		return Response.ok().build();
	}
	
	@GET
	@Path("/{key}/invoke")
	@Produces("application/json")
	public Response invokePlugin(@PathParam(value = "key") String key) throws IOException, OrtolangException, KeyNotFoundException, AccessDeniedException, CoreServiceException, DataNotFoundException, InterruptedException {
		logger.log(Level.INFO, "invoke plugin with config for key: " + key);
		//FIXME codé en dur -> FACTORISER !
		String input="";
		String lg="";
		String previewStr = "";
        
		// retrieve the config
		JsonObject config = plugins.get(key).getConfig();
		if (key.equals("treetagger")) {
			// invoke tree tagger
			lg = config.getJsonObject("lg-input").getString("id");
			input = config.getJsonObject("txt-input").getString("key");			
			
			OrtolangObject object = browser.findObject(input);
			InputStream inputFile = core.download(input);
			
			// Put stream in a temp folder
			String pathFileInput = "input-tree-tagger";
			String pathFileTokenizer = "tokenizer-tree-tagger";
			String pathFileOutput = "output-tree-tagger";
			
			File tempFile = File.createTempFile(pathFileInput, ".tmp");
			File tempFileTokenizer = File.createTempFile(pathFileTokenizer, ".tmp");
			File tempFileOutput = File.createTempFile(pathFileOutput, ".tmp");
			logger.log(Level.INFO, "Temp files : " + tempFile.getAbsolutePath() + " \n " + tempFileOutput.getAbsolutePath() + " \n " + tempFileTokenizer.getAbsolutePath());
						
			FileOutputStream inputStream = new FileOutputStream(tempFile);			
			int read = 0;
			byte[] bytes = new byte[1024];	 
			while ((read = inputFile.read(bytes)) != -1) {
				inputStream.write(bytes, 0, read);
			}
			
			// init parameters for treetagger
			String treeTaggerLocalPath = OrtolangConfig.getInstance().getProperty("plugin.treetagger.path");
			String binPath = treeTaggerLocalPath + "/bin";
			String cmdPath = treeTaggerLocalPath + "/cmd";
			String libPath = treeTaggerLocalPath + "/lib";
			String optionToken = "-token";
			String optionLemma = "-lemma";
			String optionSgml = "-sgml";
			String optionNoUnknown = "-no-unknown";
			String encoding = "utf8"; // TODO configurable
			String tokenizer = cmdPath + "/" + encoding + "-tokenize.perl";
			String tagger = binPath + "/tree-tagger";
			String abbrList = libPath + "/" + lg + "-abbreviations";
			String parFile = libPath + "/" + lg + "-" + encoding + ".par";
			
			//execute treetagger
			String[] command1 = {tokenizer, "-f", "-a", abbrList, tempFile.getPath(), tempFileOutput.getPath()};
			logger.log(Level.INFO, tokenizer + " -f" + " -a " + abbrList + " " + tempFile.getPath() + " > " + tempFileTokenizer.getPath());
			ProcessBuilder pBuilder1 = new ProcessBuilder(command1);
			pBuilder1.redirectErrorStream(true);
			pBuilder1.redirectOutput(tempFileTokenizer);
			Process p1 = pBuilder1.start(); 
			if (p1.waitFor() == 0) {
	            p1.destroy();
	            
				String[] command2 = {tagger, optionToken, optionLemma, optionSgml, optionNoUnknown, parFile, tempFileTokenizer.getPath(), tempFileOutput.getPath()};
				logger.log(Level.INFO, tagger + " " + optionToken + " " + optionLemma + " " + optionSgml + " " + parFile + " " + tempFileTokenizer.getPath() + " " + tempFileOutput.getPath());
				ProcessBuilder pBuilder2 = new ProcessBuilder(command2);
				pBuilder2.redirectErrorStream(true);
				Process p2 = pBuilder2.start(); 
				if(p2.waitFor() == 0) {
					p2.destroy();
					
					FileInputStream fis = new FileInputStream(tempFileOutput.getPath());
		            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
		         	     
		            // Preview string
		            int n = 0;
		            String line = reader.readLine();
		            while(line != null && n<=50){
		                previewStr += line + '\n';
		                n ++;
		                line = reader.readLine();
		            }          
//		            logger.log(Level.INFO, "Preview : " + previewStr);

		            // Build result
		            JsonObject preview = Json.createObjectBuilder()
		            		.add("output", previewStr)
		            		.add("urlResult", tempFileOutput.getPath())
		            		.build();
		            
		            tempFile.delete();
//		            tempFileOutput.deleteOnExit();
		            tempFileTokenizer.deleteOnExit();
		            
					final ResponseBuilder response = Response.ok(preview);
					return response.build();
				}
			}
				
		};
		
		logger.log(Level.FINE, "unable to find the plugin : " + key);
		return Response.status(Response.Status.NOT_FOUND).build();
	}
	
	@GET
	@Path("/{key}/download")
	public void download(@PathParam(value = "key") String key, @QueryParam(value = "path") String path, @Context HttpServletResponse response) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException,
			OrtolangException, DataNotFoundException, IOException, CoreServiceException, InvalidPathException {
		logger.log(Level.INFO, "GET /plugins/" + key + "/download?path=" + path );
		if (path == null) {
			response.sendError(Response.Status.BAD_REQUEST.ordinal(), "parameter 'path' is mandatory");
			return;
		}
		
		File fileResult = new File(path);
		
		JsonObject config = plugins.get(key).getConfig();
		String outputName = config.getString("txt-output");	
		if(outputName == null) {
			response.setHeader("Content-Disposition", "attachment; filename=" + fileResult.getName());			
		}
		else{
			response.setHeader("Content-Disposition", "attachment; filename=" + outputName);			
		}
		
		response.setContentType(new MimetypesFileTypeMap().getContentType(fileResult));
		response.setContentLength((int) fileResult.length());
		InputStream input = new FileInputStream(path);
		try {
			IOUtils.copy(input, response.getOutputStream());
		} finally {
			IOUtils.closeQuietly(input);
		}
	}
}