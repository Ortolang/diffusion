package fr.ortolang.diffusion.oai.format.handler;

import static fr.ortolang.diffusion.oai.format.Constant.*;

import java.io.StringReader;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.apache.commons.io.FileUtils;

import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

public class DataCiteHandler implements MetadataHandler {

    private Profile owner;
    private String organization;
	private List<String> listHandles;
	
	public DataCiteHandler() { 
		owner = null;
		organization = null;
		listHandles = null;
	}

	/**
	 * Converts a JSON (string representation) metadata object to an XML DublinCore metadata object.
	 * @param item
	 * @return
	 */
	@Override
	public void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException {
		StringReader reader = new StringReader(item);
		JsonReader jsonReader = Json.createReader(reader);
		JsonObject jsonDoc = jsonReader.readObject();

		try {
			writeDocument(builder);
			
			// Identifier
			if (listHandles != null) {
				XmlDumpAttributes attrsIdentifier = new XmlDumpAttributes();
				attrsIdentifier.put("identifierType", "Handle");
				builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, DC_ELEMENTS.get(6), attrsIdentifier, 
						listHandles.get(0).replaceAll("http://hdl.handle.net/", ""));
			}
			
			// Creator
			if (owner != null) {
				builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_CREATORS_ELEMENT);
				builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_CREATOR_ELEMENT);
				XmlDumpAttributes attrsCreatorName = new XmlDumpAttributes();
				attrsCreatorName.put(OAI_DATACITE_NAME_TYPE_ATTRIBUTE, OAI_DATACITE_NAME_TYPE_PERSONAL_VALUE);
				builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_CREATOR_NAME_ELEMENT, attrsCreatorName, owner.getFamilyName() + "," + owner.getGivenName());
				builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_GIVEN_NAME_ELEMENT, owner.getGivenName());
				builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_FAMILY_NAME_ELEMENT, owner.getFamilyName());
				if (organization != null) {
					builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_AFFILIATION_ELEMENT, organization);
				}
				builder.writeEndElement(); // end of creator
				builder.writeEndElement(); // end of creators
			}
			
			// Title
			builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_TITLES_ELEMENT);
			writeDataCiteElement(OAI_DATACITE_TITLE_ELEMENT, jsonDoc, builder);
			builder.writeEndElement(); // end of titles
			
			// Publisher is fixed with ORTOLANG
			builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, DC_ELEMENTS.get(8), "ORTOLANG");
			
			// Contributors
			JsonArray contributors = jsonDoc.getJsonArray("contributors");
			JsonArray sponsors = jsonDoc.getJsonArray("sponsors");
			if (contributors != null || sponsors != null) {
				builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_CONTRIBUTORS_ELEMENT);
				if (contributors != null) {
					for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
						JsonArray roles = contributor.getJsonArray("roles");
						for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
							
							String roleId = role.getString("id");
							XmlDumpAttributes attrsContributorType = new XmlDumpAttributes();
							switch(roleId) {
							case "editor":
								attrsContributorType.put(OAI_DATACITE_CONTRIBUTOR_TYPE_ATTRIBUTE, OAI_DATACITE_EDITOR_VALUE); break;
							case "sponsor":
								attrsContributorType.put(OAI_DATACITE_CONTRIBUTOR_TYPE_ATTRIBUTE, OAI_DATACITE_SPONSOR_VALUE); break;
							case "researcher":
								attrsContributorType.put(OAI_DATACITE_CONTRIBUTOR_TYPE_ATTRIBUTE, OAI_DATACITE_RESEARCHER_VALUE); break;
							default:
								attrsContributorType.put(OAI_DATACITE_CONTRIBUTOR_TYPE_ATTRIBUTE, OAI_DATACITE_OTHER_VALUE);
							}
							builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_CONTRIBUTOR_ELEMENT, attrsContributorType);
							
							if (roleId.equals("sponsor")) {
								JsonObject entityContributor = contributor.getJsonObject("entity");
								String fullname = entityContributor.getJsonString("fullname").getString();
								builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_CONTRIBUTOR_NAME_ELEMENT, fullname);
							} else {
								JsonObject entityContributor = contributor.getJsonObject("entity");
								String lastname = entityContributor.getJsonString("lastname").getString();
								String firstname = entityContributor.getJsonString("firstname").getString();
								
								builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_CONTRIBUTOR_NAME_ELEMENT, lastname + ", " + firstname);
								builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_GIVEN_NAME_ELEMENT, firstname);
								builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_FAMILY_NAME_ELEMENT, lastname);
								if (entityContributor.containsKey("organization")) {
									builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_AFFILIATION_ELEMENT, 
											entityContributor.getJsonObject("organization").getJsonString("fullname").getString());
								}
							}
							
							builder.writeEndElement(); // end of contributor
						}
					}
				}
				
				// Sponsor
				if (sponsors != null) {
					for (JsonObject sponsor : sponsors.getValuesAs(JsonObject.class)) {
						String fullname = sponsor.getJsonString("fullname").getString();
						XmlDumpAttributes attrsContributorType = new XmlDumpAttributes();
						attrsContributorType.put(OAI_DATACITE_CONTRIBUTOR_TYPE_ATTRIBUTE, OAI_DATACITE_SPONSOR_VALUE);
						builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_CONTRIBUTOR_ELEMENT, attrsContributorType);
						builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_CONTRIBUTOR_NAME_ELEMENT, fullname);
						builder.writeEndElement(); // end of contributor
					}
				}
				builder.writeEndElement(); // end of contributors
			}
			
			// Publisher year
			JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
        	builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_PUBLICATIONYEAR_ELEMENT, publicationDate.getString().substring(0, 4));
			// Resource type
			XmlDumpAttributes attrsResourceType = new XmlDumpAttributes();
			
			JsonString resourceType = jsonDoc.getJsonString("type");
			switch(resourceType.getString()) {
				case ORTOLANG_RESOURCE_TYPE_CORPORA:
				case ORTOLANG_RESOURCE_TYPE_LEXICON:
				case ORTOLANG_RESOURCE_TYPE_TERMINOLOGY:
					attrsResourceType.put(OAI_DATACITE_RESOURCE_TYPE_ATTRIBUTE, OAI_DATACITE_RESOURCE_TYPE_DATASET_VALUE); break;
				case ORTOLANG_RESOURCE_TYPE_TOOL:
					attrsResourceType.put(OAI_DATACITE_RESOURCE_TYPE_ATTRIBUTE, OAI_DATACITE_RESOURCE_TYPE_SOFTWARE_VALUE); break;
				case CMDI_RESOURCE_CLASS_WEBSITE:
					attrsResourceType.put(OAI_DATACITE_RESOURCE_TYPE_ATTRIBUTE, OAI_DATACITE_RESOURCE_TYPE_SERVICE_VALUE); break;
			}
			builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_RESOURCE_TYPE_ELEMENT, attrsResourceType, resourceType.getString());
			
			// Date
			builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_DATES_ELEMENT);
			XmlDumpAttributes attrsDate = new XmlDumpAttributes();
			attrsDate.put(OAI_DATACITE_DATE_TYPE_ELEMENT, OAI_DATACITE_DATE_TYPE_ISSUED_VALUE);
			builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_DATE_ELEMENT, attrsDate, publicationDate.getString());
			builder.writeEndElement(); // end of dates
			// Description
			builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_DESCRIPTIONS_ELEMENT);
			XmlDumpAttributes attrsDescriptionType = new XmlDumpAttributes();
			attrsDescriptionType.put(OAI_DATACITE_DESCRIPTION_TYPE_ATTRIBUTE, OAI_DATACITE_DESCRIPTION_TYPE_ABSTRACT_VALUE);
			JsonArray elmArray = jsonDoc.getJsonArray("description");
			for (JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
				attrsDescriptionType.put("xml:lang", elm.getString("lang"));
				builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_DESCRIPTION_ELEMENT, attrsDescriptionType, XMLDocument.removeHTMLTag(elm.getString("value")));
			}
			builder.writeEndElement(); // end of descriptions
			// Rights
			builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_RIGHTS_LIST_ELEMENT);
			JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
			String idStatusOfUse = statusOfUse.getString("id");
			if (idStatusOfUse.equals("free_use")) {
				XmlDumpAttributes attrsStatusOfUse = new XmlDumpAttributes();
				attrsStatusOfUse.put(OAI_DATACITE_RIGHTS_URI_ATTRIBUTE, OAI_DATACITE_RIGHTS_URI_OPENACCESS_VALUE);
				builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_RIGHTS_ELEMENT, attrsStatusOfUse, null);
			} else {
				XmlDumpAttributes attrsStatusOfUse = new XmlDumpAttributes();
				attrsStatusOfUse.put(OAI_DATACITE_RIGHTS_URI_ATTRIBUTE, OAI_DATACITE_RIGHTS_URI_RESTRICTED_VALUE);
				builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_RIGHTS_ELEMENT, attrsStatusOfUse, null);
				
			}
			JsonObject license = jsonDoc.getJsonObject("license");
			if (license != null) {
				XmlDumpAttributes attrsLicense = null;
				if (license.containsKey("text")) {
					JsonArray textArray = license.getJsonArray("text");
					for (JsonObject elm : textArray.getValuesAs(JsonObject.class)) {
						if (elm.containsKey("lang") && elm.getString("lang").equals("en")) {
							JsonObject textValueObj = elm.getJsonObject("value");
							attrsLicense = new XmlDumpAttributes();
							attrsLicense.put(OAI_DATACITE_RIGHTS_URI_ATTRIBUTE, textValueObj.getString("url"));
						}
					}
				}
				if (attrsLicense != null) {
					builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_RIGHTS_ELEMENT, attrsLicense, XMLDocument.removeHTMLTag(license.getString("label")));
				} else {
					builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_RIGHTS_ELEMENT, XMLDocument.removeHTMLTag(license.getString("label")));
				}
			}
			builder.writeEndElement(); // end of rights
			// Language
			JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
			if (corporaLanguages != null) {
				for (JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
					if (corporaLanguage.containsKey("id")) {
						builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, "language", corporaLanguage.getString("id"));
					}
				}
			}
			// Size
			JsonString datasize = jsonDoc.getJsonString("datasize");
			long size = Long.valueOf(datasize.getString());
			String sizeToDisplay = FileUtils.byteCountToDisplaySize(size);
			builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_SIZES_ELEMENT);
			builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_SIZE_ELEMENT, sizeToDisplay);
			builder.writeEndElement(); // end of sizes
			// Subject
			builder.writeStartElement(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_SUBJECTS_ELEMENT);
			writeDataCiteElement("keywords", jsonDoc, OAI_DATACITE_SUBJECT_ELEMENT, builder);
			builder.writeEndElement(); // end of subjects
			
			builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataHandlerException("unable to build OAI_DATACITE cause " + e.getMessage(), e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}
	

	@Override
	public void write(String json, MetadataBuilder builder) throws MetadataHandlerException {
		// not implemented
	}
	
	/**
	 * Writes the root element of the XML Document
	 * @param builder
	 * @throws MetadataBuilderException
	 */
	public static void writeDocument(MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
		namespaces.put(OAI_DATACITE_NAMESPACE_PREFIX, new XmlDumpNamespace(OAI_DATACITE_NAMESPACE_URI, OAI_DATACITE_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(XSI_NAMESPACE_URI));
		builder.setNamespaces(namespaces);
		builder.writeStartDocument(OAI_DATACITE_NAMESPACE_PREFIX, OAI_DATACITE_DOCUMENT, null);
	}


	public static void writeDataCiteElement(String elementName, JsonObject meta, MetadataBuilder builder) throws MetadataBuilderException {
		writeDataCiteElement(elementName, meta, elementName, builder);
	}
	
	public static void writeDataCiteElement(String elementName, JsonObject meta, String tagName, MetadataBuilder builder) throws MetadataBuilderException {
		if (meta.containsKey(elementName)) {
			JsonArray elmArray = meta.getJsonArray(elementName);
			for (JsonObject elm : elmArray.getValuesAs(JsonObject.class)) {
				if (elm.containsKey("lang") && elm.containsKey("value")) {
					writeDataCiteMultilingualElement(tagName, elm, builder);
				} else {
					if (elm.containsKey("value")) {
						builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, tagName, XMLDocument.removeHTMLTag(elm.getString("value")));
					}
				}
			}
		}
	}
	
	public static void writeDataCiteMultilingualElement(String tag, JsonObject multilingualObject, MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpAttributes attrs = new XmlDumpAttributes();
		if (multilingualObject.getString("lang").matches(iso639_2pattern)) {
			attrs.put("xml:lang", multilingualObject.getString("lang"));
		}
		builder.writeStartEndElement(OAI_DATACITE_NAMESPACE_PREFIX, tag, attrs, XMLDocument.removeHTMLTag(multilingualObject.getString("value")));
	}

	public Profile getOwner() {
		return owner;
	}


	public void setOwner(Profile owner) {
		this.owner = owner;
	}


	public List<String> getListHandlesRoot() {
		return listHandles;
	}

	public void setListHandlesRoot(List<String> listHandlesRoot) {
		this.listHandles = listHandlesRoot;
	}

	public String getOrganization() {
		return organization;
	}

	public void setOrganization(String organization) {
		this.organization = organization;
	}
}
