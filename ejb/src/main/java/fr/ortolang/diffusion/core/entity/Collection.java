package fr.ortolang.diffusion.core.entity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Transient;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

@Entity
@SuppressWarnings("serial")
public class Collection extends OrtolangObject {
	
	public static final String OBJECT_TYPE = "collection";
	
	private static final int MAX_SEGMENT_SIZE = 7700;
	
	@Id
	private String id;
	@Transient
	private String key;
	private String name;
	@Column(length=2500)
	private String description;
	@ElementCollection(fetch=FetchType.EAGER)
	@Column(length=7800)
	private Set<String> segments;
	@ElementCollection(fetch=FetchType.EAGER)
	private Set<String> metadatas;
	
	public Collection() {
		segments = new HashSet<String>();
		metadatas = new HashSet<String>();
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
	
	public Set<String> getElements() {
		Set<String> elements = new HashSet<String> ();
		for ( String segment : segments ) {
			if ( segment.length() > 0 ) {
				elements.addAll(Arrays.asList(segment.split(",")));
			}
		}
		return elements;
	}
	
	public void setElements(Set<String> elements) {
		segments.clear();
		StringBuffer newsegment = new StringBuffer();
		for ( String element : elements ) {
			if ( newsegment.length() >= MAX_SEGMENT_SIZE ) {
				segments.add(newsegment.toString());
				newsegment = new StringBuffer();
			}
			if ( newsegment.length() > 0 ) {
				newsegment.append(",");
			}
			newsegment.append(element);
		}
		if ( newsegment.length() > 0 ) {
			segments.add(newsegment.toString());
		}
	}
	
	public boolean addElement(String element) {
		if ( !isElement(element) ) {
			String freesegment = "";
			for ( String segment : segments ) {
				if ( segment.length() < MAX_SEGMENT_SIZE ) {
					freesegment = segment;
					segments.remove(segment);
					break;
				}
			}
			if ( freesegment.length() > 0 ) {
				freesegment += ("," + element);
			} else {
				freesegment += element;
			}
			segments.add(freesegment);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean removeElement(String element) {
		if ( isElement(element) ) {
			String newsegment = "";
			for ( String segment : segments ) {
				if ( segment.indexOf(element) != -1 ) {
					newsegment = segment;
					segments.remove(segment);
					break;
				}
			}
			newsegment = newsegment.replaceAll("(" + element + "){1},?", "");
			if ( newsegment.endsWith(",") ) {
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
	
	private boolean isElement(String element) {
		for ( String segment : segments ) {
			if ( segment.indexOf(element) != -1 ) {
				return true;
			}
		}
		return false;
	}
	
	public void setMetadatas(Set<String> metadatas) {
		this.metadatas = metadatas;
	}
	
	public Set<String> getMetadatas() {
		return metadatas;
	}
	
	public void addMetadata(String metadata) {
		this.metadatas.add(metadata);
	}
	
	public void removeMetadata(String metadata) {
		this.metadatas.remove(metadata);
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
