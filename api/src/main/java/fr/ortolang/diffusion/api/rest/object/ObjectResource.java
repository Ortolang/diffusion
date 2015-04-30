package fr.ortolang.diffusion.api.rest.object;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangObjectVersion;
import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
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
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.search.SearchServiceException;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authentication.TicketHelper;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

/**
 * @resourceDescription Operations on Objects
 */
@Path("/objects")
@Produces({ MediaType.APPLICATION_JSON })
public class ObjectResource {

	private static final Logger LOGGER = Logger.getLogger(ObjectResource.class.getName());

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
	 * 
	 * @responseType fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation
	 * @param offset
	 *            Offset of the first row to return
	 * @param limit
	 *            Maximum number of rows to return
	 * @param itemsOnly
	 *            Only get top items (items displayed in market home)
	 * @param status
	 *            {@link fr.ortolang.diffusion.OrtolangObjectState.Status}
	 * @return {@link fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation}
	 * @throws BrowserServiceException
	 */
	@GET
	public Response list(@DefaultValue(value = "0") @QueryParam(value = "offset") int offset, @DefaultValue(value = "25") @QueryParam(value = "limit") int limit,
			@QueryParam(value = "service") String service, @QueryParam(value = "type") String type,
			@DefaultValue(value = "false") @QueryParam(value = "items") boolean itemsOnly, @QueryParam(value = "status") String status) throws BrowserServiceException {
		LOGGER.log(Level.INFO, "GET /objects?offset=" + offset + "&limit=" + limit + "&items-only=" + itemsOnly + "&status=" + status);
		List<String> keys = browser.list(offset, limit, (service!=null && service.length()>0)?service:"", (type!=null && type.length()>0)?type:"", (status != null && status.length() > 0) ? OrtolangObjectState.Status.valueOf(status) : null, itemsOnly);
		long nbentries = browser.count((service!=null && service.length()>0)?service:"", (type!=null && type.length()>0)?type:"", (status != null && status.length() > 0) ? OrtolangObjectState.Status.valueOf(status) : null, itemsOnly);
		UriBuilder objects = DiffusionUriBuilder.getRestUriBuilder().path(ObjectResource.class);

		GenericCollectionRepresentation<String> representation = new GenericCollectionRepresentation<String>();
		for (String key : keys) {
			representation.addEntry(key);
		}
		representation.setOffset((offset <= 0) ? 1 : offset);
		representation.setSize(nbentries);
		representation.setLimit(keys.size());
		representation.setFirst(objects.clone().queryParam("offset", 0).queryParam("limit", limit).build());
		representation.setPrevious(objects.clone().queryParam("offset", Math.max(0, (offset - limit))).queryParam("limit", limit).build());
		representation.setSelf(objects.clone().queryParam("offset", offset).queryParam("limit", limit).build());
		representation.setNext(objects.clone().queryParam("offset", (nbentries > (offset + limit)) ? (offset + limit) : offset).queryParam("limit", limit).build());
		representation.setLast(objects.clone().queryParam("offset", ((nbentries - 1) / limit) * limit).queryParam("limit", limit).build());
		return Response.ok(representation).build();
	}

	/**
	 * Get Object by key
	 * 
	 * @responseType fr.ortolang.diffusion.api.rest.object.ObjectRepresentation
	 * @param key
	 *            The object key
	 * @return ObjectRepresentation
	 * @throws BrowserServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 * @throws SecurityServiceException
	 * @throws OrtolangException
	 */
	@GET
	@Path("/{key}")
	public Response get(@PathParam(value = "key") String key, @Context Request request) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, SecurityServiceException,
			OrtolangException {
		LOGGER.log(Level.INFO, "GET /objects/" + key);

		OrtolangObjectState state = browser.getState(key);
		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		if (state.isLocked()) {
			cc.setMaxAge(691200);
			cc.setMustRevalidate(false);
		} else {
			cc.setMaxAge(0);
			cc.setMustRevalidate(true);
		}
		Date lmd = new Date(state.getLastModification() / 1000 * 1000);
		ResponseBuilder builder = null;
		if (System.currentTimeMillis() - state.getLastModification() > 1000) {
			builder = request.evaluatePreconditions(lmd);
		}

		if (builder == null) {
			OrtolangObject object = browser.findObject(key);
			OrtolangObjectInfos infos = browser.getInfos(key);
			List<OrtolangObjectProperty> properties = browser.listProperties(key);
			String owner = security.getOwner(key);
			Map<String, List<String>> permissions = security.listRules(key);
			// TODO add history of object into representation

			ObjectRepresentation representation = new ObjectRepresentation();
			representation.setKey(key);
			representation.setService(object.getObjectIdentifier().getService());
			representation.setType(object.getObjectIdentifier().getType());
			representation.setId(object.getObjectIdentifier().getId());
			representation.setStatus(state.getStatus());
			representation.setLock(state.getLock());
			if (state.isHidden()) {
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
			for (OrtolangObjectProperty property : properties) {
				representation.getProperties().put(property.getName(), property.getValue());
			}

			builder = Response.ok(representation);
			builder.lastModified(lmd);
		}

		builder.cacheControl(cc);
		return builder.build();
	}

	@GET
	@Path("/{key}/element")
	public Response resolve(@PathParam(value = "key") String key, @QueryParam(value = "path") String relativePath, @Context Request request) throws OrtolangException, KeyNotFoundException,
			AccessDeniedException, InvalidPathException, BrowserServiceException, SecurityServiceException, CoreServiceException {
		LOGGER.log(Level.INFO, "GET /objects/" + key + "?path=" + relativePath);

		return get(core.resolvePathFromCollection(key, relativePath), request);
	}

	/**
	 * Get Object history by key
	 * 
	 * @responseType fr.ortolang.diffusion.api.rest.object.ObjectRepresentation
	 * @param key
	 *            The object key
	 * @return ObjectRepresentation
	 * @throws BrowserServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 */
	@GET
	@Path("/{key}/history")
	public Response history(@PathParam(value = "key") String key, @Context Request request) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.INFO, "get history of object " + key);

		OrtolangObjectState state = browser.getState(key);
		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		if (state.isLocked()) {
			cc.setMaxAge(691200);
			cc.setMustRevalidate(false);
		} else {
			cc.setMaxAge(0);
			cc.setMustRevalidate(true);
		}
		Date lmd = new Date(state.getLastModification() / 1000 * 1000);
		ResponseBuilder builder = null;
		if (System.currentTimeMillis() - state.getLastModification() > 1000) {
			builder = request.evaluatePreconditions(lmd);
		}

		if (builder == null) {
			List<OrtolangObjectVersion> versions = browser.getHistory(key);

			GenericCollectionRepresentation<OrtolangObjectVersion> representation = new GenericCollectionRepresentation<OrtolangObjectVersion>();
			for (OrtolangObjectVersion version : versions) {
				representation.addEntry(version);
			}

			builder = Response.ok(representation);
			builder.lastModified(lmd);
		}

		builder.cacheControl(cc);
		return builder.build();
	}

	@GET
	@Path("/{key}/keys")
	public Response listKeys(@PathParam(value = "key") String key) throws OrtolangException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.INFO, "list keys contains in object " + key);

		List<String> keys = this.listKeys(key, new ArrayList<String>());

		GenericCollectionRepresentation<String> representation = new GenericCollectionRepresentation<String>();
		for (String keyE : keys) {
			representation.addEntry(keyE);
		}
		return Response.ok(representation).build();
	}

	@GET
	@Path("/{key}/download")
	public Response download(final @PathParam(value = "key") String key, @Context Request request) throws BrowserServiceException, KeyNotFoundException,
			AccessDeniedException, OrtolangException, DataNotFoundException, IOException, CoreServiceException {
		LOGGER.log(Level.INFO, "GET /objects/" + key + "/download");
		
		OrtolangObjectState state = browser.getState(key);
		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		if (state.isLocked()) {
			cc.setMaxAge(691200);
			cc.setMustRevalidate(false);
		} else {
			cc.setMaxAge(0);
			cc.setMustRevalidate(true);
		}
		Date lmd = new Date(state.getLastModification() / 1000 * 1000);
		ResponseBuilder builder = null;
		if (System.currentTimeMillis() - state.getLastModification() > 1000) {
			builder = request.evaluatePreconditions(lmd);
		}
		
		if (builder == null) {
			builder = Response.ok();
			OrtolangObject object = browser.findObject(key);
			if (object instanceof DataObject) {
				builder.header("Content-Disposition", "attachment; filename=" + object.getObjectName());
				builder.type(((DataObject) object).getMimeType());
				builder.lastModified(lmd);
			}
			if (object instanceof MetadataObject) {
				builder.header("Content-Disposition", "attachment; filename=" + object.getObjectName());
				builder.type(((MetadataObject) object).getContentType());
				builder.lastModified(lmd);
			}
			if (object instanceof Collection) {
				builder.header("Content-Disposition", "attachment; filename=" + key + ".zip");
				builder.type("application/zip");
				builder.lastModified(lmd);
				
				StreamingOutput stream = new StreamingOutput() {
			        public void write(OutputStream output) throws IOException, WebApplicationException {
			        	try {
			        		ZipOutputStream out = exportToZip(key, new ZipOutputStream(output), PathBuilder.newInstance());
			        		out.flush();
			        		out.close();
						} catch (OrtolangException | KeyNotFoundException | AccessDeniedException e) {
							throw new IOException(e);
						}
			        }
				};
				builder.entity(stream);
				
			} else {
				InputStream input = core.download(key);
				builder.entity(input);
			}
		} 
		builder.cacheControl(cc);
		return builder.build();
	}

	@GET
	@Path("/{key}/download/ticket")
	public Response downloadTicket(@PathParam(value = "key") String key, @QueryParam(value = "hash") String hash, @Context HttpServletResponse response) throws AccessDeniedException,
			OrtolangException, KeyNotFoundException, BrowserServiceException {
		LOGGER.log(Level.INFO, "GET /objects/" + key + "/download/ticket");
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
	public void preview(@PathParam(value = "key") String key, @Context HttpServletResponse response) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException, OrtolangException,
			DataNotFoundException, IOException, CoreServiceException {
		LOGGER.log(Level.INFO, "GET /objects/" + key + "/preview");
		OrtolangObject object = browser.findObject(key);
		if (object instanceof DataObject) {
			response.setHeader("Content-Disposition", "attachment; filename=" + object.getObjectName());
			response.setContentType(((DataObject) object).getMimeType());
			response.setContentLength((int) ((DataObject) object).getSize());
		}
		InputStream input = core.preview(key);
		try {
			IOUtils.copy(input, response.getOutputStream());
		} finally {
			IOUtils.closeQuietly(input);
		}
	}

    @GET
    @Path("/{key}/size")
    public Response getObjectSize(@PathParam(value = "key") String key, @Context HttpServletResponse response) throws AccessDeniedException, OrtolangException, KeyNotFoundException, CoreServiceException {
        LOGGER.log(Level.INFO, "GET /objects/" + key + "/size");
        OrtolangObjectSize ortolangObjectSize = core.getSize(key);
        return Response.ok(ortolangObjectSize).build();
    }
    
    @POST
    @Path("/{key}/index")
    public Response reindex(@PathParam(value = "key") String key, @Context HttpServletResponse response) throws AccessDeniedException, KeyNotFoundException, BrowserServiceException {
        LOGGER.log(Level.INFO, "POST /objects/" + key + "/index");
        browser.index(key);
        return Response.ok().build();
    }

	@GET
	@Path("/semantic")
	public Response semanticSearch(@QueryParam(value = "query") String query) throws SearchServiceException {
		LOGGER.log(Level.INFO, "GET /objects/semantic?query=" + query);
		if (query != null && query.length() > 0) {
			String queryEncoded = "";
			try {
				queryEncoded = URLDecoder.decode(query, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				LOGGER.log(Level.WARNING, "cannot decode URL " + query);
			}
			LOGGER.log(Level.INFO, "searching objects with semantic query: " + queryEncoded);
			String results = search.semanticSearch(queryEncoded, "json");
			return Response.ok(results).build();
		} else {
			return Response.ok("").build();
		}
	}

	@GET
	@Path("/index")
	public Response plainTextSearch(@QueryParam(value = "query") String query) throws SearchServiceException {
		LOGGER.log(Level.INFO, "searching objects with plain text query: " + query);
		List<OrtolangSearchResult> results;
		if (query != null && query.length() > 0) {
			results = search.indexSearch(query);
		} else {
			results = Collections.emptyList();
		}
		return Response.ok(results).build();
	}

	@GET
	@Path("/json")
	public Response jsonSearch(@QueryParam(value = "query") String query) throws SearchServiceException {
		LOGGER.log(Level.INFO, "searching objects with json query: " + query);
		List<String> results;
		if (query != null && query.length() > 0) {
			results = search.jsonSearch(query);
		} else {
			results = Collections.emptyList();
		}
		return Response.ok(results).build();
	}

	protected List<String> listKeys(String key, List<String> keys) throws OrtolangException, KeyNotFoundException, AccessDeniedException {

		OrtolangObject object = browser.findObject(key);
		String type = object.getObjectIdentifier().getType();

		keys.add(key);

		if (type.equals(Collection.OBJECT_TYPE)) {
			Set<CollectionElement> elements = ((Collection) object).getElements();

			for (CollectionElement element : elements) {
				listKeys(element.getKey(), keys);
			}
		}

		if (object instanceof MetadataSource) {
			Set<MetadataElement> metadatas = ((MetadataSource) object).getMetadatas();

			for (MetadataElement metadata : metadatas) {
				keys.add(metadata.getKey());
			}
		}

		return keys;
	}

	protected ZipOutputStream exportToZip(String key, ZipOutputStream zos, PathBuilder path) throws OrtolangException, KeyNotFoundException, AccessDeniedException, IOException {

		OrtolangObject object = browser.findObject(key);
		String type = object.getObjectIdentifier().getType();

		LOGGER.log(Level.INFO, "export collection to zip : " + path.build() + " (" + key + ")");

		ZipEntry ze = new ZipEntry(path.build() + PathBuilder.PATH_SEPARATOR);

		zos.putNextEntry(ze);
		zos.closeEntry();

		if (type.equals(Collection.OBJECT_TYPE)) {
			Set<CollectionElement> elements = ((Collection) object).getElements();

			for (CollectionElement element : elements) {

				try {
					PathBuilder pathElement = path.clone().path(element.getName());
					if (element.getType().equals(Collection.OBJECT_TYPE)) {

						exportToZip(element.getKey(), zos, pathElement);

					} else if (element.getType().equals(DataObject.OBJECT_TYPE)) {
						try {

							DataObject dataObject = (DataObject) browser.findObject(element.getKey());

							LOGGER.log(Level.INFO, "export dataobject to zip : " + pathElement.build() + " (" + element.getKey() + ")");
							ZipEntry entry = new ZipEntry(pathElement.build());
							entry.setTime(element.getModification());
							entry.setSize(dataObject.getSize());
							zos.putNextEntry(entry);
							InputStream input = core.download(element.getKey());
							try {
								IOUtils.copy(input, zos);
							} catch (IOException e) {

							} finally {
								IOUtils.closeQuietly(input);
								zos.closeEntry();
							}
						} catch (CoreServiceException | DataNotFoundException e1) {
							LOGGER.log(Level.SEVERE, "unexpected error during export to zip !!", e1);
						}
					}
				} catch (InvalidPathException e) {
					LOGGER.log(Level.SEVERE, "invalid path during export to zip !!", e);
				}

			}
		}

		return zos;
	}
}
