package fr.ortolang.diffusion.test.builder;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;

public class CollectionBuilder implements FileVisitor<Path> {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private WebTarget base;
	
	private List<String> keys;
	private Map<Path, String> collections;
	private int nbCollections = 0;
	private int nbDataObjects = 0;
	private long volume = 0;

	public CollectionBuilder(WebTarget base, Map<Path, String> collections) {
		logger.log(Level.FINE, "New CollectionBuilder");
		
		this.base = base;
		this.collections = collections;
		this.keys = new ArrayList<String>();
	}

	public Map<Path, String> getCollections() {
		return collections;
	}
	
	public List<String> getKeys() {
		return keys;
	}

	public long getTotalLength() {
		return volume;
	}
	
	public int getNbCollections() {
		return nbCollections;
	}
	
	public int getNbDataObjects() {
		return nbDataObjects;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		logger.log(Level.FINE, "Creating collection for path : " + dir);
		WebTarget collectionsTarget = base.path("/core/collections");
		Form newcollection = new Form().param("name", dir.getFileName().toString()).param("description", "A collection corresponding to provided folder " + dir.getFileName());
		Response response5 = collectionsTarget.request(MediaType.APPLICATION_FORM_URLENCODED).accept(MediaType.MEDIA_TYPE_WILDCARD)
				.post(Entity.entity(newcollection, MediaType.APPLICATION_FORM_URLENCODED));
		if (response5.getStatus() != Status.CREATED.getStatusCode()) {
			logger.log(Level.WARNING, "Unexpected response code while trying to create collection : " + response5.getStatus());
			fail("Unable to create collection");
		}
		String key = response5.getLocation().getPath().substring(response5.getLocation().getPath().lastIndexOf("/"));
		nbCollections++;
		collections.put(dir, key);
		keys.add(key.substring(1));
		logger.log(Level.INFO, "Created collection key : " + key);
		if (collections.containsKey(dir.getParent())) {
			logger.log(Level.FINE, "Adding the created collection as a member of its parent : " + dir.getParent());
			String parent = collections.get(dir.getParent());
			Response response6 = collectionsTarget.path(parent).path("elements").path(key).request().accept(MediaType.MEDIA_TYPE_WILDCARD).put(Entity.entity(key, MediaType.TEXT_PLAIN));
			if (response6.getStatus() != Status.NO_CONTENT.getStatusCode()) {
				logger.log(Level.WARNING, "Unexpected response code while trying to add element to collection : " + response6.getStatus());
				fail("Unable to add element to collection");
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		logger.log(Level.FINE, "Creating DataObject for file : " + file);
		WebTarget objectsTarget = base.path("/core/objects");
		WebTarget collectionsTarget = base.path("/core/collections");
		
		File thefile = file.toFile();
		FileDataBodyPart filePart = new FileDataBodyPart("file", thefile);
		MultiPart multipart = new FormDataMultiPart()
	    .field("name", file.getFileName().toString())
	    .field("description", "A data object corresponding to provided file " + file.getFileName())
	    .bodyPart(filePart);
		Response response7 = objectsTarget.request().accept(MediaType.MEDIA_TYPE_WILDCARD).post(Entity.entity(multipart, multipart.getMediaType()));
		if ( response7.getStatus() != Status.CREATED.getStatusCode() ) {
			logger.log(Level.WARNING, "Unexpected response code while trying to create dataobject : " + response7.getStatus() );
			fail("Unable to create dataobject");
		}
		nbDataObjects++;
		volume += thefile.length();
		String key = response7.getLocation().getPath().substring(response7.getLocation().getPath().lastIndexOf("/"));
		keys.add(key.substring(1));
		logger.log(Level.INFO, "Created data object key : " + key);
		if ( collections.containsKey(file.getParent()) ) {
			logger.log(Level.FINE, "Adding the created dataobject as a member of its parent : " + file.getParent());
			String parent = collections.get(file.getParent());
			Response response8 = collectionsTarget.path(parent).path("elements").path(key).request().accept(MediaType.MEDIA_TYPE_WILDCARD).put(Entity.entity(key, MediaType.TEXT_PLAIN));
			if ( response8.getStatus() != Status.NO_CONTENT.getStatusCode() ) {
				logger.log(Level.WARNING, "Unexpected response code while trying to add element to collection : " + response8.getStatus() );
				fail("Unable to add element to collection");
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		logger.log(Level.WARNING, "Unable to create DataObject for file : " + file, exc);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

}
