package fr.ortolang.diffusion.api.content.metadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DcDocument {
	private List<DcValue> title;
	private List<DcValue> description;
	private List<DcValue> date;
	private List<DcValue> creator;
	private List<DcValue> publisher;
	private List<DcValue> subject;
	private List<DcValue> type;
	private List<DcValue> contributor;
	private List<DcValue> format;
	private List<DcValue> identifier;
	private List<DcValue> source;
	private List<DcValue> language;
	private List<DcValue> relation;
	private List<DcValue> coverage;
	private List<DcValue> rights;
	
	public DcDocument() {
		title = new ArrayList<DcValue>();
		description = new ArrayList<DcValue>();
		date = new ArrayList<DcValue>();
		creator = new ArrayList<DcValue>();
		publisher = new ArrayList<DcValue>();
		subject = new ArrayList<DcValue>();
		type = new ArrayList<DcValue>();
		contributor = new ArrayList<DcValue>();
		format = new ArrayList<DcValue>();
		identifier = new ArrayList<DcValue>();
		source = new ArrayList<DcValue>();
		language = new ArrayList<DcValue>();
		relation = new ArrayList<DcValue>();
		coverage = new ArrayList<DcValue>();
		rights = new ArrayList<DcValue>();
	}
	
	public List<DcValue> getType() {
		return type;
	}
	public void setType(List<DcValue> type) {
		this.type = type;
	}
	public List<DcValue> getContributor() {
		return contributor;
	}
	public void setContributor(List<DcValue> contributor) {
		this.contributor = contributor;
	}
	public List<DcValue> getFormat() {
		return format;
	}
	public void setFormat(List<DcValue> format) {
		this.format = format;
	}
	public List<DcValue> getIdentifier() {
		return identifier;
	}
	public void setIdentifier(List<DcValue> identifier) {
		this.identifier = identifier;
	}
	public List<DcValue> getSource() {
		return source;
	}
	public void setSource(List<DcValue> source) {
		this.source = source;
	}
	public List<DcValue> getLanguage() {
		return language;
	}
	public void setLanguage(List<DcValue> language) {
		this.language = language;
	}
	public List<DcValue> getRelation() {
		return relation;
	}
	public void setRelation(List<DcValue> relation) {
		this.relation = relation;
	}
	public List<DcValue> getCoverage() {
		return coverage;
	}
	public void setCoverage(List<DcValue> coverage) {
		this.coverage = coverage;
	}
	public List<DcValue> getRights() {
		return rights;
	}
	public void setRights(List<DcValue> rights) {
		this.rights = rights;
	}
	public List<DcValue> getTitle() {
		return title;
	}
	public void setTitle(List<DcValue> title) {
		this.title = title;
	}
	public List<DcValue> getDescription() {
		return description;
	}
	public void setDescription(List<DcValue> description) {
		this.description = description;
	}
	public List<DcValue> getDate() {
		return date;
	}
	public void setDate(List<DcValue> date) {
		this.date = date;
	}
	public List<DcValue> getCreator() {
		return creator;
	}
	public void setCreator(List<DcValue> creator) {
		this.creator = creator;
	}
	public List<DcValue> getPublisher() {
		return publisher;
	}
	public void setPublisher(List<DcValue> publisher) {
		this.publisher = publisher;
	}
	public List<DcValue> getSubject() {
		return subject;
	}
	public void setSubject(List<DcValue> subject) {
		this.subject = subject;
	}
	
	public static DcDocument valueOf(File content) throws JsonParseException, JsonMappingException, IOException {
		DcDocument dcDocument = new DcDocument();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readTree(content);
		
		if (jsonNode.has("title")) {
			dcDocument.setTitle(DcValue.valueOf(jsonNode, "title"));
		}
		if (jsonNode.has("description")) {
			dcDocument.setDescription(DcValue.valueOf(jsonNode, "description"));
		}
		if (jsonNode.has("date")) {
			dcDocument.setDate(DcValue.valueOf(jsonNode, "date"));
		}
		if (jsonNode.has("creator")) {
			dcDocument.setCreator(DcValue.valueOf(jsonNode, "creator"));
		}
		if (jsonNode.has("publisher")) {
			dcDocument.setPublisher(DcValue.valueOf(jsonNode, "publisher"));
		}
		if (jsonNode.has("subject")) {
			dcDocument.setSubject(DcValue.valueOf(jsonNode, "subject"));
		}
		if (jsonNode.has("contributor")) {
			dcDocument.setContributor(DcValue.valueOf(jsonNode, "contributor"));
		}
		if (jsonNode.has("type")) {
			dcDocument.setType(DcValue.valueOf(jsonNode, "type"));
		}
		if (jsonNode.has("format")) {
			dcDocument.setFormat(DcValue.valueOf(jsonNode, "format"));
		}
		if (jsonNode.has("identifier")) {
			dcDocument.setIdentifier(DcValue.valueOf(jsonNode, "identifier"));
		}
		if (jsonNode.has("source")) {
			dcDocument.setContributor(DcValue.valueOf(jsonNode, "source"));
		}
		if (jsonNode.has("language")) {
			dcDocument.setLanguage(DcValue.valueOf(jsonNode, "language"));
		}
		if (jsonNode.has("relation")) {
			dcDocument.setRelation(DcValue.valueOf(jsonNode, "relation"));
		}
		if (jsonNode.has("coverage")) {
			dcDocument.setCoverage(DcValue.valueOf(jsonNode, "coverage"));
		}
		if (jsonNode.has("rights")) {
			dcDocument.setRights(DcValue.valueOf(jsonNode, "rights"));
		}
		return dcDocument;
	}
}
