package fr.ortolang.diffusion.api.rest.workspace;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataObject;

@XmlRootElement(name = "workspace-element")
public class WorkspaceElementRepresentation {

	@XmlAttribute
	private String path;
	private String[] pathParts;
	private String key;
	private String workspace;
	private String name;
	private int clock;
	private long size;
	private String description;
	private String preview;
	private String stream;
	private String type;
	private String mimeType;
	private String format;
	private String target;
	private long modification;
	private long creation;
	private Set<CollectionElement> elements;
	private Set<MetadataElement> metadatas;

	public WorkspaceElementRepresentation() {
		elements = new HashSet<CollectionElement>();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getClock() {
		return clock;
	}

	public void setClock(int clock) {
		this.clock = clock;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String[] getPathParts() {
		return pathParts;
	}

	public void setPathParts(String[] pathparts) {
		this.pathParts = pathparts;
	}

	public String getWorkspace() {
		return workspace;
	}

	public void setWorkspace(String workspace) {
		this.workspace = workspace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimetype) {
		this.mimeType = mimetype;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public long getModification() {
		return modification;
	}

	public void setModification(long modification) {
		this.modification = modification;
	}

	public long getCreation() {
		return creation;
	}

	public void setCreation(long creation) {
		this.creation = creation;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getPreview() {
		return preview;
	}

	public void setPreview(String preview) {
		this.preview = preview;
	}

	public String getStream() {
		return stream;
	}

	public void setStream(String stream) {
		this.stream = stream;
	}

	public Set<CollectionElement> getElements() {
		return elements;
	}

	public void setElements(Set<CollectionElement> elements) {
		this.elements = elements;
	}

	public Set<MetadataElement> getMetadatas() {
		return metadatas;
	}

	public void setMetadatas(Set<MetadataElement> metadatas) {
		this.metadatas = metadatas;
	}

	public static WorkspaceElementRepresentation fromCollection(Collection collection) {
		WorkspaceElementRepresentation representation = new WorkspaceElementRepresentation();
		representation.setKey(collection.getKey());
		representation.setClock(collection.getClock());
		representation.setName(collection.getName());
		representation.setSize(collection.getElements().size());
		representation.setType(Collection.OBJECT_TYPE);
		representation.setMimeType(Collection.MIME_TYPE);
		representation.setElements(collection.getElements());
		representation.setMetadatas(collection.getMetadatas());
		representation.setDescription(collection.getDescription());
		return representation;
	}

	public static WorkspaceElementRepresentation fromMetadataObject(MetadataObject metadata) {
		WorkspaceElementRepresentation representation = new WorkspaceElementRepresentation();
		representation.setKey(metadata.getKey());
		representation.setName(metadata.getName());
		representation.setSize(metadata.getSize());
		representation.setType(MetadataObject.OBJECT_TYPE);
		representation.setMimeType(metadata.getContentType());
		representation.setFormat(metadata.getFormat());
		representation.setStream(metadata.getStream());
		representation.setTarget(metadata.getTarget());
		return representation;
	}

	public static WorkspaceElementRepresentation fromDataObject(DataObject dataobject) {
		WorkspaceElementRepresentation representation = new WorkspaceElementRepresentation();
		representation.setKey(dataobject.getKey());
		representation.setClock(dataobject.getClock());
		representation.setName(dataobject.getName());
		representation.setSize(dataobject.getSize());
		representation.setType(DataObject.OBJECT_TYPE);
		representation.setMimeType(dataobject.getMimeType());
		representation.setStream(dataobject.getStream());
		representation.setMetadatas(dataobject.getMetadatas());
		representation.setPreview(dataobject.getPreview());
		representation.setDescription(dataobject.getDescription());
		return representation;
	}

	public static WorkspaceElementRepresentation fromLink(Link link) {
		WorkspaceElementRepresentation representation = new WorkspaceElementRepresentation();
		representation.setKey(link.getKey());
		representation.setClock(link.getClock());
		representation.setName(link.getName());
		representation.setType(Link.OBJECT_TYPE);
		representation.setMimeType(Link.MIME_TYPE);
		representation.setTarget(link.getTarget());
		representation.setMetadatas(link.getMetadatas());
		return representation;
	}

	public static WorkspaceElementRepresentation fromOrtolangObject(OrtolangObject object) {
		if (object instanceof Collection) {
			return WorkspaceElementRepresentation.fromCollection((Collection) object);
		}
		if (object instanceof DataObject) {
			return WorkspaceElementRepresentation.fromDataObject((DataObject) object);
		}
		if (object instanceof MetadataObject) {
			return WorkspaceElementRepresentation.fromMetadataObject((MetadataObject) object);
		}
		if (object instanceof Link) {
			return WorkspaceElementRepresentation.fromLink((Link) object);
		}
		return null;
	}
	
}
