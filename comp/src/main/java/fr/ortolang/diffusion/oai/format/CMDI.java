package fr.ortolang.diffusion.oai.format;

public class CMDI extends XMLDocument {

	public static final String CMDI_NAMESPACE = "cmd";
	
	public static final String HEADER_FIELD = "Header";
	public static final String MDCREATOR_FIELD = "MdCreator";
	public static final String MDCREATIONDATE_FIELD = "MdCreationDate";
	public static final String MDSELFLINK_FIELD = "MdSelfLink";
	public static final String MDPROFILE_FIELD = "MdProfile";
	public static final String MDCOLLECTIONDISPLAYNAME_FIELD = "MdCollectionDisplayName";
	public static final String RESOURCEPROXY_FIELD = "ResourceProxy";
	
	public CMDI() {
		super();
		header = "<cmd:CMD xmlns:cmd=\"http://www.clarin.eu/cmd/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:cmdp=\"http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1288172614026\" CMDVersion=\"1.2\" xsi:schemaLocation=\"http://www.clarin.eu/cmd/1 https://infra.clarin.eu/CMDI/1.x/xsd/cmd-envelop.xsd http://www.clarin.eu/cmd/1/profiles/clarin.eu:cr1:p_1288172614026 https://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/1.x/profiles/clarin.eu:cr1:p_1288172614026/xsd\">";
        footer = "</cmd:CMD>";
	}

    public void addCmdiField(String name, String value) {
    	fields.add(XMLElement.createElement(CMDI_NAMESPACE, name, value));
    }
    
    public void setHeader(String creator, String creationDate, String selfLink, String profile, String collectionDisplayName) {
    	XMLElement header = XMLElement.createElement(CMDI_NAMESPACE, HEADER_FIELD);
    	header.addElement(XMLElement.createElement(CMDI_NAMESPACE, MDCREATOR_FIELD, creator));
    	header.addElement(XMLElement.createElement(CMDI_NAMESPACE, MDCREATIONDATE_FIELD, creationDate));
    	header.addElement(XMLElement.createElement(CMDI_NAMESPACE, MDSELFLINK_FIELD, selfLink));
    	header.addElement(XMLElement.createElement(CMDI_NAMESPACE, MDPROFILE_FIELD, profile));
    	header.addElement(XMLElement.createElement(CMDI_NAMESPACE, MDCOLLECTIONDISPLAYNAME_FIELD, collectionDisplayName));
    	fields.add(header);
    }

    public void addCmdiResourceProxy(String id) {
    	fields.add(XMLElement.createElement(CMDI_NAMESPACE, RESOURCEPROXY_FIELD).withAttribute("id", id));
    }
}
