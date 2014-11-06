package fr.ortolang.diffusion.api.rest.object;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.MetadataSource;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.search.SearchServiceException;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

@Path("/objects")
@Produces({ MediaType.APPLICATION_JSON })
public class ObjectResource {

	private Logger logger = Logger.getLogger(ObjectResource.class.getName());

	@Context
	private UriInfo uriInfo;
	@EJB
	private BrowserService browser;
	@EJB
	private SearchService search;
	@EJB
	private SecurityService security;
	@EJB
	private CoreService core;

	public ObjectResource() {
	}

	@GET
	@Template( template="objects/list.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response list(@DefaultValue(value = "0") @QueryParam(value = "offset") int offset, @DefaultValue(value = "25") @QueryParam(value = "limit") int limit)
			throws BrowserServiceException {
		logger.log(Level.INFO, "GET /objects?offset=" + offset + "&limit=" + limit);
		List<String> keys = browser.list(offset, limit, "", "");
		long nbentries = browser.count("", "");
		UriBuilder objects = DiffusionUriBuilder.getRestUriBuilder().path(ObjectResource.class);

		GenericCollectionRepresentation<String> representation = new GenericCollectionRepresentation<String> ();
		for ( String key : keys ) {
			representation.addEntry(key);
		}
		representation.setOffset((offset<=0)?1:offset);
		representation.setSize(nbentries);
		representation.setLimit(keys.size());
		representation.setFirst(objects.clone().queryParam("offset", 0).queryParam("limit", limit).build());
		representation.setPrevious(objects.clone().queryParam("offset", Math.max(0, (offset - limit))).queryParam("limit", limit).build());
		representation.setSelf(objects.clone().queryParam("offset", offset).queryParam("limit", limit).build());
		representation.setNext(objects.clone().queryParam("offset", (nbentries > (offset + limit)) ? (offset + limit) : offset).queryParam("limit", limit).build());
		representation.setLast(objects.clone().queryParam("offset", ((nbentries - 1) / limit) * limit).queryParam("limit", limit).build());
		Response response = Response.ok(representation).build();
		return response;
	}

	@GET
	@Path("/{key}")
	@Template( template="objects/detail.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response get(@PathParam(value = "key") String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, SecurityServiceException, OrtolangException {
		logger.log(Level.INFO, "GET /objects/" + key);
		OrtolangObject object = browser.findObject(key);
		OrtolangObjectState state = browser.getState(key);
		OrtolangObjectInfos infos = browser.getInfos(key);
		List<OrtolangObjectProperty> properties = browser.listProperties(key);
		String owner = security.getOwner(key);
		Map<String, List<String>> permissions = security.listRules(key);
		//TODO add history of object into representation
		
		ObjectRepresentation representation = new ObjectRepresentation();
		representation.setKey(key);
		representation.setService(object.getObjectIdentifier().getService());
		representation.setType(object.getObjectIdentifier().getType());
		representation.setId(object.getObjectIdentifier().getId());
		representation.setStatus(state.getStatus());
		representation.setLock(state.getLock());
		if ( state.isHidden() ) {
			representation.setVisibility("hidden");
		} else {
			representation.setVisibility("visible");
		}
		representation.setOwner(owner);
		representation.setPermissions(permissions);
		representation.setObject(object);
		representation.setAuthor(infos.getAuthor());
		representation.setCreationDate(infos.getCreationDate() + "");
		representation.setLastModificationDate(infos.getLastModificationDate() + "");
		for ( OrtolangObjectProperty property : properties ) {
			representation.getProperties().put(property.getName(), property.getValue());
		}
		return Response.ok(representation).build();
	}
	
	@GET
	@Path("/{key}/keys")
	public Response listKeys(@PathParam(value = "key") String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "list keys contains in object "+key);
		
		List<String> keys = this.listKeys(key, new ArrayList<String>());
		
		GenericCollectionRepresentation<String> representation = new GenericCollectionRepresentation<String> ();
		for ( String keyE : keys ) {
			representation.addEntry(keyE);
		}
		return Response.ok(representation).build();
	}
	
	@GET
	@Path("/{key}/download")
	public void download(@PathParam(value = "key") String key, @Context HttpServletResponse response) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, OrtolangException, DataNotFoundException, IOException, CoreServiceException {
		logger.log(Level.INFO, "GET /objects/" + key + "/download");
		OrtolangObject object = browser.findObject(key);
		if ( object instanceof DataObject ) {
			response.setHeader("Content-Disposition", "attachment; filename=" + object.getObjectName());
			response.setContentType(((DataObject)object).getMimeType());
			response.setContentLength((int) ((DataObject)object).getSize());
		}
		if ( object instanceof MetadataObject ) {
			response.setHeader("Content-Disposition", "attachment; filename=" + object.getObjectName());
			response.setContentType(((MetadataObject)object).getContentType());
			response.setContentLength((int) ((MetadataObject)object).getSize());
		}
		
		if ( object instanceof Collection ) {
			
			response.setHeader("Content-Disposition", "attachment; filename=" + object.getObjectName() + ".zip");
			response.setContentType("application/zip");
//			response.setContentLength((int) ((MetadataObject)object).getSize());

			ZipOutputStream zos = exportToZip(key, new ZipOutputStream(response.getOutputStream()), PathBuilder.newInstance());
			zos.finish();
			zos.close();
			logger.log(Level.INFO, "End of zipping");
		} else {
			InputStream input = core.download(key);

			try {
			    IOUtils.copy(input, response.getOutputStream());
			} finally {
	            IOUtils.closeQuietly(input);
	        }
		}
		
	}
	
	@GET
	@Path("/{key}/preview")
	public void preview(@PathParam(value = "key") String key, @Context HttpServletResponse response) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, OrtolangException, DataNotFoundException, IOException, CoreServiceException {
		logger.log(Level.INFO, "GET /objects/" + key + "/preview");
		OrtolangObject object = browser.findObject(key);
		if ( object instanceof DataObject ) {
			response.setHeader("Content-Disposition", "attachment; filename=" + object.getObjectName());
			response.setContentType(((DataObject)object).getMimeType());
			response.setContentLength((int) ((DataObject)object).getSize());
		}
		InputStream input = core.preview(key);
		try {
		    IOUtils.copy(input, response.getOutputStream());
		} finally {
            IOUtils.closeQuietly(input);
        }
	}
	

	@GET
	@Path("/semantic")
	@Template( template="semantic/query.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML})
	public Response semanticSearch(@QueryParam(value = "query") String query) throws SearchServiceException {
		logger.log(Level.INFO, "GET /objects/semantic?query=" + query);
		if ( query != null && query.length() > 0 ) {
			String queryEncoded = "";
			try {
				queryEncoded = URLDecoder.decode(query, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.log(Level.WARNING, "cannot decode URL "+query);
			}
			logger.log(Level.INFO, "searching objects with semantic query: " + queryEncoded);
			String results = search.semanticSearch(queryEncoded, "json");
			return Response.ok(results).build();
		} else {
			return Response.ok("").build();
		}
	}


	/**
	 * List of keys contains in a object.
	 * @param key
	 * @param keys
	 * @return
	 * @throws AccessDeniedException 
	 * @throws KeyNotFoundException 
	 * @throws OrtolangException 
	 */
	protected List<String> listKeys(String key, List<String> keys) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
		
		OrtolangObject object = browser.findObject(key);
		String type = object.getObjectIdentifier().getType();
		
		keys.add(key);
		
		if(type.equals(Collection.OBJECT_TYPE)) {
			Set<CollectionElement> elements = ((Collection) object).getElements();
			
			for(CollectionElement element : elements) {
				listKeys(element.getKey(), keys);
			}
		}
		
		if(object instanceof MetadataSource) {
			Set<MetadataElement> metadatas = ((MetadataSource) object).getMetadatas();
			
			for(MetadataElement metadata : metadatas) {
				keys.add(metadata.getKey());
			}
		}
			
		return keys;
	}

	/**
	 * List of keys contains in a object.
	 * @param key
	 * @param keys
	 * @return
	 * @throws AccessDeniedException 
	 * @throws KeyNotFoundException 
	 * @throws OrtolangException 
	 * @throws IOException 
	 */
	protected ZipOutputStream exportToZip(String key, ZipOutputStream zos, PathBuilder path) throws OrtolangException, KeyNotFoundException, AccessDeniedException, IOException {
		
		OrtolangObject object = browser.findObject(key);
		String type = object.getObjectIdentifier().getType();
		
		logger.log(Level.INFO, "export collection to zip : "+path.build()+" ("+key+")");

		ZipEntry ze = new ZipEntry(path.build() + PathBuilder.PATH_SEPARATOR);
		
		zos.putNextEntry(ze);
		zos.closeEntry();
		
		if(type.equals(Collection.OBJECT_TYPE)) {
			Set<CollectionElement> elements = ((Collection) object).getElements();
			
			for(CollectionElement element : elements) {
				
				try {
					PathBuilder pathElement = path.clone().path(element.getName());
					if(element.getType().equals(Collection.OBJECT_TYPE)) {
					
							exportToZip(element.getKey(), zos, pathElement);
						
					} else if(element.getType().equals(DataObject.OBJECT_TYPE)) {
						try {
							
							DataObject dataObject = (DataObject) browser.findObject(element.getKey());
							
							logger.log(Level.INFO, "export dataobject to zip : "+pathElement.build()+" ("+element.getKey()+")");
							ZipEntry entry = new ZipEntry(pathElement.build());
							entry.setTime(element.getModification());
							entry.setSize(dataObject.getSize());
							zos.putNextEntry(entry);
							InputStream input = core.download(element.getKey());
							try {
								IOUtils.copy(input, zos);
							} catch(IOException e) {
								
							} finally {
								IOUtils.closeQuietly(input);
								zos.closeEntry();
							}
						} catch (CoreServiceException | DataNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				} catch (InvalidPathException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		
		return zos;
	}
}
