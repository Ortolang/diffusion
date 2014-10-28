package fr.ortolang.diffusion.tool.treetagger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.output.ByteArrayOutputStream;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
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
			if ( !params.containsKey("lg-input") || params.get("lg-input").length() == 0 ) {
				throw new Exception("parameter lg-input is mandatory");
			} else {
				lg = params.get("lg-input");
			}
			if ( !params.containsKey("txt-input") || params.get("txt-input").length() == 0 ) {
				throw new Exception("parameter txt-input is mandatory");
			} else {
				key = params.get("txt-input");
			}
			
			Path tokenizer = Paths.get(base, "cmd", "utf8-tokenize.perl");
			Path tagger = Paths.get(base, "bin", "tree-tagger");
			Path abbreviations = Paths.get(base, "lib", lg + "-abbreviations");
			Path par = Paths.get(base, "lib", lg + "-utf8.par");
			
			Path input = Files.createTempFile("ttin.", ".tmp");
			Path inter = Files.createTempFile("ttmid.", ".tmp");
			Path output = Files.createTempFile("ttout.", ".tmp");
			
			InputStream is = getCoreService().download(key);
			Files.copy(is, input, StandardCopyOption.REPLACE_EXISTING);
			
			String[] tokenize = {tokenizer.toString(), "-f", "-a", abbreviations.toString(), input.toString()};
			
			ProcessBuilder ptokenize = new ProcessBuilder(tokenize);
			ptokenize.redirectErrorStream(true);
			ptokenize.redirectOutput(inter.toFile());
			Process p1 = ptokenize.start();
			result.setLog(result.getLog() + "tokenizer subprocess called: " + Arrays.deepToString(tokenize) + "\r\n");
			if ( p1.waitFor() == 0 ) {
				result.setLog(result.getLog() + "tokenizer subprocess finished successfully\r\n");
				String[] tag = {tagger.toString(), "-token", "-lemma", "-sgml", "-no-unknown", par.toString(), inter.toString(), output.toString()};
				ProcessBuilder ptag = new ProcessBuilder(tag);
				ptag.redirectErrorStream(true);
				Process p2 = ptag.start(); 
				result.setLog(result.getLog() + "tagger subprocess called: " + Arrays.deepToString(tag) + "\r\n");
				if(p2.waitFor() == 0) {
					result.setLog(result.getLog() + "tagger subprocess finished successfully\r\n");
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					Files.copy(output, baos);
					result.setOutput(baos.toString());
					result.setStatus(ToolInvokerResult.Status.SUCCESS);
					result.setStop(System.currentTimeMillis());
				} else {
					throw new Exception("tagger subprocess failed.");
				}
			} else {
				throw new Exception("tokenizer subprocess failed.");
			}
		} catch ( Exception e ) {
			e.printStackTrace();
			result.setLog(result.getLog() + "unexpected error occured during tool execution: " + e.getMessage() + "\r\n");
			result.setStatus(ToolInvokerResult.Status.ERROR);
			result.setStop(System.currentTimeMillis());
		}
		return result;
	}

}
