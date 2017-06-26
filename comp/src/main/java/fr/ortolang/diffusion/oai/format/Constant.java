package fr.ortolang.diffusion.oai.format;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	public static final List<String> DC_ELEMENTS = Arrays.asList("identifier", "title", "creator", "subject",
			"description", "publisher", "contributor", "date", "type", "format", "source", "language", "relation",
			"coverage", "rights");

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
    
	// Utils
    public static final SimpleDateFormat w3cdtf = new SimpleDateFormat("yyyy-MM-dd");
    
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
}
