package fr.ortolang.diffusion;

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

import java.io.Serializable;
import java.util.StringTokenizer;

@SuppressWarnings("serial")
public class OrtolangObjectIdentifier implements Serializable {

	private String service;
	private String type;
	private String id;

	public OrtolangObjectIdentifier() {
	}

	public OrtolangObjectIdentifier(String service, String type, String id) {
		this.service = service;
		this.type = type;
		this.id = id;
	}
	
	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}
	
	public String getService() {
		return service;
	}

	public static OrtolangObjectIdentifier deserialize(String serializedIdentifier) {
		if (serializedIdentifier == null) {
			return null;
		}

		StringTokenizer tokenizer = new StringTokenizer(serializedIdentifier, "/");
		return new OrtolangObjectIdentifier(tokenizer.nextToken(), tokenizer.nextToken(), tokenizer.nextToken());
	}
	
	public static String buildFilterPattern(String service, String type) {
		StringBuilder pattern = new StringBuilder();
		pattern.append("/");
		if ( service != null && service.length() > 0 ) {
			pattern.append(service.toLowerCase());
		} else {
			pattern.append(".*");
		}
		pattern.append("/");
		if ( type != null && type.length() > 0 ) {
			pattern.append(type.toLowerCase());
		} else {
			pattern.append(".*");
		}
		pattern.append("/.*");
		return pattern.toString();
	}
	
	public static String buildJPQLFilterPattern(String service, String type) {
		StringBuilder pattern = new StringBuilder();
		pattern.append("/");
		if ( service != null && service.length() > 0 ) {
			pattern.append(service.toLowerCase());
			pattern.append("/");
		} 
		if ( type != null && type.length() > 0 ) {
			pattern.append(type.toLowerCase());
			pattern.append("/");
		}
		return pattern.toString();
	}

	public String serialize() {
		return "/" + this.getService() + "/" + this.getType() + "/" + this.getId();
	}

	@Override
	public String toString() {
		return "Service:" + getService() + "; Type:" + getType() + "; Id:" + getId();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((id == null) ? 0 : id.hashCode());
		result = (prime * result) + ((type == null) ? 0 : type.hashCode());
		result = (prime * result) + ((service == null) ? 0 : service.hashCode());

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		OrtolangObjectIdentifier other = (OrtolangObjectIdentifier) obj;

		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}

		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		
		if (service == null) {
			if (other.service != null) {
				return false;
			}
		} else if (!service.equals(other.service)) {
			return false;
		}

		return true;
	}
}
