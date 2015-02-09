package fr.ortolang.diffusion.api.rest.object;

import fr.ortolang.diffusion.*;
import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
import fr.ortolang.diffusion.core.PathBuilder;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.*;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.search.SearchServiceException;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authentication.TicketHelper;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

import org.apache.commons.io.IOUtils;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @resourceDescription Operations on Objects
 */
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
	@EJB
	private MembershipService membership;

	public ObjectResource() {
	}

	/**
	 * List objects
	 * @responseType fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation
	 * @param offset Offset of the first row to return
	 * @param limit Maximum number of rows to return
	 * @param itemsOnly Only get top items (items displayed in market home)
	 * @param status {@link fr.ortolang.diffusion.OrtolangObjectState.Status}
	 * @return {@link fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation}
	 * @throws BrowserServiceException
	 */
	@GET
	@Template( template="objects/list.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response list(@DefaultValue(value = "0") @QueryParam(value = "offset") int offset, @DefaultValue(value = "25") @QueryParam(value = "limit") int limit,
			@DefaultValue(value = "false") @QueryParam(value = "items") boolean itemsOnly, @QueryParam(value = "status") String status)
			throws BrowserServiceException {
		logger.log(Level.INFO, "GET /objects?offset=" + offset + "&limit=" + limit + "&items-only=" + itemsOnly + "&status=" + status);
		List<String> keys = browser.list(offset, limit, "", "", (status != null && status.length() > 0) ? OrtolangObjectState.Status.valueOf(status) : null, itemsOnly);
		long nbentries = browser.count("", "", (status != null && status.length() > 0) ? OrtolangObjectState.Status.valueOf(status) : null, itemsOnly);
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

	/**
	 * Get Object by key
	 * @responseType fr.ortolang.diffusion.api.rest.object.ObjectRepresentation
	 * @param key The object key
	 * @return ObjectRepresentation
	 * @throws BrowserServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 * @throws SecurityServiceException
	 * @throws OrtolangException
	 */
	@GET
	@Path("/{key}")
	@Template( template="objects/detail.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response get(@PathParam(value = "key") String key, @Context Request request) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, SecurityServiceException, OrtolangException {
		logger.log(Level.INFO, "GET /objects/" + key);
		
		OrtolangObjectState state = browser.getState(key);
		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		if ( state.isLocked() ) {
			cc.setMaxAge(31536000);
			cc.setMustRevalidate(false);
		} else {
			cc.setMaxAge(0);
			cc.setMustRevalidate(true);
		}
		Date lmd = new Date((state.getLastModification()/1000)*1000);
		ResponseBuilder builder = request.evaluatePreconditions(lmd);

        if(builder == null){
        	OrtolangObject object = browser.findObject(key);
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
    		
    		builder = Response.ok(representation);
    		builder.lastModified(lmd);
        }

        builder.cacheControl(cc);
        Response response = builder.build();
        return response;
	}
	
	@GET
	@Path("/{key}/element")
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
	public Response resolve(@PathParam(value = "key") String key, @QueryParam(value = "path") String relativePath, @Context Request request) throws OrtolangException, KeyNotFoundException, AccessDeniedException, InvalidPathException, BrowserServiceException, SecurityServiceException, CoreServiceException {
		logger.log(Level.INFO, "GET /objects/"+key+"?path="+relativePath);
		
		return get(core.resolvePathFromCollection(key, relativePath), request);
	}

	/**
	 * Get Object history by key
	 * @responseType fr.ortolang.diffusion.api.rest.object.ObjectRepresentation
	 * @param key The object key
	 * @return ObjectRepresentation
	 * @throws BrowserServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 */
	@GET
	@Path("/{key}/history")
	public Response history(@PathParam(value = "key") String key, @Context Request request) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "get history of object "+key);
		
		OrtolangObjectState state = browser.getState(key);
		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		if ( state.isLocked() ) {
			cc.setMaxAge(31536000);
			cc.setMustRevalidate(false);
		} else {
			cc.setMaxAge(0);
			cc.setMustRevalidate(true);
		}
		Date lmd = new Date((state.getLastModification()/1000)*1000);
		ResponseBuilder builder = request.evaluatePreconditions(lmd);
		
		if(builder == null){
			List<OrtolangObjectVersion> versions = browser.getHistory(key);
			
			GenericCollectionRepresentation<OrtolangObjectVersion> representation = new GenericCollectionRepresentation<OrtolangObjectVersion> ();
			for ( OrtolangObjectVersion version : versions ) {
				representation.addEntry(version);
			}
			
			builder = Response.ok(representation);
    		builder.lastModified(lmd);
        }

        builder.cacheControl(cc);
        Response response = builder.build();
        return response;		
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

	/**
	 * Download Object by key
	 * @param key The object key
	 * @param response
	 * @throws BrowserServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 * @throws OrtolangException
	 * @throws DataNotFoundException
	 * @throws IOException
	 * @throws CoreServiceException
	 */
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
			response.setHeader("Content-Disposition", "attachment; filename=" + key + ".zip");
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
	@Path("/{key}/download/ticket")
	public Response downloadTicket(@PathParam(value = "key") String key, @QueryParam(value = "hash") String hash, @Context HttpServletResponse response) throws AccessDeniedException, OrtolangException, KeyNotFoundException, BrowserServiceException {
		logger.log(Level.INFO, "GET /objects/" + key + "/download/ticket");
		if (hash != null) {
			browser.lookup(key);
		} else {
			OrtolangObject object = browser.findObject(key);
			if (object instanceof DataObject) {
				hash = ((DataObject) object).getStream();
			} else if (object instanceof MetadataObject) {
				hash = ((MetadataObject) object).getStream();
			}
		}
		String ticket = TicketHelper.makeTicket(membership.getProfileKeyForConnectedIdentifier(), hash);
		JsonObject jsonObject = Json.createObjectBuilder().add("t", ticket).build();
		return Response.ok(jsonObject).build();
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

	@GET
	@Path("/index")
	@Template( template="index/query.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response plainTextSearch(@QueryParam(value = "query") String query) throws SearchServiceException {
		logger.log(Level.INFO, "searching objects with plain text query: " + query);
		List<OrtolangSearchResult> results;
		if ( query != null && query.length() > 0 ) {
			results = search.indexSearch(query);
		} else {
			results = Collections.emptyList();
		}
		return Response.ok(results).build();
	}
	
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
