package fr.ortolang.diffusion.tool.tika;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.activiti.engine.impl.util.json.JSONArray;
import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.apache.tika.sax.BodyContentHandler;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.tool.invoke.ToolInvoker;
import fr.ortolang.diffusion.tool.invoke.ToolInvokerResult;

public class TikaInvoker implements ToolInvoker {
	
	private Logger logger = Logger.getLogger(TikaInvoker.class.getName());
	
	private CoreService core;
	private boolean initialized = false;

	public TikaInvoker() {
	}
	
	private void init() throws Exception {
		logger.log(Level.INFO, "Initializing Tika Tool");
		logger.log(Level.INFO, "Tika Tool initialized");
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
			HashMap<String, String> listFileResult = new HashMap<String,String>();
			Path resultPath;
			String output = "";
			
			// prepare input			
			if ( !params.containsKey("txt-input") || params.get("txt-input").length() == 0 ) {
				throw new Exception("parameter txt-input is mandatory");
			} else {
				key = params.get("txt-input");
			}								
			
			InputStream is = getCoreService().download(key);
			String outputFileName = getCoreService().readDataObject(key).getObjectName();
			Metadata metadata = new Metadata();
			metadata.set(Metadata.RESOURCE_NAME_KEY, outputFileName);
			
			// run tika
			Tika tika = new Tika();
			ParseContext context = new ParseContext();
	        DefaultDetector detector = new DefaultDetector();
	        AutoDetectParser parser = new AutoDetectParser(detector);
	        context.set(Parser.class, parser);
	        
			if( params.containsKey("output") && !params.get("output").isEmpty() ){
				switch (params.get("output")) {
				case "detect": //-d  or --detect        Detect document type
					String filetype = tika.detect(is);
					result.setOutput(filetype.toString());
					break;
					
				case "metadata": //-m  or --metadata      Output only metadata
					BodyContentHandler handler = new BodyContentHandler();
					parser.parse(is, handler, metadata, context);
				      
					//getting the list of all meta data elements 
					String[] metadataNames = metadata.names();
					Arrays.sort(metadataNames);
				   
					for(String name : metadataNames) {
				    	for(String value : metadata.getValues(name)) {
			                 output += name + ":\t" + value + "\n";
				    	}
		            }
					resultPath = Paths.get("/tmp/" + outputFileName + "_metadata");
					Files.write(resultPath, output.getBytes());
					result.setOutput(output);
					listFileResult.put(outputFileName + "_metadata", resultPath.toString());
					result.setOutputFilePath(listFileResult);
					break;
					
				case "language": //-l  or --language      Output only language
					LanguageIdentifier identifier = new LanguageIdentifier(tika.parseToString(is));
					String language = identifier.getLanguage();
					result.setOutput(language);
					break;
					
				case "encoding": //return file encoding
					CharsetDetector charsetDetector = new CharsetDetector();
					charsetDetector.setText(is);
					CharsetMatch match = charsetDetector.detect();
					if(match != null)
			        {
						try
			            {
							Charset inputEncoding = Charset.forName(match.getName());
							logger.log(Level.INFO, inputEncoding.toString());
				            result.setOutput(inputEncoding.displayName());
			            }
			            catch(UnsupportedCharsetException e)
			            {
			                throw(new Exception("Charset detected as " + match.getName() + " but the JVM does not support this, detection skipped"));
			            }
			        } else {
			        	throw(new Exception("Charset detection failed."));
			        }
					
					break;
					
				case "extract" :
					output = tika.parseToString(is).trim();
					result.setOutput(output);
					resultPath = Paths.get("/tmp/" + outputFileName + "_content");
					Files.write(resultPath, output.getBytes());
					listFileResult.put(outputFileName + "_content", resultPath.toString());
					result.setOutputFilePath(listFileResult);
					break;

				default: // by default parse file
					output = tika.parseToString(is).trim();
					result.setOutput(output);
					resultPath = Paths.get("/tmp/" + outputFileName + "_content");
					Files.write(resultPath, output.getBytes());
					listFileResult.put(outputFileName + "_content", resultPath.toString());
					result.setOutputFilePath(listFileResult);
					break;
				}
			} else{
				// by default parse file
				output = tika.parseToString(is).trim();
				result.setOutput(output);
				resultPath = Paths.get("/tmp/" + outputFileName + "_content");
				Files.write(resultPath, output.getBytes());
				listFileResult.put(outputFileName + "_content", resultPath.toString());
				result.setOutputFilePath(listFileResult);
			}

			result.setLog(result.getLog() + "tika subprocess finished successfully\r\n");
//			result.setLog(processOutput);
			result.setStatus(ToolInvokerResult.Status.SUCCESS);
			result.setStop(System.currentTimeMillis());
			//logger.log(Level.INFO, result.getOutput());
						
		} catch ( Exception e ) {
			e.printStackTrace();
			result.setLog(result.getLog() + "unexpected error occured during tool execution: " + e.getMessage() + "\r\n");
			result.setStatus(ToolInvokerResult.Status.ERROR);
			result.setStop(System.currentTimeMillis());
		}
		return result;
	}

}
