package fr.ortolang.diffusion.oai.format;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Patterns {
	private List<Patterns.Entry> patterns;
	
	public Patterns() {
		patterns = new ArrayList<Patterns.Entry>();
	}

	public List<Patterns.Entry> getPatterns() {
		return patterns;
	}
	
	public void add(Pattern pattern, String replacement) {
		patterns.add(new Patterns.Entry(pattern, replacement));
	}
	
	public void setPatterns(List<Patterns.Entry> patterns) {
		this.patterns = patterns;
	}

	public class Entry {
		private Pattern pattern;
		private String replacement;
		
		public Entry(Pattern pattern, String replacement) {
			this.pattern = pattern;
			this.replacement = replacement;
		}

		public Pattern getPattern() {
			return pattern;
		}

		public void setPattern(Pattern pattern) {
			this.pattern = pattern;
		}

		public String getReplacement() {
			return replacement;
		}

		public void setReplacement(String replacement) {
			this.replacement = replacement;
		}
	}
}
