package fr.ortolang.diffusion.xml;

import java.util.LinkedHashMap;

public class XmlDumpNamespaces extends LinkedHashMap<String, XmlDumpNamespace> {

    private static final long serialVersionUID = 1L;

    @Override
    public synchronized XmlDumpNamespace put(String key, XmlDumpNamespace value) {
        if (value == null) {
            return null;
        }
        return super.put(key, value);
    }

}
