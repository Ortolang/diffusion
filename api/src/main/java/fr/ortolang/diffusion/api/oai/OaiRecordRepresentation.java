package fr.ortolang.diffusion.api.oai;

import fr.ortolang.diffusion.api.oai.metadata.OaiXmlDocument;

public class OaiRecordRepresentation {
    
	private String context;
    private RecordRepresentation recordRepresentation;
    private OaiXmlDocument xmlDocument;

    public OaiRecordRepresentation(String context, RecordRepresentation recordRepresentation, OaiXmlDocument cmdiDocument) {
        this.context = context;
        this.recordRepresentation = recordRepresentation;
        this.xmlDocument = cmdiDocument;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public RecordRepresentation getRecordRepresentation() {
        return recordRepresentation;
    }

    public void setRecordRepresentation(RecordRepresentation recordRepresentation) {
        this.recordRepresentation = recordRepresentation;
    }

    public OaiXmlDocument getXmlDocument() {
        return xmlDocument;
    }

    public void setXmlDocument(OaiXmlDocument cmdiDocument) {
        this.xmlDocument = cmdiDocument;
    }
}
