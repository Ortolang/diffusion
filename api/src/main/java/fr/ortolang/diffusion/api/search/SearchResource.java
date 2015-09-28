package fr.ortolang.diffusion.api.search;

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

import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.search.SearchService;
import fr.ortolang.diffusion.search.SearchServiceException;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/search")
@Produces({ MediaType.APPLICATION_JSON })
public class SearchResource {

	private static final Logger LOGGER = Logger.getLogger(SearchResource.class.getName());

	@EJB
	private SearchService search;

	public SearchResource() {
	}

	@GET
	@Path("/index")
	public Response plainTextSearch(@QueryParam(value = "query") String query) throws SearchServiceException {
		LOGGER.log(Level.INFO, "GET /search/index?query=" + query);
		List<OrtolangSearchResult> results;
		if (query != null && query.length() > 0) {
			results = search.indexSearch(query);
		} else {
			results = Collections.emptyList();
		}
		return Response.ok(results).build();
	}

	@GET
	@Path("/json")
	public Response jsonSearch(@QueryParam(value = "query") String query) {
		LOGGER.log(Level.INFO, "GET /search/json?query=" + query);
		List<String> results;
		if (query != null && query.length() > 0) {
			try {
				results = search.jsonSearch(query);
			} catch (SearchServiceException e) {
				results = Collections.emptyList();
			}
		} else {
			results = Collections.emptyList();
		}
		return Response.ok(results).build();
	}

}
