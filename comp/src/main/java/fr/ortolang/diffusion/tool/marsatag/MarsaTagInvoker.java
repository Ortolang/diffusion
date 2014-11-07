package fr.ortolang.diffusion.tool.marsatag;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.tool.invoke.ToolInvoker;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

public class MarsaTagInvoker implements ToolInvoker {
	
	private Logger logger = Logger.getLogger(MarsaTagInvoker.class.getName());
	
	private CoreService core;
	private String base;
	private boolean initialized = false;
	
	public MarsaTagInvoker() {
	}
	
	private void init() throws Exception {
		logger.log(Level.INFO, "Initializing MarsaTag Tool");
		if ( OrtolangConfig.getInstance().getProperty("plugin.marsatag.path") != null ) {
			base = OrtolangConfig.getInstance().getProperty("plugin.marsatag.path");
			logger.log(Level.INFO, "Base marsatag path set to: " + base);
		} else {
			throw new Exception("base path of marsatag tool not found in configuration");
		}
		logger.log(Level.INFO, "MarsaTag Tool initialized");
	}
	
	public CoreService getCoreService() throws Exception {
		if (core == null) {
			core = (CoreService) OrtolangServiceLocator.findService(CoreService.SERVICE_NAME);
		}
		return core;
	}

	protected void setCoreService(CoreService core) {
		this.core = core;
	}

	@Override
	public ToolInvokerResult invoke(Map<String, String> params) {
		ToolInvokerResult result = new ToolInvokerResult();
		result.setStart(System.currentTimeMillis());
		result.setLog("");
		try {
			if ( !initialized ) {
				this.init();
			}
			
			String key = null;
			ArrayList<String> options = prepareCmd(params);
			
			// validate params
			if ( !params.containsKey("txt-input") || params.get("txt-input").length() == 0 ) {
				throw new Exception("parameter txt-input is mandatory");
			} else {
				key = params.get("txt-input");
			}			
			
		    // create temporary files
			Path program = Paths.get(base, "./MarsaTag-UI.sh");			
			
			// prepare input
			Path input = Files.createTempFile("ttin.", ".tmp");
			
			InputStream is = getCoreService().download(key);
			Files.copy(is, input, StandardCopyOption.REPLACE_EXISTING);
			
			result = invokeMarsaTag(result, program, options, input);
			
			//logger.log(Level.INFO, result.getOutput());
				
			
		} catch ( Exception e ) {
			//e.printStackTrace();
			result.setLog(result.getLog() + "unexpected error occured during tool execution: " + e.getMessage() + "\r\n");
			result.setStatus(ToolInvokerResult.Status.ERROR);
			result.setStop(System.currentTimeMillis());
		}
		return result;
	}
		
	/**
	 * Return the list of options for the treetagger command
	 * @param params
	 * @return ArrayList<String>
	 * @throws Exception 
	 * @throws DataNotFoundException 
	 * @throws AccessDeniedException 
	 * @throws KeyNotFoundException 
	 * @throws CoreServiceException 
	 */
	public ArrayList<String> prepareCmd (Map<String, String> params) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException, Exception {
		ArrayList<String> options = new ArrayList<String>();

		// cli version
		options.add("--cli");
		
		// output
		options.add("--out-dir");
		options.add("/tmp");
				
		// reader options
		if( params.containsKey("reader") && !params.get("reader").isEmpty() ){
			options.add("--reader");
			options.add(params.get("reader"));
			if( params.containsKey("encoding") && !params.get("encoding").isEmpty() ){
				options.add("--encoding");
				options.add(params.get("encoding"));
			}
			if( (params.get("reader").equals("praat-textgrid") || params.get("reader").equals("praat-textgrid-text"))
					&& params.containsKey("praat-tier") && !params.get("praat-tier").isEmpty() ){
				options.add("--praat-tier");
				options.add(params.get("praat-tier"));
			} else if((params.get("reader").equals("praat-textgrid") || params.get("reader").equals("praat-textgrid-text"))){
				throw new Exception("parameter praat-tier is mandatory with readers praat-textgrid and praat-textgrid-text.");
			}
		}
		
		// writer options
		if( params.containsKey("writer") && !params.get("writer").isEmpty() ){
			options.add("--writer");
			options.add(params.get("writer"));
			if( params.containsKey("out-encoding") && !params.get("out-encoding").isEmpty() ){
				options.add("--out-encoding");
				options.add(params.get("out-encoding"));
			}
		}
		
		// tokenizer
		if ( params.containsKey("tokenizer") && Boolean.valueOf(params.get("tokenizer")) ){
			options.add("--tokenizer");
			if ( params.containsKey("tokenizer-class") && !params.get("tokenizer-class").isEmpty() ){
				options.add(params.get("tokenizer-class"));
			} else {
				// default
				options.add("lpl");
			}
		}
		
		// morphosyntaxer
		if ( params.containsKey("morphosyntaxer") && Boolean.valueOf(params.get("morphosyntaxer")) ){
			options.add("--morphosyntaxer");
			if ( params.containsKey("morphosyntaxer-class") && !params.get("morphosyntaxer-class").isEmpty() ){
				options.add(params.get("morphosyntaxer-class"));
			} else {
				// default
				options.add("lpl");
			}
		}
		
		// postagger
		if ( params.containsKey("postagger") && Boolean.valueOf(params.get("postagger")) ){
			options.add("--postagger");
			if ( params.containsKey("postagger-class") && !params.get("postagger-class").isEmpty() ){
				options.add(params.get("postagger-class"));
			} else {
				// default
				options.add("lpl");
			}
		}
		
		// parser
		if ( params.containsKey("parser") && Boolean.valueOf(params.get("parser")) ){
			options.add("--parser");
			if ( params.containsKey("parser-class") && !params.get("parser-class").isEmpty() ){
				options.add(params.get("parser-class"));
			} else {
				// default
				options.add("lpl");
			}
		}
		
		// oral
		if ( params.containsKey("oral") && Boolean.valueOf(params.get("oral")) ){
			options.add("--oral");
		}

		return options;
	}

	/**
	 * Invoke tree-tagger
	 * @param result
	 * @param program
	 * @param options
	 * @param input
	 * @return ToolInvokerResult
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public ToolInvokerResult invokeMarsaTag(ToolInvokerResult result, Path program, ArrayList<String> options, Path input) throws InterruptedException, Exception{
		ArrayList<String> tagList = new ArrayList<String>();
		tagList.add(program.toString());
		tagList.addAll(options);
		if(input != null && !input.toString().isEmpty()){
			tagList.add(input.toString());
		}
		String[] tag = new String[tagList.size()];
		tag = tagList.toArray(tag);
		
		ProcessBuilder ptag = new ProcessBuilder(tag);
		final Process p2 = ptag.start(); 
		final StringWriter writer = new StringWriter();
		
		new Thread(new Runnable() {
	        public void run() {
	            try {
		            IOUtils.copy(p2.getInputStream(), writer);
		            IOUtils.copy(p2.getErrorStream(), writer);
				} catch (IOException e) {
					e.printStackTrace();
				}
	        }
		}).start();

		result.setLog(result.getLog() + "marsatag subprocess called: " + Arrays.deepToString(tag) + "\r\n");
		
		final int exitValue = p2.waitFor();
	    final String processOutput = writer.toString();
		
		if(exitValue == 0) {
			result.setLog(result.getLog() + "marsatag subprocess finished successfully\r\n");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Path output = Paths.get(input + ".out");
			Files.copy(output, baos);
			result.setOutput(baos.toString());
			List<String> listFileResult = Arrays.asList(output.toString());
			result.setOutputFilePath(listFileResult);
			result.setLog(processOutput);
			result.setStatus(ToolInvokerResult.Status.SUCCESS);
			result.setStop(System.currentTimeMillis());
			p2.destroy();
			return result;
		} else {
			result.setLog(result.getLog() + processOutput + "\t\n");
			p2.destroy();
			throw new Exception("marsatag subprocess failed.");
		}
	}
}
