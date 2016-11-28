package fr.ortolang.diffusion.api.oaipmh.repository;

import java.util.List;
import java.util.stream.Collectors;

import com.lyncode.xoai.dataprovider.handlers.results.ListSetsResult;
import com.lyncode.xoai.dataprovider.repository.SetRepository;
import com.lyncode.xoai.dataprovider.model.Set;

import fr.ortolang.diffusion.oai.OaiService;
import fr.ortolang.diffusion.oai.SetNotFoundException;

public class DiffusionSetRepository implements SetRepository {

	private OaiService oaiService;
	
	public DiffusionSetRepository(OaiService oaiService) {
		this.oaiService = oaiService;
	}

	@Override
	public boolean supportSets() {
		return true;
	}

	@Override
	public ListSetsResult retrieveSets(int offset, int length) {
		List<Set> sets = oaiService.listSets().stream().map(set -> Set.set(set.getSpec()).withName(set.getName())).collect(Collectors.toList());
		return new ListSetsResult(offset + length < sets.size(), sets.subList(offset, Math.min(offset + length, sets.size())));
	}

	@Override
	public boolean exists(String setSpec) {
		try {
			oaiService.findSet(setSpec);
			return true;
		} catch (SetNotFoundException e) {
		}
		return false;
	}

}
