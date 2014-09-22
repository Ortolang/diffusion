package fr.ortolang.diffusion.publication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.membership.MembershipService;

public class ForAllPublicationType extends PublicationType {

	private static final String NAME = "Read & download for all";
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Map<String, List<String>> getSecurityRules() {
		Map<String, List<String>> rules = new HashMap<String, List<String>>();
		rules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList("read", "download"));
		return rules;
	}
	
}
