package fr.ortolang.diffusion.publication;

import java.util.List;
import java.util.Map;

public abstract class PublicationType {
	
	abstract public String getName();
	
	abstract public Map<String, List<String>> getSecurityRules();
	
}
