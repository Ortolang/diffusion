package fr.ortolang.diffusion.dump;

import java.util.LinkedHashMap;

public class XmlDumpAttributes extends LinkedHashMap<String, String> {

    private static final long serialVersionUID = 1L;

    @Override
    public synchronized String put(String key, String value) {
        if (value == null) {
            value = new String();
        }
        return super.put(key, value);
    }

}
