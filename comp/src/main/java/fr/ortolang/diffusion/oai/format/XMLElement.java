package fr.ortolang.diffusion.oai.format;

import java.util.ArrayList;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XMLElement {

	private String name;
	private String prefixNamespace;
	private Map<String, String> attributes;
	private String value;
	protected List<XMLElement> fields;

	public XMLElement() {
		fields = new ArrayList<XMLElement>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrefixNamespace() {
		return prefixNamespace;
	}

	public void setPrefixNamespace(String prefixNamespace) {
		this.prefixNamespace = prefixNamespace;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void addElement(XMLElement element) {
		this.fields.add(element);
	}
	
	public static XMLElement createElement(String namespace, String name) {
		XMLElement elem = new XMLElement();
		elem.setPrefixNamespace(namespace);
		elem.setName(name);
		return elem;
	}
	
	public static XMLElement createElement(String namespace, String name, String value) {
		XMLElement elem = new XMLElement();
		elem.setPrefixNamespace(namespace);
		elem.setName(name);
		elem.setValue(value);
		return elem;
	}
	
	public XMLElement withAttribute(String name, String value) {
		if(this.attributes==null) {
			this.attributes = new HashMap<String, String>();
		}
		this.attributes.put(name, value);
		return this;
	}
}
