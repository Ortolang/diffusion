package fr.ortolang.diffusion.api.oaipmh.repository;

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.model.oaipmh.About;
import com.lyncode.xoai.model.oaipmh.Metadata;

import fr.ortolang.diffusion.api.oaipmh.model.DiffusionMetadata;
import fr.ortolang.diffusion.oai.entity.Record;

public class DiffusionItem implements Item {

	public static DiffusionItem item () {
		return new DiffusionItem();
	}

	private Date datestamp;
	private String identifier;
	private Metadata metadata;
	private List<Set> sets;
	private boolean deleted;

	public DiffusionItem() {
		this.identifier = null;
		this.datestamp = null;
		this.metadata = null;
		this.sets = new ArrayList<Set>();
		this.deleted = false;
	}
	
	public DiffusionItem withIdentifier(String id) {
		this.identifier = id;
		return this;
	}
	
	public DiffusionItem withDatestamp(Date date) {
		this.datestamp = date;
		return this;
	}

	public DiffusionItem withMetadata(InputStream input) throws IOException {
		this.metadata = new DiffusionMetadata(input);
		return this;
	}
	
	public DiffusionItem withSets(List<Set> sets) throws IOException {
		this.sets = sets;
		return this;
	}
	
	@Override
	public Date getDatestamp() {
		return this.datestamp;
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public List<Set> getSets() {
		return sets;
	}

	@Override
	public boolean isDeleted() {
		return deleted;
	}

	@Override
	public List<About> getAbout() {
		return new ArrayList<About>();
	}

	@Override
	public Metadata getMetadata() {
		return this.metadata;
	}

	public static DiffusionItem fromRecord(Record rec) throws IOException {
	    return DiffusionItem.item().withIdentifier(DiffusionItemRepository.PREFIX_IDENTIFIER + rec.getIdentifier())
	            .withDatestamp(new Date(rec.getLastModificationDate()))
	            .withMetadata(new ByteArrayInputStream(rec.getXml().getBytes(StandardCharsets.UTF_8)))
	            .withSets(rec.getSets().stream().map(spec -> Set.set(spec)).collect(Collectors.toList()));
	}
}
