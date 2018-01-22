package fr.ortolang.diffusion.api.content;

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

import java.util.ArrayList;
import java.util.List;

import fr.ortolang.diffusion.api.content.metadata.DcDocument;
import fr.ortolang.diffusion.core.entity.CollectionElement;

public class ContentRepresentation {
	
	private String context;
	private String base;
	private String alias;
	private String root;
	private String path;
	private String order;
	private boolean asc;
	private boolean linkbykey;
	private String parentPath;
	private List<CollectionElement> elements;
	private DcDocument dcDocument;

	public ContentRepresentation() {
		context = "";
		base = "";
		alias = "";
		root = "";
		path = "";
		order = "N";
		linkbykey = false;
		asc = true;
		parentPath = "";
		elements = new ArrayList<CollectionElement> ();
		dcDocument = new DcDocument();
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getPath() {
		return path;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getRoot() {
		return root;
	}

	public void setRoot(String root) {
		this.root = root;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public boolean isAsc() {
		return asc;
	}

	public void setAsc(boolean asc) {
		this.asc = asc;
	}
	
	public boolean isLinkbykey() {
		return linkbykey;
	}

	public void setLinkbykey(boolean linkbykey) {
		this.linkbykey = linkbykey;
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}

	public List<CollectionElement> getElements() {
		return elements;
	}

	public void setElements(List<CollectionElement> elements) {
		this.elements = elements;
	}

	public DcDocument getDcDocument() {
		return dcDocument;
	}

	public void setDcDocument(DcDocument dcDocument) {
		this.dcDocument = dcDocument;
	}
}
