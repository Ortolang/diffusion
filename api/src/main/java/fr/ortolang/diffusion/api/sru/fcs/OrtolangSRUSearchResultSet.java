package fr.ortolang.diffusion.api.sru.fcs;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import eu.clarin.sru.server.SRUDiagnosticList;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.SRUSearchResultSet;
import eu.clarin.sru.server.fcs.XMLStreamWriterHelper;

public class OrtolangSRUSearchResultSet extends SRUSearchResultSet {

    private static final String HITS_MIME_TYPE = "application/x-clarin-fcs-hits+xml";
    private static final String FCS_HITS_PREFIX = "hits";
    private static final String FCS_HITS_NS = "http://clarin.eu/fcs/dataview/hits";
    
	private int indexResult;
	private OrtolangSearchHits hits;

	protected OrtolangSRUSearchResultSet(SRUDiagnosticList diagnostics, OrtolangSearchHits hits, int position) {
		super(diagnostics);
		this.hits = hits;
		this.indexResult = position;
	}

	@Override
	public int getTotalRecordCount() {
		return (int) hits.getHits().size();
	}

	@Override
	public int getRecordCount() {
		return hits.getHits().size();
	}

	@Override
	public String getRecordIdentifier() {
//		OrtolangSearchHit hit = hits.getHit(indexResult);
//		if (hit != null) {
//			return hit.getId();
//		}
		return null;
	}

	@Override
	public String getRecordSchemaIdentifier() {
		return OrtolangEndpoint.CLARIN_FCS_RECORD_SCHEMA;
	}

	@Override
	public boolean nextRecord() throws SRUException {
		return  (++indexResult < getRecordCount() ? true : false);
	}

	@Override
	public void writeRecord(XMLStreamWriter writer) throws XMLStreamException {
		OrtolangSearchHit hit = hits.getHit(indexResult);
		String pid = hit.getPid();
		
		XMLStreamWriterHelper.writeStartResource(writer, pid, pid);
		XMLStreamWriterHelper.writeStartResourceFragment(writer, null, null);
		
		writeHitsDataView(writer, hit);
		
		XMLStreamWriterHelper.writeEndResourceFragment(writer);
		XMLStreamWriterHelper.writeEndResource(writer);
	}
	
	public void writeHitsDataView(XMLStreamWriter writer, OrtolangSearchHit hit) throws XMLStreamException {
		XMLStreamWriterHelper.writeStartDataView(writer, HITS_MIME_TYPE);
		writer.setPrefix(FCS_HITS_PREFIX, FCS_HITS_NS);
        writer.writeStartElement(FCS_HITS_NS, "Result");
        writer.writeNamespace(FCS_HITS_PREFIX, FCS_HITS_NS);
        
        String textStr = hit.getFragment();
		int iPreTag = textStr.indexOf(OrtolangEndpoint.HIGHLIGHT_PRETAG);
		if (iPreTag < 0) {
			writer.writeCharacters(textStr);
		} else {
			writer.writeCharacters(textStr.substring(0, iPreTag).replaceAll("\n"," "));
			int iPostTag = textStr.indexOf(OrtolangEndpoint.HIGHLIGHT_POSTTAG);
			writer.writeStartElement(FCS_HITS_NS, "Hit");
			writer.writeCharacters(textStr.substring(iPreTag + OrtolangEndpoint.HIGHLIGHT_PRETAG.length(), iPostTag));
			writer.writeEndElement();
			writer.writeCharacters(textStr.substring(iPostTag + OrtolangEndpoint.HIGHLIGHT_POSTTAG.length()).replaceAll("\n"," ") + "\n");
		}
        
        writer.writeEndElement(); // "Result" element
        XMLStreamWriterHelper.writeEndDataView(writer);
	}

}
