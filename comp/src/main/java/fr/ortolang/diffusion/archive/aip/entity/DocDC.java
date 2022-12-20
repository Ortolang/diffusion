package fr.ortolang.diffusion.archive.aip.entity;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamReader;

@XmlRootElement(name = "DocDC")
public class DocDC {
    private String identifier;

    @XmlElement(name="identifier")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{identifier: ").append(identifier)
            .append("}");
        return builder.toString();
    }

    public static DocDC fromXML(String xml) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(DocDC.class);
        Unmarshaller um = ctx.createUnmarshaller();
        return (DocDC) um.unmarshal(new StringReader(xml));
    }

    public static DocDC fromXMLStreamReader(XMLStreamReader reader) throws JAXBException {
        JAXBContext ctx = JAXBContext.newInstance(DocDC.class);
        Unmarshaller um = ctx.createUnmarshaller();
        return (DocDC) um.unmarshal(reader);
    }
}
