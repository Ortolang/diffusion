package fr.ortolang.diffusion.api.group;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 * *
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

import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.annotations.GZIP;

import fr.ortolang.diffusion.api.profile.ProfileCardRepresentation;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

/**
 * @resourceDescription Operations on Groups
 */
@Path("/groups")
@Produces({ MediaType.APPLICATION_JSON })
public class GroupResource {

    private static final Logger LOGGER = Logger.getLogger(GroupResource.class.getName());

    @EJB
    private MembershipService membership;
    @Context
    private UriInfo uriInfo;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createGroup(@FormParam("name") String name, @FormParam("description") String description, @QueryParam("members") @DefaultValue("true") boolean members) throws MembershipServiceException, AccessDeniedException, KeyAlreadyExistsException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "POST FORM /groups");
        String key = UUID.randomUUID().toString();
        membership.createGroup(key, name, description);
        Group ngroup = membership.readGroup(key);
        GroupRepresentation representation = GroupRepresentation.fromGroup(ngroup);
        if ( members ) {
            for (String member : ngroup.getMembers()) {
                representation.addMember(ProfileCardRepresentation.fromProfile(membership.readProfile(member)));
            }
        }
        URI location = uriInfo.getBaseUriBuilder().path(this.getClass()).path(key).build();
        return Response.created(location).entity(representation).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createGroup(GroupRepresentation group) throws MembershipServiceException, AccessDeniedException, KeyAlreadyExistsException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "POST JSON /groups");
        String key = UUID.randomUUID().toString();
        membership.createGroup(key, group.getName(), group.getDescription());
        Group ngroup = membership.readGroup(key);
        GroupRepresentation representation = GroupRepresentation.fromGroup(ngroup);
        for (String member : ngroup.getMembers()) {
            representation.addMember(ProfileCardRepresentation.fromProfile(membership.readProfile(member)));
        }
        URI location = uriInfo.getBaseUriBuilder().path(this.getClass()).path(key).build();
        return Response.created(location).entity(representation).build();
    }

    @GET
    @Path("/{key}")
    @GZIP
    public Response getGroup(@PathParam(value = "key") String key, @QueryParam("members") @DefaultValue("true") boolean members) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /groups/" + key);
        Group group = membership.readGroup(key);
        GroupRepresentation representation = GroupRepresentation.fromGroup(group);
        if ( members ) {
            for (String member : group.getMembers()) {
                representation.addMember(ProfileCardRepresentation.fromProfile(membership.readProfile(member)));
            }
        }
        return Response.ok(representation).build();
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateGroup(@PathParam(value = "key") String key, GroupRepresentation group, @QueryParam("members") @DefaultValue("true") boolean members) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "PUT /groups/" + key);
        membership.updateGroup(key, group.getName(), group.getDescription());
        Group ngroup = membership.readGroup(key);
        GroupRepresentation representation = GroupRepresentation.fromGroup(ngroup);
        if ( members ) {
            for (String member : ngroup.getMembers()) {
                representation.addMember(ProfileCardRepresentation.fromProfile(membership.readProfile(member)));
            }
        }
        return Response.ok(representation).build();
    }

    @PUT
    @Path("/{key}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateGroupFromForm(@PathParam(value = "key") String key, @FormParam("name") String name, @FormParam("description") String description, @QueryParam("members") @DefaultValue("true") boolean members) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "PUT /groups/" + key);
        membership.updateGroup(key, name, description);
        Group group = membership.readGroup(key);
        GroupRepresentation representation = GroupRepresentation.fromGroup(group);
        if ( members ) {
            for (String member : group.getMembers()) {
                representation.addMember(ProfileCardRepresentation.fromProfile(membership.readProfile(member)));
            }
        }
        return Response.ok(representation).build();
    }

    @DELETE
    @Path("/{key}")
    public Response deleteGroup(@PathParam(value = "key") String key) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "DELETE /groups/" + key);
        membership.deleteGroup(key);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{key}/members/{member}")
    public Response addMember(@PathParam(value = "key") String key, @PathParam(value = "member") String member, @QueryParam("members") @DefaultValue("true") boolean members) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "PUT /groups/" + key + "/members/" + member);
        membership.addMemberInGroup(key, member);
        Group group = membership.readGroup(key);
        GroupRepresentation representation = GroupRepresentation.fromGroup(group);
        if ( members ) {
            for (String gmember : group.getMembers()) {
                representation.addMember(ProfileCardRepresentation.fromProfile(membership.readProfile(gmember)));
            }
        }
        return Response.ok(representation).build();
    }

    @DELETE
    @Path("/{key}/members/{member}")
    public Response removeMember(@PathParam(value = "key") String key, @PathParam(value = "member") String member, @QueryParam("members") @DefaultValue("true") boolean members) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "DELETE /groups/" + key + "/members/" + member);
        membership.removeMemberFromGroup(key, member);
        Group group = membership.readGroup(key);
        GroupRepresentation representation = GroupRepresentation.fromGroup(group);
        if ( members ) {
            for (String gmember : group.getMembers()) {
                representation.addMember(ProfileCardRepresentation.fromProfile(membership.readProfile(gmember)));
            }
        }
        return Response.ok(representation).build();
    }

}
