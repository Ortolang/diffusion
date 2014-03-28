package fr.ortolang.diffusion.security.authorisation.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

@Entity
public class AuthorisationPolicy {
	
	@Id
	private String id;
	private String owner;
	@ElementCollection(fetch=FetchType.EAGER)
	private Map<String, String> rules;
	
	public AuthorisationPolicy() {
		rules = new HashMap<String, String>();
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

	public String getPermissionsList(String subject) {
		if ( rules.containsKey(subject) ) {
			return rules.get(subject);
		} else {
			return "";
		}
	}

	public void setPermissionsList(String subject, String permissionsList) {
		rules.put(subject, permissionsList);
	}
	
	public List<String> getPermissions(String subject) {
		if ( rules.containsKey(subject) && !rules.get(subject).equals("") ) {
			return Arrays.asList(rules.get(subject).split(","));
		} else {
			return Collections.emptyList();
		}
	}
	
	public void setPermissions(String subject, List<String> permissions) {
		if ( permissions.size() > 0 ) {
			StringBuffer permissionsList = new StringBuffer();
			for ( String permission : permissions ) {
				permissionsList.append(permission);
				permissionsList.append(",");
			}
			rules.put(subject, permissionsList.substring(0, permissionsList.length()-1));
		} else {
			if ( rules.containsKey(subject) ) {
				rules.remove(subject);
			}
		}
	}

	public Map<String, List<String>> getRules() {
		HashMap<String, List<String>> map = new HashMap<String, List<String>>();
		for ( String subject : rules.keySet() ) {
			map.put(subject, getPermissions(subject));
		}
		return map;
	}
	
	public void setRules(Map<String, List<String>> newrules) {
		rules.clear();
		for ( String subject : newrules.keySet() ) {
			setPermissions(subject, newrules.get(subject));
		}
	}
	
	public boolean hasPermission(String subject, String permission) {
		if ( rules.containsKey(subject) && rules.get(subject).indexOf(permission) != -1 ) {
			return true;
		} 
		return false;
	}

}
