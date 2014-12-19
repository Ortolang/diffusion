package fr.ortolang.diffusion.tool.invoke;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.language.LanguageIdentifier;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import fr.ortolang.diffusion.tool.ToolConfig;
import fr.ortolang.diffusion.tool.client.OrtolangDiffusionRestClient;
import fr.ortolang.diffusion.tool.job.ToolJobException;
import fr.ortolang.diffusion.tool.job.ToolJobInvocationListener;
import fr.ortolang.diffusion.tool.job.ToolJobService;
import fr.ortolang.diffusion.tool.job.entity.ToolJob;
import fr.ortolang.diffusion.tool.job.entity.ToolJobStatus;

@Stateless
@Local(ToolJobInvocation.class)
public class TikaInvocation implements ToolJobInvocation {

	private Logger logger = Logger.getLogger(ToolJobInvocationListener.class.getName());
	private boolean initialized = false;

	@EJB
	private ToolJobService tjob;

    private String base;
	private ToolJob currentJob;
	private Path jobFolder;
	
	public TikaInvocation() {
	}

	/**
	 * Tool invoker initialization
	 * @param job
	 * @param logFile
	 * @throws SecurityException
	 * @throws IOException
	 */
	private void init(ToolJob job, Path logFile) throws SecurityException, IOException{
		logger.log(Level.INFO, "Initializing Tika Tool...");		

		// Set attribute
		currentJob = job;
		base = ToolConfig.getInstance().getProperty("tool.working.space.path");
		jobFolder = Paths.get(base, currentJob.getId());
		
		// Logger init
		FileHandler fhandler = new FileHandler(logFile.toString(), true);
		fhandler.setFormatter(new SimpleFormatter());
		logger.addHandler(fhandler);		
				
		logger.log(Level.INFO, "Tika Tool initialized");
	}
	
	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public ToolJobInvocationResult invoke(ToolJob job, Path logFile) throws SecurityException, IOException {
		
		if ( !initialized ) {
			this.init(job, logFile);
		}
		
		logger.log(Level.INFO, "Starting tika invocation...");
		job.setStatus(ToolJobStatus.RUNNING);
		
		ToolJobInvocationResult result = new ToolJobInvocationResult();
		try {
			
			// prepare input			
			logger.log(Level.INFO, "Getting input file...");
			String key;
			Path pathInput;
			
			if ( !currentJob.getParameters().containsKey("input") || currentJob.getParameter("input").length() == 0 ) {
				throw new ToolJobException("parameter input is mandatory");
			} else {
				key = currentJob.getParameter("input");
			}
			
			// init
			logger.log(Level.INFO, "Trying to retrieve job data object on remote diffusion server...");
			pathInput = getDataObject(key);
			//pathInput = getLocalDataObject(key); // For testing purpose
			String fileName = pathInput.getFileName().toString();
						
			logger.log(Level.INFO, "Input file downloaded");			
			InputStream is = new FileInputStream(pathInput.toString());
			
			Path resultPath;
			String output = "";
			Metadata metadata = new Metadata();
			metadata.set(Metadata.RESOURCE_NAME_KEY, fileName);
			
			// run tika
			Tika tika = new Tika();
			ParseContext context = new ParseContext();
	        DefaultDetector detector = new DefaultDetector();
	        AutoDetectParser parser = new AutoDetectParser(detector);
	        context.set(Parser.class, parser);
	        
			if( currentJob.getParameters().containsKey("output") && !currentJob.getParameter("output").isEmpty() ){
				switch (currentJob.getParameter("output")) {
				case "detect": //-d  or --detect        Detect document type
					logger.log(Level.INFO, "detecting document type...");				
					String filetype = tika.detect(is);
					result.setPreview("Document type detected : " + filetype);
					logger.log(Level.INFO, "saving document type detected : " + filetype + "...");	
			        JsonValue files = null ;
					resultPath = Paths.get(jobFolder.toString(), fileName + ".filetype.txt");
					Files.write(resultPath, output.getBytes());
					result.setFile(fileName + ".filetype.txt", resultPath.toString());
					break;
					
				case "metadata": //-m  or --metadata      Output only metadata
					logger.log(Level.INFO, "retrieving document metadata...");				
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
					result.setPreview(output);
					logger.log(Level.INFO, "saving metadata...");	
					resultPath = Paths.get(jobFolder.toString(), fileName + ".metadata.txt");
					Files.write(resultPath, output.getBytes());					
					result.setFile(fileName + ".metadata.txt", resultPath.toString());
					break;
					
				case "language": //-l  or --language      Output only language
					logger.log(Level.INFO, "detecting language...");				
					LanguageIdentifier identifier = new LanguageIdentifier(tika.parseToString(is));
					String language = identifier.getLanguage();
					result.setPreview("language detected : " + language);
					logger.log(Level.INFO, "saving language detected : " + language + "...");				
					resultPath = Paths.get(jobFolder.toString(), fileName + ".language.txt");
					Files.write(resultPath, output.getBytes());			
					result.setFile(fileName + ".language.txt", resultPath.toString());
					break;
					
				case "encoding": //return file encoding
					logger.log(Level.INFO, "detecting file encoding...");				
					CharsetDetector charsetDetector = new CharsetDetector();
					charsetDetector.setText(is);
					CharsetMatch match = charsetDetector.detect();
					if(match != null)
			        {
						try
			            {
							Charset inputEncoding = Charset.forName(match.getName());
							result.setPreview("charset detected : " + inputEncoding.toString());
							logger.log(Level.INFO, "saving charset detected : " + inputEncoding.toString() + "...");				
							resultPath = Paths.get(jobFolder.toString(), fileName + ".charset.txt");
							Files.write(resultPath, output.getBytes());			
							result.setFile(fileName + ".charset.txt", resultPath.toString());	
			            }
			            catch(UnsupportedCharsetException e)
			            {
			            	logger.log(Level.WARNING, "Charset detected as " + match.getName() + " but the JVM does not support this, detection skipped");									
			                break;
			            }
			        } else {
			        	logger.log(Level.SEVERE, "Charset detection failed.");									
		                throw(new ToolJobException("Charset detection failed."));
			        }
					
					break;
					
				case "extract" :
					logger.log(Level.INFO, "extracting file content...");				
					output = tika.parseToString(is).trim();
					result.setPreview(output);
					resultPath = Paths.get(jobFolder.toString(), fileName + ".content.txt");
					logger.log(Level.INFO, "saving file content in a file...");				
					Files.write(resultPath, output.getBytes());
					result.setFile(fileName + ".content.txt", resultPath.toString());
					break;
	
				default: // by default parse file
					logger.log(Level.INFO, "extracting file content...");				
					output = tika.parseToString(is).trim();
					result.setPreview(output);
					resultPath = Paths.get(jobFolder.toString(), fileName + ".content.txt");
					logger.log(Level.INFO, "saving file content in a file...");				
					Files.write(resultPath, output.getBytes());
					result.setFile(fileName + ".content.txt", resultPath.toString());
					break;
				}
			} else{
				// by default parse file
				logger.log(Level.INFO, "extracting file content...");				
				output = tika.parseToString(is).trim();
				result.setPreview(output);
				resultPath = Paths.get(jobFolder.toString(), fileName + ".content.txt");
				logger.log(Level.INFO, "saving file content in a file...");				
				Files.write(resultPath, output.getBytes());
				result.setFile(fileName + ".content.txt", resultPath.toString());
			}
			
			logger.log(Level.INFO, "tika subprocess finished successfully\r\n");
			result.setStatus(ToolJobInvocationResult.Status.SUCCESS);
			return result;
				
		} catch (ToolJobException | IOException | TikaException | SAXException e) {
			e.printStackTrace();
			result.setStatus(ToolJobInvocationResult.Status.ERROR);
			logger.log(Level.SEVERE, "unexpected error occured during tika execution: " + e.getMessage(), e);
			return result;
		}
	}

	/**
	 * Get an ortolang dataobject with given key
	 * 
	 * @param String key
	 * @return Path Path of the file on the server
	 * @throws IOException
	 * @throws ToolJobException
	 */
	private Path getDataObject(String key) throws IOException, ToolJobException {
		String urlstr = ToolConfig.getInstance().getProperty( "ortolang.diffusion.host.url")
				+ ToolConfig.getInstance().getProperty( "ortolang.diffusion.rest.url");
		URL url = new URL(urlstr + "/objects/" + key + "/download");
		
		// Temporary hack
		String username = ToolConfig.getInstance().getProperty( "ortolang.diffusion.username");
		String password = ToolConfig.getInstance().getProperty( "ortolang.diffusion.password");
		String token = username + ":" + password;
        String base64Token = new String(Base64.encodeBase64(token.getBytes(StandardCharsets.UTF_8)));
		
		try {
			OrtolangDiffusionRestClient client = new OrtolangDiffusionRestClient( urlstr, username, password);

			if (client.objectExists(key)) {
				JsonObject object = client.getObject(key);
				String fileName = object.getJsonObject("object").getString( "name");
				logger.log(Level.INFO, "Retrieving " + fileName + " from remote diffusion server...");
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestProperty ("Authorization", "Basic " + base64Token);
			    connection.setRequestMethod("GET");
			    InputStream input = (InputStream) connection.getInputStream();
				logger.log(Level.INFO, "Copying " + fileName + " into working job folder...");
				Path path = Paths.get(jobFolder.toString(), fileName);
				try{
					Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
					logger.log(Level.INFO, fileName + " retrieved.");
				} finally {
					IOUtils.closeQuietly(input);
				}
				return path;
			} else {
				logger.log(Level.SEVERE, "unable to find dataobject with key " + key);
				throw new ToolJobException("unable to find dataobject with key " + key);
			}
		} catch (IOException e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, "exception raised : " + e.getLocalizedMessage());
			throw new ToolJobException("exception raised : " + e.getLocalizedMessage());
		}
	}
	
	// For testing purpose
	private Path getLocalDataObject(String key) throws IOException,
			ToolJobException {
		String fileName = "test.txt";
		logger.log(Level.INFO, fileName + "retrieved from remote diffusion server");
		Path input = Paths.get("/home/cmoro/test.txt");
		Path path = Paths.get(jobFolder.toString(), fileName);
		Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
		logger.log(Level.INFO, fileName + "copied into working job folder.");			
		return path;

	}

}
