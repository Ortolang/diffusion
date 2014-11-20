package fr.ortolang.diffusion.publication.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.membership.MembershipService;

public class ForAllPublicationType extends PublicationType {

	public static final String NAME = "forall";
	public static final String DESCRIPTION = "read & download permission for all users";
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public Map<String, List<String>> getSecurityRules() {
		Map<String, List<String>> rules = new HashMap<String, List<String>>();
		rules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList("read", "download"));
		return rules;
	}
	
}
