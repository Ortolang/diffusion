package fr.ortolang.diffusion.api.rest.group;

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

import fr.ortolang.diffusion.api.rest.profile.ProfileRepresentation;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @resourceDescription Operations on Groups
 */
@Path("/groups")
@Produces({ MediaType.APPLICATION_JSON })
public class GroupResource {

    private static final Logger LOGGER = Logger.getLogger(GroupResource.class.getName());

    @EJB
    private BrowserService browser;
    @EJB
    private MembershipService membership;
    @EJB
    private AuthorisationService authorisation;

    @GET
    @Path("/{key}")
    public Response getGroup(@PathParam(value = "key") String key) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /groups/" + key);
        Group group = membership.readGroup(key);
        GroupRepresentation representation = GroupRepresentation.fromGroup(group);
        for (String member : group.getMembers()) {
            representation.addMember(ProfileRepresentation.fromProfile(membership.readProfile(member)));
        }
        return Response.ok(representation).build();
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addMember(@PathParam(value = "key") String key, @FormParam(value = "member") String member) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "PUT /groups/" + key);
        Group group = membership.addMemberInGroup(key, member);
        GroupRepresentation representation = GroupRepresentation.fromGroup(group);
        return Response.ok(representation).build();
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addMemberRepresentation(@PathParam(value = "key") String key, ProfileRepresentation profileRepresentation) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "PUT /groups/" + key);
        Group group = membership.addMemberInGroup(key, profileRepresentation.getKey());
        GroupRepresentation representation = GroupRepresentation.fromGroup(group);
        return Response.ok(representation).build();
    }
}
