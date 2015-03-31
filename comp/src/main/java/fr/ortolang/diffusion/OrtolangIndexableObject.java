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

import java.util.List;

public class OrtolangIndexableObject<T> {

	private OrtolangObjectIdentifier identifier;
	private String service;
	private String type;
	private String key;
	private String name;
	private boolean locked;
	private boolean hidden;
	private String status;
	private String author;
	private long creationDate;
	private long lastModificationDate;
	private List<OrtolangObjectProperty> properties;
	private T content;
	
	public OrtolangIndexableObject() {
	}

	public void setIdentifier(OrtolangObjectIdentifier identifier) {
		this.identifier = identifier;
	}

	public void setService(String service) {
		this.service = service;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setContent(T content) {
		this.content = content;
	}

	public T getContent() {
		return content;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setProperties(List<OrtolangObjectProperty> properties) {
		this.properties = properties;
	}

	public OrtolangObjectIdentifier getIdentifier() {
		return identifier;
	}

	public String getService() {
		return service;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getKey() {
		return key;
	}

	public boolean isHidden() {
		return hidden;
	}

	public boolean isLocked() {
		return locked;
	}

	public String getStatus() {
		return status;
	}

	public List<OrtolangObjectProperty> getProperties() {
		return properties;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getLastModificationDate() {
		return lastModificationDate;
	}

	public void setLastModificationDate(long lastModificationDate) {
		this.lastModificationDate = lastModificationDate;
	}

}
