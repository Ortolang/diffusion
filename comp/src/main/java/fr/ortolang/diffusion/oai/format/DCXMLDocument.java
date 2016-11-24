package fr.ortolang.diffusion.oai.format;

public abstract class DCXMLDocument extends XMLDocument {

    public void addDcField(String name, String value) {
        fields.add(XMLElement.createDcElement(name, value));
    }

    public void addDcMultilingualField(String name, String lang, String value) {
        fields.add(XMLElement.createDcElement(name, value).withAttribute("xml:lang", lang));
    }

}
