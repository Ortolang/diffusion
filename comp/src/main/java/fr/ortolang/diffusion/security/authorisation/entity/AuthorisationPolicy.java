package fr.ortolang.diffusion.security.authorisation.entity;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

@Entity
@Cacheable(true)
public class AuthorisationPolicy {

	@Id
	private String id;
	private String owner;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String rulesContent = "";
	@Transient
	private Properties rules;

	public AuthorisationPolicy() {
		rules = null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public boolean isOwner(String subject) {
		return owner.equals(subject);
	}

	public boolean isOwner(List<String> subjects) {
		return subjects.contains(owner);
	}

	public void setRulesContent(String rulesContent) {
		this.rulesContent = rulesContent;
	}

	public String getRulesContent() {
		return rulesContent;
	}

	public String getPermissionsList(String subject) throws IOException {
		loadRules();
		if (rules.containsKey(subject)) {
			return rules.getProperty(subject);
		} else {
			return "";
		}
	}

	public void setPermissionsList(String subject, String permissionsList) throws IOException {
		loadRules();
		rules.setProperty(subject, permissionsList);
		saveRules();
	}

	public List<String> getPermissions(String subject) throws IOException {
		loadRules();
		if (rules.containsKey(subject) && !rules.getProperty(subject).equals("")) {
			return Arrays.asList(rules.getProperty(subject).split(","));
		} else {
			return Collections.emptyList();
		}
	}

	public void setPermissions(String subject, List<String> permissions) throws IOException {
		loadRules();
		if (permissions.size() > 0) {
			StringBuffer permissionsList = new StringBuffer();
			for (String permission : permissions) {
				permissionsList.append(permission);
				permissionsList.append(",");
			}
			rules.setProperty(subject, permissionsList.substring(0, permissionsList.length() - 1));
		} else {
			if (rules.containsKey(subject)) {
				rules.remove(subject);
			}
		}
		saveRules();
	}

	public boolean hasPermission(String subject, String permission) throws IOException {
		loadRules();
		if (rules.containsKey(subject) && rules.getProperty(subject).indexOf(permission) != -1) {
			return true;
		}
		return false;
	}

	public Map<String, List<String>> getRules() throws IOException {
		loadRules();
		HashMap<String, List<String>> map = new HashMap<String, List<String>>();
		for (Entry<Object, Object> rule : rules.entrySet()) {
			String subject = (String) rule.getKey();
			if (rules.containsKey(subject) && !rules.getProperty(subject).equals("")) {
				map.put(subject, Arrays.asList(((String) rule.getValue()).split(",")));
			} else {
				map.put(subject, new ArrayList<String> ());
			}
		}
		return map;
	}

	public void setRules(Map<String, List<String>> newrules) throws IOException {
		loadRules();
		rules.clear();
		for (Entry<String, List<String>> rule: newrules.entrySet()) {
			if (rule.getValue().size() > 0) {
				StringBuffer permissionsList = new StringBuffer();
				for (String permission : rule.getValue()) {
					permissionsList.append(permission);
					permissionsList.append(",");
				}
				rules.setProperty(rule.getKey(), permissionsList.substring(0, permissionsList.length() - 1));
			} 
		}
		saveRules();
	}

	private void loadRules() throws IOException {
		if (rules == null) {
			rules = new Properties();
			if (rulesContent.length() > 0) {
				rules.load(new StringReader(rulesContent));
			}
		}
	}

	private void saveRules() throws IOException {
		if (rules != null && !rules.isEmpty()) {
			StringWriter output = new StringWriter();
			rules.store(output, null);
			rulesContent = output.toString();
		}
	}

}
