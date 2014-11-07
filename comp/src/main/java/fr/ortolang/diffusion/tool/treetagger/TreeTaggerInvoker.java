package fr.ortolang.diffusion.tool.treetagger;

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

public class TreeTaggerInvoker implements ToolInvoker {
	
	private Logger logger = Logger.getLogger(TreeTaggerInvoker.class.getName());
	
	private CoreService core;
	private String base;
	private boolean initialized = false;
	
	public TreeTaggerInvoker() {
	}
	
	private void init() throws Exception {
		logger.log(Level.INFO, "Initializing TreeTagger Tool");
		if ( OrtolangConfig.getInstance().getProperty("plugin.treetagger.path") != null ) {
			base = OrtolangConfig.getInstance().getProperty("plugin.treetagger.path");
			logger.log(Level.INFO, "Base treetagger path set to: " + base);
		} else {
			throw new Exception("base path of treetagger perl library not found in configuration");
		}
		logger.log(Level.INFO, "TreeTagger Tool initialized");
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
			
			String lg = null;
			String key = null;
			Boolean tokenizeInput = true;
			
			// validate params
			if ( !params.containsKey("lg-input") || params.get("lg-input").length() == 0 ) {
				throw new Exception("parameter lg-input is mandatory");
			} else {
				lg = params.get("lg-input");
			}
			
			if( params.containsKey("task") && params.get("task").equals("run-tt") ) {	
				if ( !params.containsKey("txt-input") || params.get("txt-input").length() == 0 ) {
					throw new Exception("parameter txt-input is mandatory");
				} else {
					key = params.get("txt-input");
				}
				if( params.containsKey("tokenizer") ) {
					tokenizeInput = Boolean.valueOf(params.get("tokenizer"));
				}
			}

			ArrayList<String> options = prepareCmd(params);
			
		    // create temporary files
			Path tagger = Paths.get(base, "bin", "tree-tagger");
			Path par = Paths.get(base, "lib", lg + "-utf8.par");
			Path output = Files.createTempFile("ttout.", ".tmp");
			
			if (params.containsKey("task") && params.get("task").equals("run-tt") ) {
				
				// prepare input
				Path input = Files.createTempFile("ttin.", ".tmp");
				
				InputStream is = getCoreService().download(key);
				Files.copy(is, input, StandardCopyOption.REPLACE_EXISTING);
				
				// Avec pre-processing : tokenizer
				if ( tokenizeInput) {
					Path tokenizer = Paths.get(base, "cmd", "utf8-tokenize.perl");
					Path abbreviations = Paths.get(base, "lib", lg + "-abbreviations");
					Path inter = Files.createTempFile("ttmid.", ".tmp");
					
					// invoke tokenizer
					String[] tokenize = {tokenizer.toString(), "-f", "-a", abbreviations.toString(), input.toString()};			
					ProcessBuilder ptokenize = new ProcessBuilder(tokenize);
					ptokenize.redirectErrorStream(true);
					ptokenize.redirectOutput(inter.toFile());
					Process p1 = ptokenize.start();
					result.setLog(result.getLog() + "tokenizer subprocess called: " + Arrays.deepToString(tokenize) + "\r\n");
					if ( p1.waitFor() == 0 ) {
						result.setLog(result.getLog() + "tokenizer subprocess finished successfully\r\n");					
						result = invokeTreeTagger(result, tagger, options, par, inter, output);
					} else {
						throw new Exception("tokenizer subprocess failed.");
					}
					p1.destroy();			
					inter.toFile().deleteOnExit();
					
				// Sans pre-processing
				} else {
					result = invokeTreeTagger(result, tagger, options, par, input, output);
				} 

				input.toFile().deleteOnExit();
				//logger.log(Level.INFO, result.getOutput());
				
			}else {
				result = invokeTreeTagger(result, tagger, options, par, null, output);
			}
			output.toFile().deleteOnExit();
			
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
		options.add("-no-unknown");
		List<String> listOtherParams = Arrays.asList("task", "tokenizer", "lg-input", "txt-input", "txt-output", "lex-input", "wc-input", "eos-tag", "p", "prob", "epsilon");
		
		if( params.containsKey("task") && params.get("task").equals("run-tt") ) {			

			for (Map.Entry<String, String> entry : params.entrySet())
			{
			    if(!listOtherParams.contains(entry.getKey())){
			    	if( entry.getKey().equals("sgml")){
			    		if(Boolean.valueOf(entry.getValue())) {
			    			options.add("-"+entry.getKey());
							if( params.containsKey("eos-tag") && params.get("eos-tag")!="SENT" && params.get("eos-tag").length()>0 ) {
								options.add("-eos-tag");
								options.add(params.get("eos-tag"));
							}
			    		}
					}else if( entry.getKey().equals("info-lex")) {
						if(!entry.getValue().equals("none")){
							options.add("-"+entry.getValue());
						}
					}else if( entry.getKey().equals("threshold")){
						if( Boolean.valueOf(entry.getValue())) {
							if( params.containsKey("p")) {
								options.add("-"+entry.getKey());
								options.add(params.get("p"));
							}
							if( params.containsKey("prob") && Boolean.valueOf(params.get("prob"))) {
								options.add("-prob");
							}
						}
					}else if( entry.getKey().equals("eps")){
						if( Boolean.valueOf(entry.getValue())) {
							if( params.containsKey("epsilon")) {
								options.add("-"+entry.getKey());
								options.add(params.get("epsilon"));
							}
						}
					}else if( entry.getKey().equals("wc")){
						if( Boolean.valueOf(entry.getValue())) {
							if( params.containsKey("wc-input")) {
								options.add("-"+entry.getKey());
								Path wcInput = Files.createTempFile("ttwc.", ".tmp");								
								InputStream is = getCoreService().download(params.get("wc-input"));
								Files.copy(is, wcInput, StandardCopyOption.REPLACE_EXISTING);
								options.add(wcInput.toString());
								wcInput.toFile().deleteOnExit();
							}
						}
					}else if( entry.getKey().equals("lex")){
						if( Boolean.valueOf(entry.getValue())) {
							if( params.containsKey("lex-input")) {
								options.add("-"+entry.getKey());
								Path lexInput = Files.createTempFile("ttlex.", ".tmp");								
								InputStream is = getCoreService().download(params.get("lex-input"));
								Files.copy(is, lexInput, StandardCopyOption.REPLACE_EXISTING);
								options.add(lexInput.toString());
								lexInput.toFile().deleteOnExit();
							}
						}
					}else {
						if(Boolean.valueOf(entry.getValue())) {
							options.add("-"+entry.getKey());
						}
					}
			    }
			}
			
		} else if ( params.containsKey("task") && (params.get("task").equals("print-prob-tree") || params.get("task").equals("print-suffix-tree")) ) {
			options.add("-"+params.get("task"));
		}
		return options;
	}

	/**
	 * Invoke tree-tagger
	 * @param result
	 * @param tagger
	 * @param options
	 * @param par
	 * @param input
	 * @param output
	 * @return ToolInvokerResult
	 * @throws InterruptedException
	 * @throws Exception
	 */
	public ToolInvokerResult invokeTreeTagger(ToolInvokerResult result, Path tagger, ArrayList<String> options, Path par, Path input, Path output) throws InterruptedException, Exception{
		ArrayList<String> tagList = new ArrayList<String>();
		tagList.add(tagger.toString());
		tagList.addAll(options);
		tagList.add(par.toString());
		if(input != null && !input.toString().isEmpty()){
			tagList.add(input.toString());
		}
		String[] tag = new String[tagList.size()];
		tag = tagList.toArray(tag);
		
		ProcessBuilder ptag = new ProcessBuilder(tag);
		ptag.redirectOutput(output.toFile());
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

		result.setLog(result.getLog() + "tagger subprocess called: " + Arrays.deepToString(tag) + "\r\n");
		
		final int exitValue = p2.waitFor();
	    final String processOutput = writer.toString();
		
		if(exitValue == 0) {
			result.setLog(result.getLog() + "tagger subprocess finished successfully\r\n");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
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
			throw new Exception("tagger subprocess failed.");
		}
	}
}
