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

	public static final String XMLNS = "xmlns";
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
	
	public static final String SIP_NAMESPACE_PREFIX = "sip";
	public static final String SIP_NAMESPACE_URI = "http://www.cines.fr/pac/sip";
	public static final String SIP_NAMESPACE_SCHEMA_LOCATION = "http://www.cines.fr/pac/sip.xsd";

	public static final String OAI_DATACITE_NAMESPACE_PREFIX = "datacite";
	public static final String OAI_DATACITE_NAMESPACE_URI = "http://datacite.org/schema/kernel-4";
	public static final String OAI_DATACITE_NAMESPACE_SCHEMA_LOCATION = "https://schema.datacite.org/meta/kernel-4/metadata.xsd";
	
	public static final String OAI_OPENAIRE_SET_NAME = "OpenAIRE";
	public static final String OAI_OPENAIRE_SET_SPEC = "openaire_data";
	
	public static final String OAI_DATACITE_DOCUMENT = "resource";
	public static final String OAI_DATACITE_ALTERNATE_IDENTIFIERS_ELEMENT = "alternateIdentifiers";
	public static final String OAI_DATACITE_ALTERNATE_IDENTIFIER_ELEMENT = "alternateIdentifier";
	public static final String OAI_DATACITE_CREATORS_ELEMENT = "creators";
	public static final String OAI_DATACITE_CREATOR_ELEMENT = "creator";
	public static final String OAI_DATACITE_CREATOR_NAME_ELEMENT = "creatorName";
	public static final String OAI_DATACITE_NAME_TYPE_ATTRIBUTE = "nameType";
	public static final String OAI_DATACITE_NAME_TYPE_PERSONAL_VALUE = "Personal";
	public static final String OAI_DATACITE_GIVEN_NAME_ELEMENT = "givenName";
	public static final String OAI_DATACITE_FAMILY_NAME_ELEMENT = "familyName";
	public static final String OAI_DATACITE_AFFILIATION_ELEMENT = "affiliation";
	public static final String OAI_DATACITE_TITLES_ELEMENT = "titles";
	public static final String OAI_DATACITE_TITLE_ELEMENT = "title";
	public static final String OAI_DATACITE_PUBLICATIONYEAR_ELEMENT = "publicationYear";
	public static final String OAI_DATACITE_RESOURCE_TYPE_ELEMENT = "resourceType";
	public static final String OAI_DATACITE_RESOURCE_TYPE_ATTRIBUTE = "resourceTypeGeneral";
	public static final String OAI_DATACITE_RESOURCE_TYPE_DATASET_VALUE = "Dataset";
	public static final String OAI_DATACITE_RESOURCE_TYPE_SOFTWARE_VALUE = "Software";
	public static final String OAI_DATACITE_RESOURCE_TYPE_SERVICE_VALUE = "Service";
	public static final String OAI_DATACITE_RESOURCE_TYPE_OTHER_VALUE = "Other";
	public static final String OAI_DATACITE_DATES_ELEMENT = "dates";
	public static final String OAI_DATACITE_DATE_ELEMENT = "date";
	public static final String OAI_DATACITE_DATE_TYPE_ELEMENT = "dateType";
	public static final String OAI_DATACITE_DATE_TYPE_ISSUED_VALUE = "Issued";
	public static final String OAI_DATACITE_DESCRIPTIONS_ELEMENT = "descriptions";
	public static final String OAI_DATACITE_DESCRIPTION_ELEMENT = "description";
	public static final String OAI_DATACITE_DESCRIPTION_TYPE_ATTRIBUTE = "descriptionType";
	public static final String OAI_DATACITE_DESCRIPTION_TYPE_ABSTRACT_VALUE = "Abstract";
	public static final String OAI_DATACITE_RIGHTS_LIST_ELEMENT = "rightsList";
	public static final String OAI_DATACITE_RIGHTS_ELEMENT = "rights";
	public static final String OAI_DATACITE_RIGHTS_URI_ATTRIBUTE = "rightsURI";
	public static final String OAI_DATACITE_RIGHTS_URI_OPENACCESS_VALUE = "info:eu-repo/semantics/openAccess";
	public static final String OAI_DATACITE_RIGHTS_URI_RESTRICTED_VALUE = "info:eu-repo/semantics/restrictedAccess";
	public static final String OAI_DATACITE_SIZES_ELEMENT = "sizes";
	public static final String OAI_DATACITE_SIZE_ELEMENT = "size";
	public static final String OAI_DATACITE_SUBJECTS_ELEMENT = "subjects";
	public static final String OAI_DATACITE_SUBJECT_ELEMENT = "subject";
	public static final String OAI_DATACITE_CONTRIBUTORS_ELEMENT = "contributors";
	public static final String OAI_DATACITE_CONTRIBUTOR_ELEMENT = "contributor";
	public static final String OAI_DATACITE_CONTRIBUTOR_NAME_ELEMENT = "contributorName";
	public static final String OAI_DATACITE_CONTRIBUTOR_TYPE_ATTRIBUTE = "contributorType";
	public static final String OAI_DATACITE_EDITOR_VALUE = "Editor";
	public static final String OAI_DATACITE_OTHER_VALUE = "Other";
	public static final String OAI_DATACITE_SPONSOR_VALUE = "Sponsor";
	public static final String OAI_DATACITE_RESEARCHER_VALUE = "Researcher";
	
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
	
	public static final List<String> DCTERMS_TYPE = Arrays.asList("DCMIType", "DDC", "IMT", "LCC", "LCSH", "MESH", "NLM", "TGN", 
			"UDC", "Box", "ISO3166", "ISO639-2", "ISO639-3", "Period", "Point", "RFC1766", "RFC3066", "RFC4646", "RFC5646", 
			"URI", "W3CDTF");
	
	public static final List<String> OLAC_LINGUISTIC_FIELDS = Arrays.asList("anthropological_linguistics", "applied_linguistics", 
			"cognitive_science", "computational_linguistics", "discourse_analysis", "forensic_linguistics", "general_linguistics", 
			"historical_linguistics", "history_of_linguistics", "language_acquisition", "language_documentation", "lexicography", 
			"linguistics_and_literature", "linguistic_theories", "mathematical_linguistics", "morphology", "neurolinguistics", 
			"philosophy_of_language", "phonetics", "phonology", "pragmatics", "psycholinguistics", "semantics", "sociolinguistics", 
			"syntax", "text_and_corpus_linguistics", "translating_and_interpreting", "typology", "writing_systems");
	
	public static final List<String> OLAC_DISCOURSE_TYPES = Arrays.asList("dialogue", "drama", "formulaic", "ludic", "oratory", 
			"narrative", "procedural", "report", "singing", "unintelligible_speech");
	
	public static final List<String> OLAC_LINGUISTIC_TYPES = Arrays.asList("language_description", "lexicon", "primary_text");
	
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
	public static final String CMDI_REF_ATTRIBUT = "ref";
	public static final String CMDI_MIMETYPE_ATTRIBUT = "mimetype";
	public static final String CMDI_TEXT_VALUE = "#text";
	
	public static final String CMDI_MDCREATOR_VALUE = "ORTOLANG Repository";
	public static final String CMDI_OLAC_PROFILE_VALUE = "clarin.eu:cr1:p_1288172614026";
	
	public static final String CMDI_RESOURCE_CLASS_CORPUS = "corpus";
	public static final String CMDI_RESOURCE_CLASS_LEXICON_RESOURCE = "lexicalResource";
	public static final String CMDI_RESOURCE_CLASS_TERMINOLOGY = "terminology";
	public static final String CMDI_RESOURCE_CLASS_TOOL_SERVICE = "toolService";
	public static final String CMDI_RESOURCE_CLASS_WEBSITE = "website";
	
	public static final String ORTOLANG_RESOURCE_TYPE_CORPORA = "Corpus";	
	public static final String ORTOLANG_RESOURCE_TYPE_LEXICON = "Lexique";	
	public static final String ORTOLANG_RESOURCE_TYPE_TOOL = "Outil";	
	public static final String ORTOLANG_RESOURCE_TYPE_TERMINOLOGY = "Terminologie";	
	public static final String ORTOLANG_RESOURCE_TYPE_APPLICATION = "Application";	
	
	// Utils
	public static final String w3cdtfFormat = "yyyy-MM-dd";
    public static final SimpleDateFormat w3cdtf = new SimpleDateFormat(w3cdtfFormat);
    public static final String w3cdtfPattern = "(\\d{4})|(\\d{4}-\\d{2})|(\\d{4}-\\d{2}-\\d{2})|(\\d{4}-\\d{2}-\\d{2}T\\d{2}(:\\d{2}){1,2}[-+]\\d{2}:\\d{2})";
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
