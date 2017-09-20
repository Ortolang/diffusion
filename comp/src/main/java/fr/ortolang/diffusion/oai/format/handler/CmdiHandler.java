package fr.ortolang.diffusion.oai.format.handler;

import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import fr.ortolang.diffusion.oai.exception.MetadataBuilderException;
import fr.ortolang.diffusion.oai.exception.MetadataHandlerException;
import fr.ortolang.diffusion.oai.format.Constant;
import fr.ortolang.diffusion.oai.format.XMLDocument;
import fr.ortolang.diffusion.oai.format.builder.MetadataBuilder;
import fr.ortolang.diffusion.util.DateUtils;
import fr.ortolang.diffusion.xml.XmlDumpAttributes;
import fr.ortolang.diffusion.xml.XmlDumpNamespace;
import fr.ortolang.diffusion.xml.XmlDumpNamespaces;

public class CmdiHandler implements MetadataHandler {

    private static final Logger LOGGER = Logger.getLogger(CmdiHandler.class.getName());

    private String id;
	private List<String> listHandlesRoot;
	
	@Override
	public void writeItem(String item, MetadataBuilder builder) throws MetadataHandlerException {

		StringReader reader = new StringReader(item);
		JsonReader jsonReader = Json.createReader(reader);

		try {
			JsonObject jsonDoc = jsonReader.readObject();
			
			writeCmdiDocument(builder);
			writeCmdiHeader(Constant.CMDI_MDCREATOR_VALUE, id, Constant.w3cdtf.format(new Date()), builder);
			writeCmdiResources(listHandlesRoot, builder);
			
			builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_COMPONENTS_ELEMENT);
			
			builder.writeStartElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, Constant.CMDI_OLAC_ELEMENT);

			JsonArray bibligraphicCitations = jsonDoc.getJsonArray("bibliographicCitation");
	        if(bibligraphicCitations!=null) {
	            for(JsonObject multilingualBibliographicCitation : bibligraphicCitations.getValuesAs(JsonObject.class)) {
	            	XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("xml:lang", multilingualBibliographicCitation.getString("lang"));
					builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "bibliographicCitation", attrs, XMLDocument.removeHTMLTag(multilingualBibliographicCitation.getString("value")));
	            }
	        }
	        
			JsonArray sponsors = jsonDoc.getJsonArray("sponsors");
	        if(sponsors!=null) {
	        	for(JsonObject sponsor : sponsors.getValuesAs(JsonObject.class)) {
	            	if(sponsor.containsKey("fullname")) {
	            		XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	attrs.put("olac-role", "sponsor");
				    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "contributor", attrs, sponsor.getString("fullname"));
	            	}
	            }
	        }

			JsonArray contributors = jsonDoc.getJsonArray("contributors");
			if (contributors != null) {
				for (JsonObject contributor : contributors.getValuesAs(JsonObject.class)) {
					JsonArray roles = contributor.getJsonArray("roles");
					for (JsonObject role : roles.getValuesAs(JsonObject.class)) {
						String roleId = role.getString("id");
						XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	attrs.put("olac-role", roleId);
				    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "contributor", attrs, Constant.person(contributor));
	                    
	                    if("author".equals(roleId)) {
	                    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "creator", Constant.person(contributor));
	                    }
					}
				}
			}
			
			JsonString creationDate = jsonDoc.getJsonString("originDate");
	        if(creationDate!=null) {
		        if (DateUtils.isThisDateValid(creationDate.getString())) {
		        	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "date", creationDate.getString());
		        } else {
		        	LOGGER.log(Level.WARNING, "invalid creation date : " + creationDate.getString());
		        }
	        } else {
	        	JsonString publicationDate = jsonDoc.getJsonString("publicationDate");
	            if(publicationDate!=null) {
			        if (DateUtils.isThisDateValid(publicationDate.getString())) {
			        	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "date", publicationDate.getString());
			        } else {
			        	LOGGER.log(Level.WARNING, "invalid publication date : " + publicationDate.getString());
			        }
	            }
	        }
	        
			writeCmdiOlacElement("description", jsonDoc, builder);
			
			if (listHandlesRoot != null) {
				listHandlesRoot.forEach(handleUrl -> {
					try {
						builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "identifier", handleUrl);
					} catch (MetadataBuilderException e) {
						LOGGER.log(Level.WARNING, "Unables to build XML : " + e.getMessage());
						LOGGER.log(Level.FINE, "Unables to build XML : " + e.getMessage(), e);
					}
				});
			}

			JsonArray corporaLanguages = jsonDoc.getJsonArray("corporaLanguages");
			if (corporaLanguages != null) {
				for (JsonObject corporaLanguage : corporaLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = corporaLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
						writeCmdiOlacElement("subject", label, builder);
						XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	attrs.put("olac-language", label.getString("lang"));
				    	if (label.containsKey("lang")) {
				    		attrs.put("xml:lang", label.getString("lang"));
				    	}
				    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "language", attrs, label.containsKey("value") ? XMLDocument.removeHTMLTag(label.getString("value")) : null);
//						writeOlacElement("language", "olac:language", corporaLanguage.getString("id"), label.getString("lang"), label.getString("value"), builder);
					}
				}
			}

			JsonObject license = jsonDoc.getJsonObject("license");
			if (license != null) {
				if (license != null) {
					XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("xml:lang", "fr");
					builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "license", attrs, XMLDocument.removeHTMLTag(license.getString("label")));
				}
			}
			
			JsonArray producers = jsonDoc.getJsonArray("producers");
			if (producers != null) {
				for (JsonObject producer : producers.getValuesAs(JsonObject.class)) {
					if (producer.containsKey("fullname")) {
						builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "publisher", producer.getString("fullname"));
					}
				}
			}

			JsonArray conditionsOfUse = jsonDoc.getJsonArray("conditionsOfUse");
			if (conditionsOfUse != null) {
				for (JsonObject label : conditionsOfUse.getValuesAs(JsonObject.class)) {
					writeCmdiOlacMultilingualElement("rights", label, builder);
				}
			}
			
	        JsonObject statusOfUse = jsonDoc.getJsonObject("statusOfUse");
			if (statusOfUse != null) {
				String idStatusOfUse = statusOfUse.getString("id");
				builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "rights", idStatusOfUse);

				JsonArray multilingualLabels = statusOfUse.getJsonArray("labels");
				for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
					writeCmdiOlacMultilingualElement("rights", label, builder);
				}
			}

			writeCmdiOlacElement("keywords", jsonDoc, "subject", builder);

			JsonArray linguisticSubjects = jsonDoc.getJsonArray("linguisticSubjects");
			if (linguisticSubjects != null) {
				for (JsonString linguisticSubject : linguisticSubjects.getValuesAs(JsonString.class)) {
					XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("olac-linguistic-field", linguisticSubject.getString());
					builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "subject", attrs, null);
					
				}
			}
			
			JsonArray studyLanguages = jsonDoc.getJsonArray("studyLanguages");
			if (studyLanguages != null) {
				for (JsonObject studyLanguage : studyLanguages.getValuesAs(JsonObject.class)) {
					JsonArray multilingualLabels = studyLanguage.getJsonArray("labels");

					for (JsonObject label : multilingualLabels.getValuesAs(JsonObject.class)) {
//						writeCmdiOlacElement("subject", label, builder);
						XmlDumpAttributes attrs = new XmlDumpAttributes();
				    	attrs.put("olac-language", label.getString("id"));
				    	if (label.containsKey("lang")) {
				    		attrs.put("xml:lang", label.getString("lang"));
				    	}
				    	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "subject", attrs, label.containsKey("value") ? XMLDocument.removeHTMLTag(label.getString("value")) : null);
//						writeOlacElement("subject", "olac:language", studyLanguage.getString("id"), label.getString("lang"), label.getString("value"), builder);
					}
				}
			}
			
	        if(creationDate!=null) {
	            XmlDumpAttributes attrs = new XmlDumpAttributes();
		        attrs.put("dcterms-type", "W3CDTF");
		        if (DateUtils.isThisDateValid(creationDate.getString())) {
		        	builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "temporal", attrs, creationDate.getString());
		        } else {
		        	LOGGER.log(Level.WARNING, "invalid creation date : " + creationDate.getString());
		        }
	        }
	        
			writeCmdiOlacElement("title", jsonDoc, builder);

			JsonString linguisticDataType = jsonDoc.getJsonString("linguisticDataType");
			if (linguisticDataType != null) {
				XmlDumpAttributes attrs = new XmlDumpAttributes();
		        attrs.put("olac-linguistic-type", linguisticDataType.getString());
				builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "type", attrs, null);
			}
			JsonArray discourseTypes = jsonDoc.getJsonArray("discourseTypes");
			if (discourseTypes != null) {
				for (JsonString discourseType : discourseTypes.getValuesAs(JsonString.class)) {
					XmlDumpAttributes attrs = new XmlDumpAttributes();
			        attrs.put("olac-discourse-type", discourseType.getString());
					builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, "type", attrs, null);
				}
			}
	        
			builder.writeEndElement(); // OLAC-DcmiTerms
			builder.writeEndElement(); // Components
			
        	builder.writeEndDocument();
		} catch (Exception e) {
			throw new MetadataHandlerException("unable to build CMID metadata format", e);
		} finally {
			jsonReader.close();
			reader.close();
		}
	}

	@Override
	public void write(String json, MetadataBuilder builder) throws MetadataHandlerException {
		// TODO Auto-generated method stub
		
	}

	public static void writeCmdiDocument(MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpNamespaces namespaces = new XmlDumpNamespaces();
		namespaces.put(Constant.CMDI_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.CMDI_NAMESPACE_URI, Constant.CMDI_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(Constant.CMDI_OLAC_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.CMDI_OLAC_NAMESPACE_URI, Constant.CMDI_OLAC_NAMESPACE_SCHEMA_LOCATION));
		namespaces.put(Constant.XSI_NAMESPACE_PREFIX, new XmlDumpNamespace(Constant.XSI_NAMESPACE_URI));
		builder.setNamespaces(namespaces);
		XmlDumpAttributes attrs = new XmlDumpAttributes();
		attrs.put(Constant.CMDI_VERSION_ATTRIBUTE, Constant.CMDI_VERSION_VALUE_ATTRIBUTE);
		builder.writeStartDocument(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_ELEMENT, attrs);
	}
	
	public static void writeCmdiHeader(String mdCreator, String mdSelfLink, String mdCreationDate, MetadataBuilder builder) throws MetadataBuilderException {
		builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_HEADER_ELEMENT);
		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_MDCREATOR_ELEMENT, null, mdCreator);
		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_MDCREATIONDATE_ELEMENT, mdCreationDate);
		if (mdSelfLink != null) {
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_MDSELFLINK_ELEMENT, mdSelfLink);
		}
		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_MDPROFILE_ELEMENT, Constant.CMDI_OLAC_PROFILE_VALUE);
		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_MDCOLLECTIONDISPLAYNAME_ELEMENT, null, mdCreator);
		builder.writeEndElement(); // End of Header
	}

	public static void writeCmdiResources(List<String> ressources, MetadataBuilder builder) throws MetadataBuilderException {
		builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCES_ELEMENT);
		
		if (ressources != null && !ressources.isEmpty()) {
			builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCEPROXYLIST_ELEMENT);
			XmlDumpAttributes attrs = new XmlDumpAttributes();
			attrs.put(Constant.CMDI_RESOURCEPROXYID_ELEMENT, "_"+UUID.randomUUID().toString().substring(0,8));
			builder.writeStartElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCEPROXY_ELEMENT, attrs);
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCETYPE_ELEMENT, Constant.CMDI_RESOURCETYPE_LANDINGPAGE);
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCEREF_ELEMENT, ressources.get(0));
			builder.writeEndElement();
			builder.writeEndElement();
		}
		else {
			builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCEPROXYLIST_ELEMENT);
		}
		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_JOURNALFILEPROXYLIST_ELEMENT);
		builder.writeStartEndElement(Constant.CMDI_NAMESPACE_PREFIX, Constant.CMDI_RESOURCERELATIONLISTT_ELEMENT);
		builder.writeEndElement(); // End of Resources
	}
	
	public static void writeCmdiOlacElement(String elementName, JsonObject meta, MetadataBuilder builder) throws MetadataBuilderException {
		writeCmdiOlacElement(elementName, meta, elementName, builder);
	}
	
	public static void writeCmdiOlacElement(String elementName, JsonObject meta, String tagName, MetadataBuilder builder) throws MetadataBuilderException {
		if (meta.containsKey(elementName)) {
			JsonArray elmArray = meta.getJsonArray(elementName);
			for (JsonObject elementObject : elmArray.getValuesAs(JsonObject.class)) {
				XmlDumpAttributes attrs = new XmlDumpAttributes();
				if (elementObject.containsKey("lang")) {
            		attrs.put("xml:lang", elementObject.getString("lang"));
            	}
				if (elementObject.containsKey("type")) {
					if (elementObject.getString("type").equals("olac:language")) {
						attrs.put("olac-language", elementObject.getString("code"));
					} else if (elementObject.getString("type").equals("olac:role")) {
						attrs.put("olac-role", elementObject.getString("code"));
					} else if (elementObject.getString("type").equals("olac:linguistic-field")) {
						attrs.put("olac-linguistic-field", elementObject.getString("code"));
					} else if (elementObject.getString("type").equals("olac:discourse-type")) {
						attrs.put("olac-discourse-type", elementObject.getString("code"));
					} else if (elementObject.getString("type").equals("olac:linguistic-type")) {
						attrs.put("olac-linguistic-type", elementObject.getString("code"));
					} else {
						attrs.put("dcterms-type", elementObject.getString("type"));
					}
            	}
				builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, elementName, attrs, elementObject.containsKey("value") ? XMLDocument.removeHTMLTag(elementObject.getString("value")) : null);
			}
		}
	}

	public static void writeCmdiOlacMultilingualElement(String tag, JsonObject multilingualObject, MetadataBuilder builder) throws MetadataBuilderException {
		XmlDumpAttributes attrs = new XmlDumpAttributes();
        attrs.put("xml:lang", multilingualObject.getString("lang"));
		builder.writeStartEndElement(Constant.CMDI_OLAC_NAMESPACE_PREFIX, tag, attrs, XMLDocument.removeHTMLTag(multilingualObject.getString("value")));
	}
	
	public List<String> getListHandlesRoot() {
		return listHandlesRoot;
	}

	public void setListHandlesRoot(List<String> listHandlesRoot) {
		this.listHandlesRoot = listHandlesRoot;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
