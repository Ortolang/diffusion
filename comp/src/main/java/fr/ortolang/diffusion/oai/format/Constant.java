package fr.ortolang.diffusion.oai.format;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonObject;
import javax.json.JsonString;

public class Constant {

	// OAI_DC metadata format
	public static final String OAI_DC_NAMESPACE_PREFIX = "oai_dc";
	public static final String OAI_DC_NAMESPACE_URI = "http://www.openarchives.org/OAI/2.0/oai_dc/";
	public static final String OAI_DC_NAMESPACE_SCHEMA_LOCATION = "http://www.openarchives.org/OAI/2.0/oai_dc.xsd";
	
	public static final String DC_NAMESPACE_PREFIX = "dc";
	public static final String DC_NAMESPACE_URI = "http://purl.org/dc/elements/1.1/";
	public static final String DC_NAMESPACE_SCHEMA_LOCATION = "http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd";
	
	public static final String XSI_NAMESPACE_PREFIX = "xsi";
	public static final String XSI_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String XSI_SCHEMA_LOCATION = "xsi:schemaLocation";
	
	public static final String OAI_DC_ELEMENT = "dc";
	
	public static final List<String> DC_ELEMENTS = Arrays.asList("contributor", "coverage", "creator", "date", "description",
			"format", "identifier", "language", "publisher", "relation", "rights", "source", "subject","title", "type");

	// OLAC metadata format
	public static final String OLAC_NAMESPACE_PREFIX = "olac";
	public static final String OLAC_NAMESPACE_URI = "http://www.language-archives.org/OLAC/1.1/";
	public static final String OLAC_NAMESPACE_SCHEMA_LOCATION = "http://www.language-archives.org/OLAC/1.1/olac.xsd";
	
	public static final String DCTERMS_NAMESPACE_PREFIX = "dcterms";
	public static final String DCTERMS_NAMESPACE_URI = "http://purl.org/dc/terms/";
	public static final String DCTERMS_NAMESPACE_SCHEMA_LOCATION = "http://dublincore.org/schemas/xmls/qdc/2006/01/06/dcterms.xsd";
	
	public static final String OLAC_ELEMENT = "olac";
	public static final List<String> DCTERMS_ELEMENTS = Arrays.asList("abstract", "accessRights", "accrualMethod", "accrualPeriodicity", "accrualPolicy",
    		"alternative", "audience", "available", "bibliographicCitation", "conformsTo", "created", "dateAccepted", "dateCopyrighted",
    		"dateSubmitted", "educationLevel", "extent", "hasFormat", "hasPart", "hasVersion", "instructionalMethod", "isFormatOf", 
    		"isPartOf", "isReferencedBy", "isReplacedBy", "isRequiredBy", "issued", "isVersionOf", "license", "mediator", "medium",
    		"modified", "provenance", "references", "replaces", "requires", "rightsHolder", "spatial", "tableOfContents", "temporal",
    		"valid");
	public static HashMap<List<String>, String> OLAC_TO_DC_ELEMENTS = new HashMap<List<String>, String>();
	static {
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("alternative"), "title");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("tableOfContents", "abstract"), "description");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("created", "valid", "available", "issued", "modified", "dateAccepted", "dateCopyrighted", "dateSubmitted"), "date");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("extent", "medium"), "format");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("bibliographicCitation"), "identifier");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("isVersionOf", "hasVersion", "isReplacedBy", "replaces", "isRequiredBy", "requires", "isPartOf", "hasPart", "isReferencedBy", "references", "isFormatOf", "hasFormat", "conformsTo"), "relation");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("spatial", "temporal"), "coverage");
		OLAC_TO_DC_ELEMENTS.put(Arrays.asList("accessRights", "license"), "rights");
	}
	
	public static final List<String> DATE_ELEMENTS = Arrays.asList("date", "issued", "dateCopyrighted", "created", "available", "dateAccepted", "dateSubmitted", "modified", "valid");
    
	public static final List<String> OLAC_ROLES = Arrays.asList("annotator", "author", "compiler", "consultant", "data_inputter", "depositor", "developer", "editor", "illustrator",
			"interpreter", "interviewer", "participant", "performer", "photographer", "recorder", "researcher", "research_participant", "responder", "signer", "singer", "speaker",
			"sponsor", "transcriber", "translator");
	
	// CMDI
	public static final String CMDI_NAMESPACE_PREFIX = "cmd";
	public static final String CMDI_NAMESPACE_URI = "http://www.clarin.eu/cmd/1";
	public static final String CMDI_NAMESPACE_SCHEMA_LOCATION = "https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd";

	// CMDI OLAC Profile
	public static final String CMDI_OLAC_NAMESPACE_PREFIX = "cmdOlac";
	public static final String CMDI_OLAC_NAMESPACE_URI = "http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1288172614026";
	public static final String CMDI_OLAC_NAMESPACE_SCHEMA_LOCATION = "https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1288172614026/xsd";

	public static final String CMDI_VERSION_ATTRIBUTE = "CMDVersion";
	public static final String CMDI_VERSION_VALUE_ATTRIBUTE = "1.2";
	
	public static final String CMDI_ELEMENT = "CMD";
	public static final String CMDI_HEADER_ELEMENT = "Header";
	public static final String CMDI_MDCREATOR_ELEMENT = "MdCreator";
	public static final String CMDI_MDCREATIONDATE_ELEMENT = "MdCreationDate";
	public static final String CMDI_MDSELFLINK_ELEMENT = "MdSelfLink";
	public static final String CMDI_MDPROFILE_ELEMENT = "MdProfile";
	public static final String CMDI_MDCOLLECTIONDISPLAYNAME_ELEMENT = "MdCollectionDisplayName";
	public static final String CMDI_RESOURCES_ELEMENT = "Resources";
	public static final String CMDI_RESOURCEPROXYLIST_ELEMENT = "ResourceProxyList";
	public static final String CMDI_RESOURCEPROXY_ELEMENT = "ResourceProxy";
	public static final String CMDI_RESOURCEPROXYID_ELEMENT = "id";
	public static final String CMDI_RESOURCETYPE_ELEMENT = "ResourceType";
	public static final String CMDI_RESOURCETYPE_LANDINGPAGE = "LandingPage";
	public static final String CMDI_RESOURCEREF_ELEMENT = "ResourceRef";
	public static final String CMDI_JOURNALFILEPROXYLIST_ELEMENT = "JournalFileProxyList";
	public static final String CMDI_RESOURCERELATIONLISTT_ELEMENT = "ResourceRelationList";
	public static final String CMDI_COMPONENTS_ELEMENT = "Components";
	public static final String CMDI_OLAC_ELEMENT = "OLAC-DcmiTerms";
	
	public static final String CMDI_MDCREATOR_VALUE = "ORTOLANG Repository";
	public static final String CMDI_OLAC_PROFILE_VALUE = "clarin.eu:cr1:p_1288172614026";
	
	// Utils
	public static final String w3cdtfFormat = "yyyy-MM-dd";
    public static final SimpleDateFormat w3cdtf = new SimpleDateFormat(w3cdtfFormat);
    public static final String iso639_3pattern = "[A-Za-z][A-Za-z][A-Za-z]?";
    public static final String iso639_2pattern = "[A-Za-z][A-Za-z]";
    
    private static Patterns patterns = new Patterns();
    
    static {
    	patterns.add(Pattern.compile("\\<!--.*-->", Pattern.DOTALL), "");
    	patterns.add(Pattern.compile("\\<.*?>", Pattern.DOTALL), "");
    	patterns.add(Pattern.compile("\\&nbsp;"), " ");
    	patterns.add(Pattern.compile("\\&"), "");
    }

    public static String removeHTMLTag(String str) {
    	for(Patterns.Entry pattern : patterns.getPatterns()) {
    		str = replace(str, pattern.getPattern(), pattern.getReplacement());
    	}
    	return str;
    }
    
    protected static String replace(String str, Pattern pattern, String replacement) {
    	Matcher m = pattern.matcher(str);
    	return m.replaceAll(replacement);
    }

	public static String person(JsonObject contributor) {
		JsonObject entityContributor = contributor.getJsonObject("entity");
		JsonString lastname = entityContributor.getJsonString("lastname");
		JsonString midname = entityContributor.getJsonString("midname");
		JsonString firstname = entityContributor.getJsonString("firstname");
		JsonString title = entityContributor.getJsonString("title");
		JsonString acronym = null;

		if (entityContributor.containsKey("organization")) {
			acronym = entityContributor.getJsonObject("organization").getJsonString("acronym");
		}
		return (lastname != null ? lastname.getString() : "") + (midname != null ? ", " + midname.getString() : "")
				+ (firstname != null ? ", " + firstname.getString() : "")
				+ (title != null ? " " + title.getString() : "") + (acronym != null ? ", " + acronym.getString() : "");
	}
}
