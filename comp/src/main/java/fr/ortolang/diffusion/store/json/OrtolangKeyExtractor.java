package fr.ortolang.diffusion.store.json;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrtolangKeyExtractor {

	public static final Pattern ORTOLANG_KEY_MATCHER = Pattern.compile("\\$\\{([\\w\\d:\\-_]*)\\}");

	public static String getMarker(String ortolangKey) {
		return "${"+ortolangKey+"}";
	}
	
	public static List<String> extractOrtolangKeys(String json) {
		Matcher okMatcher = OrtolangKeyExtractor.ORTOLANG_KEY_MATCHER.matcher(json);
		List<String> ortolangKeys = new ArrayList<String>();
		while(okMatcher.find()) {
			ortolangKeys.add(okMatcher.group(1));
		}
		return ortolangKeys;
	}
}
