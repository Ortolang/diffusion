package fr.ortolang.diffusion.api.search;

import fr.ortolang.diffusion.api.sru.fcs.OrtolangSearchHits;
import fr.ortolang.diffusion.content.ContentSearchService;

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

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.core.indexing.OrtolangItemIndexableContent;
import fr.ortolang.diffusion.core.indexing.UserMetadataIndexableContent;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
import fr.ortolang.diffusion.referential.indexing.SuggestReferentialEntityIndexableContent;
import fr.ortolang.diffusion.search.SearchQuery;
import fr.ortolang.diffusion.search.SearchService;

import org.jboss.resteasy.annotations.GZIP;

import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/search")
@Produces({ MediaType.APPLICATION_JSON })
public class SearchResource {

	@EJB
	private SearchService search;

	public SearchResource() {
	}

	@GET
	@Path("/suggest")
	public Response searchAll(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request);
		query.setIndex(SuggestReferentialEntityIndexableContent.INDEX);
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query))).build();
	}

	@GET
	@Path("/items")
	public Response searchItems(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request, OrtolangItemIndexableContent.INDEX);
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query))).build();
	}

	@GET
	@Path("/items/{id}")
	public Response getItem(@PathParam(value = "id") String id, @QueryParam(value = "type") String type,
			@QueryParam(value = "version") String version) {
		if (type == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'type' is mandatory").build();
		}
		if (id == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'id' is mandatory").build();
		}

		String index = OrtolangItemIndexableContent.INDEX;

		if (version != null) {
			index = OrtolangItemIndexableContent.INDEX_ALL;
			id = id + "-" + version;
		}
		// TOOD find an item if type is not specify
		return Response.ok(search.get(index, type, id)).build();
	}

	@GET
	@Path("/workspaces/{alias}")
	public Response getWorkspaces(@PathParam(value = "alias") String alias) {
		if (alias == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'alias' is mandatory").build();
		}
		String document = search.get(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, alias);
		return Response.ok(document).build();
	}

	@GET
	@Path("/entities")
	@GZIP
	public Response searchEntities(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request, ReferentialService.SERVICE_NAME);
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query))).build();
	}

	@GET
	@Path("/entities/{id}")
	@GZIP
	public Response getEntity(@PathParam(value = "id") String id, @QueryParam(value = "type") String type) {
		if (type == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'type' is mandatory").build();
		}
		if (id == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'id' is mandatory").build();
		}
		// TOOD find an entity if type is not specify
		String entity = search.get(ReferentialService.SERVICE_NAME, type, id);
		if (entity != null) {
			return Response.ok(entity).build();
		}
		return Response.status(404).build();
	}

	@GET
	@Path("/persons/{key}")
	@GZIP
	public Response getPerson(@PathParam(value = "key") String key) {
		if (key == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'key' is mandatory").build();
		}
		String person = search.get(ReferentialService.SERVICE_NAME, ReferentialEntityType.PERSON.name().toLowerCase(), key);
		if (person != null) {
			return Response.ok(person).build();
		}
		return Response.status(404).build();
	}

	@GET
	@Path("/persons")
	@GZIP
	public Response searchPersons(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request, ReferentialService.SERVICE_NAME, ReferentialEntityType.PERSON.name().toLowerCase());
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query))).build();
	}

	@GET
	@Path("/organizations/{key}")
	@GZIP
	public Response getOrganization(@PathParam(value = "key") String key) {
		if (key == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'key' is mandatory").build();
		}
		String person = search.get(ReferentialService.SERVICE_NAME, ReferentialEntityType.ORGANIZATION.name().toLowerCase(), key);
		if (person != null) {
			return Response.ok(person).build();
		}
		return Response.status(404).build();
	}

	@GET
	@Path("/organizations")
	@GZIP
	public Response searchOrganizations(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request, ReferentialService.SERVICE_NAME, ReferentialEntityType.ORGANIZATION.name().toLowerCase());
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query))).build();
	}

	@GET
	@Path("/roles/{key}")
	@GZIP
	public Response getRole(@PathParam(value = "key") String key) {
		if (key == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'key' is mandatory").build();
		}
		String role = search.get(ReferentialService.SERVICE_NAME, ReferentialEntityType.ROLE.name().toLowerCase(), key);
		if (role != null) {
			return Response.ok(role).build();
		}
		return Response.status(404).build();
	}

	@GET
	@Path("/roles")
	@GZIP
	public Response searchRoles(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request, ReferentialService.SERVICE_NAME, ReferentialEntityType.ROLE.name().toLowerCase());
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query))).build();
	}

	@GET
	@Path("/languages")
	@GZIP
	public Response searchLanguages(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request, ReferentialService.SERVICE_NAME, ReferentialEntityType.LANGUAGE.name().toLowerCase());
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query))).build();
	}

	@GET
	@Path("/profiles")
	@GZIP
	public Response searchProfiles(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request, MembershipService.SERVICE_NAME,
				Profile.OBJECT_TYPE);
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query))).build();
	}

	@GET
	@Path("/profiles/{key}")
	@GZIP
	public Response getProfile(@PathParam(value = "key") String key) {
		if (key == null) {
			return Response.status(Response.Status.BAD_REQUEST).entity("parameter 'key' is mandatory").build();
		}
		String profile = search.get(MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, key);
		if (profile != null) {
			return Response.ok(profile).build();
		}
		return Response.status(404).build();
	}

	@GET
	@Path("/metadata")
	@GZIP
	public Response searchUserMetadata(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request, UserMetadataIndexableContent.INDEX);
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query))).build();
	}

	@GET
	@Path("/content")
	@GZIP
	public Response searchContent(@Context HttpServletRequest request) {
		SearchQuery query = SearchResourceHelper.executeQuery(request, ContentSearchService.SERVICE_NAME);
		return Response.ok(SearchResultRepresentation.fromSearchResult(search.search(query), false)).build();
	}

}
