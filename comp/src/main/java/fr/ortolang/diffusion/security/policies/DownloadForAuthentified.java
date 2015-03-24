package fr.ortolang.diffusion.security.policies;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.ortolang.diffusion.membership.MembershipService;

public class DownloadForAuthentified extends PredefinedSecurityPolicy {

	public static final String NAME = "authentified";
	public static final String DESCRIPTION = "read permission for all user but download restricted to authentified users";
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public Map<String, List<String>> getRules(Map<String, String> params) {
		Map<String, List<String>> rules = new HashMap<String, List<String>>();
		rules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList("read"));
		rules.put(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY, Arrays.asList("read", "download"));
		return rules;
	}
	
}
