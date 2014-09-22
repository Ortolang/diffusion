package fr.ortolang.diffusion.api.oaipmh.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.lyncode.xoai.dataprovider.model.ItemIdentifier;
import com.lyncode.xoai.dataprovider.model.Set;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

public class DiffusionItemIdentifier implements ItemIdentifier {

	public static DiffusionItemIdentifier item () {
		return new DiffusionItemIdentifier();
	}

    public static DiffusionItemIdentifier randomItem() {
        return new DiffusionItemIdentifier()
                .withIdentifier(randomAlphabetic(10))
                .withDatestamp(new Date());
    }

	private Date datestamp;
	private String identifier;
	
	public DiffusionItemIdentifier withIdentifier(String id) {
		this.identifier = id;
		return this;
	}
	
	public DiffusionItemIdentifier withDatestamp(Date date) {
		this.datestamp = date;
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
		// No deleted status
		return false;
	}

}
