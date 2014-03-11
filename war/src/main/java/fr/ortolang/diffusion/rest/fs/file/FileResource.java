package fr.ortolang.diffusion.rest.fs.file;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.util.GenericType;

import com.healthmarketscience.rmiio.RemoteInputStreamServer;
import com.healthmarketscience.rmiio.SimpleRemoteInputStream;

import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.CoreServiceLocal;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.rest.core.object.DigitalObjectResource;
import fr.ortolang.diffusion.rest.fs.folder.FolderResource;

/**
 * A file is identified by the key of a Reference which must target an object.
 * @author cyril
 *
 */
@Path("/fs/files")
@Produces({ MediaType.APPLICATION_JSON })
public class FileResource {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Context
    private UriInfo uriInfo;
	@EJB 
	private BrowserService browser;
	@EJB 
	private CoreService core;
	@EJB 
	private CoreServiceLocal coreLocal;
 
 
    public FileResource() {
    }

    /**
     * Create a file (object + dynamic reference).
     * @param name
     * @param description
     * @return
     * @throws CoreServiceException
     * @throws KeyAlreadyExistsException
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response create( MultipartFormDataInput input) throws CoreServiceException, KeyAlreadyExistsException {
    	logger.log(Level.INFO, "creating file");
    	// Generate a key for the registry
    	String key = UUID.randomUUID().toString();
    	Map<String, List<InputPart>> formParts = input.getFormDataMap();
    	String name = "No name provided";
    	try {
	    	List<InputPart> inParts = formParts.get("name");
	    	for (InputPart inputPart : inParts) {
	    		name = inputPart.getBodyAsString();
	    	}
	    	String description = "No description provided";
	    	inParts = formParts.get("description");
	    	for (InputPart inputPart : inParts) {
	    		description = inputPart.getBodyAsString();
	    	}
	    	InputStream is = null;
	    	inParts = formParts.get("file");
	    	for (InputPart inputPart : inParts) {
	    		is = inputPart.getBody(new GenericType<InputStream>() { });
	    	}
	    	if ( coreLocal != null ) {
	    		logger.log(Level.INFO, "using local core interface for optimisation");
	    		coreLocal.createObject(key, name, description, is);
	    	} else {
	    		logger.log(Level.INFO, "using remote core interface");
	    		RemoteInputStreamServer ris = new SimpleRemoteInputStream(is);
	    		core.createObject(key, name, description, ris.export());
	    	}
	    	
    	} catch ( IOException ioe ) {
    		return Response.serverError().entity(ioe.getMessage()).build();
    	}

    	String keyRef = UUID.randomUUID().toString();
    	logger.log(Level.INFO, "create reference to target "+key);
		core.createReference(keyRef, true, name, key);
    	
    	URI newly = UriBuilder.fromUri(uriInfo.getBaseUri()).path(FileResource.class).path(keyRef).build();
    	return Response.created(newly).build();
    }
    
}
