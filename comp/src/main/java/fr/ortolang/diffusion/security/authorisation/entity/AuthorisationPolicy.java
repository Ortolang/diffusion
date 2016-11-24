package fr.ortolang.diffusion.security.authorisation.entity;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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
		if (rules.containsKey(subject) && !rules.getProperty(subject).isEmpty()) {
			return Arrays.asList(rules.getProperty(subject).split(","));
		} else {
			return Collections.emptyList();
		}
	}

	public void setPermissions(String subject, List<String> permissions) throws IOException {
		loadRules();
		if (permissions.isEmpty()) {
			if (rules.containsKey(subject)) {
				rules.remove(subject);
			}
		} else {
			StringBuilder permissionsList = new StringBuilder();
			for (String permission : permissions) {
				permissionsList.append(permission);
				permissionsList.append(",");
			}
			rules.setProperty(subject, permissionsList.substring(0, permissionsList.length() - 1));
		}
		saveRules();
	}

	public boolean hasPermission(String subject, String permission) throws IOException {
		loadRules();
		return rules.containsKey(subject) && rules.getProperty(subject).contains(permission);
	}

	public Map<String, List<String>> getRules() throws IOException {
		loadRules();
		Map<String, List<String>> map = new HashMap<String, List<String>>();
		for (Entry<Object, Object> rule : rules.entrySet()) {
			String subject = (String) rule.getKey();
			if (rules.containsKey(subject) && !rules.getProperty(subject).isEmpty()) {
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
			if (!rule.getValue().isEmpty()) {
				StringBuilder permissionsList = new StringBuilder();
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((rules == null) ? 0 : rules.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AuthorisationPolicy other = (AuthorisationPolicy) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        if (rules == null) {
            if (other.rules != null)
                return false;
        } else if (!rules.equals(other.rules))
            return false;
        return true;
    }
	
}
