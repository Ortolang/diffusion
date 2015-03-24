package fr.ortolang.diffusion.security.policies;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.membership.MembershipService;

public abstract class PredefinedSecurityPolicy {
	
	private static Map<String, List<String>> defaultRules = new HashMap<String, List<String>>();
	static {
		defaultRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList("read", "download"));
	}
	
	public static Map<String, List<String>> getDefaultRules() {
		return defaultRules;
	}
	
	abstract public String getName();

	abstract public String getDescription();
	
	abstract public Map<String, List<String>> getRules(Map<String, String> params);
	
}
