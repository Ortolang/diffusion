package fr.ortolang.diffusion.core.entity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Type;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@Entity
@SuppressWarnings("serial")
public class Collection extends OrtolangObject implements MetadataSource {
	
	public static final String OBJECT_TYPE = "collection";
	public static final String MIME_TYPE = "ortolang/collection";
	
	private static final int MAX_SEGMENT_SIZE = 7500;
	
	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private boolean root;
	private int clock; 
	private String name;
	@Column(length=2500)
	private String description;
	@Column(length=8000)
	@ElementCollection(fetch=FetchType.EAGER)
	private Set<String> segments;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String metadatasContent = "";
	
	@Transient
	private Set<CollectionElement> elements = null;
	
	public Collection() {
		segments = new HashSet<String>();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public boolean isRoot() {
		return root;
	}
	
	public void setRoot(boolean root) {
		this.root = root;
	}

	public int getClock() {
		return clock;
	}
	
	public void setClock(int clock) {
		this.clock = clock;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setSegments(Set<String> segments) {
		this.segments = segments;
	}
	
	public Set<String> getSegments() {
		return segments;
	}
	
	public Set<CollectionElement> getElements() {
		if ( elements != null ) {
			return elements;
		} else {
			elements = new HashSet<CollectionElement>();
			for ( String segment : segments ) {
				if ( segment.length() > 0 ) {
					for ( String element : Arrays.asList(segment.split("\n")) ) {
						elements.add(CollectionElement.deserialize(element));
					}
				}
			}
			return elements;
		}
	}
	
	public void clearElements() {
		elements = null;
		segments.clear();
	}
	
	public void setElements(Set<CollectionElement> elements) {
		this.elements = elements;
		segments.clear();
		StringBuffer newsegment = new StringBuffer();
		for ( CollectionElement element : elements ) {
			String serializedElement = element.serialize();
			if ( newsegment.length() >= (MAX_SEGMENT_SIZE+serializedElement.length()) ) {
				segments.add(newsegment.toString());
				newsegment = new StringBuffer();
			}
			if ( newsegment.length() > 0 ) {
				newsegment.append("\n");
			}
			newsegment.append(serializedElement);
		}
		if ( newsegment.length() > 0 ) {
			segments.add(newsegment.toString());
		}
	}
	
	public boolean addElement(CollectionElement element) {
		if ( !containsElement(element) ) {
			elements = null;
			String serializedElement = element.serialize();
			String freesegment = "";
			for ( String segment : segments ) {
				if ( segment.length() < (MAX_SEGMENT_SIZE+serializedElement.length()) ) {
					freesegment = segment;
					segments.remove(segment);
					break;
				}
			}
			if ( freesegment.length() > 0 ) {
				freesegment += ("\n" + serializedElement);
			} else {
				freesegment += serializedElement;
			}
			segments.add(freesegment);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean removeElement(CollectionElement element) {
		if ( containsElement(element) ) {
			elements = null;
			String newsegment = "";
			for ( String segment : segments ) {
				if ( segment.indexOf(element.serialize()) != -1 ) {
					newsegment = segment;
					segments.remove(segment);
					break;
				}
			}
			newsegment = newsegment.replaceAll("(?m)^(" + element.serialize() + ")\n?", "");
			if ( newsegment.endsWith("\n") ) {
				newsegment = newsegment.substring(0, newsegment.length()-1);
			}
			if ( newsegment.length() > 0 ) {
				segments.add(newsegment);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean containsElement(CollectionElement element) {
		for ( String segment : segments ) {
			if ( segment.indexOf(element.serialize()) != -1 ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsElementName(String name) {
		for ( String segment : segments ) {
			if ( segment.indexOf("\t" + name + "\t") != -1 ) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsElementKey(String key) {
		for ( String segment : segments ) {
			if ( segment.indexOf("\t" + key) != -1 ) {
				return true;
			}
		}
		return false;
	}
	
	public CollectionElement findElementByName(String name) {
		Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
		String nname = SPECIAL_REGEX_CHARS.matcher(name).replaceAll("\\\\$0");
		Pattern pattern = Pattern.compile("(?s).*((" + Collection.OBJECT_TYPE + "|" + DataObject.OBJECT_TYPE + "|" + Link.OBJECT_TYPE + ")\t" + nname + "\t([0-9]{13})\t([0-9]+)\t([^\t]+)\t([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})).*$");
		for ( String segment : segments ) {
			Matcher matcher = pattern.matcher(segment);
			if ( matcher.matches() ) {
				return CollectionElement.deserialize(matcher.group(1));
			}
		}
		return null;
	}
	
	public void setMetadatasContent(String metadatasContent) {
		this.metadatasContent = metadatasContent;
	}
	
	public String getMetadatasContent() {
		return metadatasContent;
	}
	
	public Set<MetadataElement> getMetadatas() {
		Set<MetadataElement> metadatas = new HashSet<MetadataElement>();
		if ( metadatasContent != null && metadatasContent.length() > 0 ) {
			for ( String metadata : Arrays.asList(metadatasContent.split("\n")) ) {
				metadatas.add(MetadataElement.deserialize(metadata));
			}
		}
		return metadatas;
	}
	
	public void setMetadatas(Set<MetadataElement> metadatas) {
		StringBuffer newmetadatas = new StringBuffer();
		for ( MetadataElement metadata : metadatas ) {
			if ( newmetadatas.length() > 0 ) {
				newmetadatas.append("\n");
			}
			newmetadatas.append(metadata.serialize());
		}
		metadatasContent = newmetadatas.toString();
	}
	
	public boolean addMetadata(MetadataElement metadata) {
		if ( !containsMetadata(metadata) ) {
			if ( metadatasContent.length() > 0 ) {
				metadatasContent += "\n" + metadata.serialize();
			} else {
				metadatasContent = metadata.serialize();
			}
			return true;
		}
		return false;
	}
	
	public boolean removeMetadata(MetadataElement metadata) {
		if ( containsMetadata(metadata) ) {
			metadatasContent = metadatasContent.replaceAll("(?m)^(" + metadata.serialize() + ")\n?", "");
			if ( metadatasContent.endsWith("\n") ) {
				metadatasContent = metadatasContent.substring(0, metadatasContent.length()-1);
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean containsMetadata(MetadataElement metadata) {
		if ( metadatasContent.length() > 0 && metadatasContent.indexOf(metadata.serialize()) != -1 ) {
			return true;
		}
		return false;
	}
	
	public boolean containsMetadataName(String name) {
		if ( metadatasContent.indexOf(name + "/") != -1 ) {
			return true;
		}
		return false;
	}
	
	public boolean containsMetadataKey(String key) {
		if ( metadatasContent.indexOf("/" + key) != -1 ) {
			return true;
		}
		return false;
	}
	
	public MetadataElement findMetadataByName(String name) {
		Pattern pattern = Pattern.compile("(?s).*(" + name + "/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})).*$");
		Matcher matcher = pattern.matcher(metadatasContent);
		if ( matcher.matches() ) {
			return MetadataElement.deserialize(matcher.group(1));
		}
		return null;
	}
	
	@Override
	public String getObjectKey() {
		return key;
	}

	@Override
	public String getObjectName() {
		return getName();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, id);
	}
	
}
