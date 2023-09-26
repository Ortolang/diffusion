package fr.ortolang.diffusion.api.oai.metadata.cmdi;

public enum CmdiDocumentConstants {
    
    MDSELFLINK_PATH("//cmd:MdSelfLink"),
    COMPONENTS_PATH("//cmd:Components");

    private final String key;

    CmdiDocumentConstants(String name) {
        this.key = name;
    }

    public String key() {
        return key;
    }
}
