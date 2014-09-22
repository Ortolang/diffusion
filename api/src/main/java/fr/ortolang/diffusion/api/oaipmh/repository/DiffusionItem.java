package fr.ortolang.diffusion.api.oaipmh.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.model.Set;
import com.lyncode.xoai.model.oaipmh.About;
import com.lyncode.xoai.model.oaipmh.Metadata;

import fr.ortolang.diffusion.api.oaipmh.model.DiffusionMetadata;

public class DiffusionItem implements Item {

	public static DiffusionItem item () {
		return new DiffusionItem();
	}

	private Date datestamp;
	private String identifier;
	private Metadata metadata;

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
		// No Set
		return new ArrayList<Set>();
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public List<About> getAbout() {
		return new ArrayList<About>();
	}

	@Override
	public Metadata getMetadata() {
		return this.metadata;
	}


}
