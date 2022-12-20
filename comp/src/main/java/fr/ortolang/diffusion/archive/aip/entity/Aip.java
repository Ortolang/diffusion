package fr.ortolang.diffusion.archive.aip.entity;

import java.io.StringReader;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.stax2.XMLInputFactory2;

public class Aip {

    private DocDC docDc;
    private DocMeta docMeta;

    public DocDC getDocDc() {
        return docDc;
    }

    public void setDocDc(DocDC docDc) {
        this.docDc = docDc;
    }

    public DocMeta getDocMeta() {
        return docMeta;
    }

    public void setDocMeta(DocMeta docMeta) {
        this.docMeta = docMeta;
    }

    public static Aip fromXML(String aipXml) throws XMLStreamException, JAXBException {
        Aip aip = new Aip();
        // Extracts informations form aip.xml
        XMLInputFactory xmlInputFactory = XMLInputFactory2.newInstance();
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(new StringReader(aipXml));

        while (reader.hasNext() && (!reader.isStartElement() || !reader.getLocalName().equals("DocDC"))) {
            reader.next();
        }

        if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("DocDC")) {
            DocDC docDc = DocDC.fromXMLStreamReader(reader);
            aip.setDocDc(docDc);
            reader.next();
        }

        if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("DocMeta")) {
            DocMeta docMeta = DocMeta.fromXMLStreamReader(reader);
            aip.setDocMeta(docMeta);
            reader.next();
        }

        if (reader.getEventType() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("FichMeta")) {
            // idFichier, nomFichier, encodage, formatFichier, versionFormatFichier, empreinte, empreinteOri, idDocument, migration, tailleEnOctets
            while(reader.hasNext() && (!reader.isEndElement() || reader.getLocalName().equals("FichMeta"))) {
                reader.next();
                if (reader.getEventType() ==  XMLStreamConstants.START_ELEMENT) {
                    switch( reader.getLocalName() ) {
                        case "idFichier":
                            
                            break;
    
                    }
                }
            }
            reader.next();
        }

        reader.close(); //TODO add to a finally
		
        return aip;
    }

}
