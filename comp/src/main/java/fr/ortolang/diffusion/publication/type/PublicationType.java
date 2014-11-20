package fr.ortolang.diffusion.publication.type;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PublicationType {
	
	private static Map<String, PublicationType> types = new HashMap<String, PublicationType> ();
	
	static {
		ForAllPublicationType all = new ForAllPublicationType();
		types.put(all.getName(), all);
	}
	
	abstract public String getName();
	
	abstract public String getDescription();
	
	abstract public Map<String, List<String>> getSecurityRules();
	
	public static PublicationType getType(String name) {
		return types.get(name);
	}
	
	public static Collection<PublicationType> listTypes() {
		return types.values();
	}
	
}
