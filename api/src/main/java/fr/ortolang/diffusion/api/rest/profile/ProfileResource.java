package fr.ortolang.diffusion.api.rest.profile;

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

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.membership.entity.ProfileDataType;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authentication.TicketHelper;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @resourceDescription Operations on Profiles
 */
@Path("/profiles")
@Produces({ MediaType.APPLICATION_JSON })
public class ProfileResource {

	private static final Logger LOGGER = Logger.getLogger(ProfileResource.class.getName());

	@EJB
	private BrowserService browser;
	@EJB
	private MembershipService membership;
	@EJB
	private AuthorisationService authorisation;

	public ProfileResource() {
	}

	/**
	 * @description Connect to current profile
	 * @responseType fr.ortolang.diffusion.api.rest.profile.ProfileRepresentation
	 * @return {@link ProfileRepresentation}
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws ProfileAlreadyExistsException
	 * @throws AccessDeniedException
	 */
	@GET
	@Path("/connected")
	public Response getConnected() throws MembershipServiceException, KeyNotFoundException, ProfileAlreadyExistsException, AccessDeniedException {
		LOGGER.log(Level.INFO, "GET /profiles/connected");
		String key = membership.getProfileKeyForConnectedIdentifier();
		Profile profile;
		try {
			profile = membership.readProfile(key);
		} catch (KeyNotFoundException e) {
			profile = membership.createProfile("", "", "");
		}
		ProfileRepresentation representation = ProfileRepresentation.fromProfile(profile);
		List<String> friends = membership.listMembers(profile.getFriends());
		representation.setFriends(friends.toArray(new String[friends.size()]));
		return Response.ok(representation).build();
	}
	
	/**
	 * @description List profiles
	 * @responseType fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation
	 * @return {@link GenericCollectionRepresentation}&lt;{@link ProfileRepresentation}&gt;
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 * @throws AuthorisationServiceException
	 */
	@GET
	@Path("/list")
	public Response getProfiles() throws MembershipServiceException, KeyNotFoundException, AccessDeniedException, AuthorisationServiceException {
		LOGGER.log(Level.INFO, "GET /profiles/list");
		GenericCollectionRepresentation<ProfileRepresentation> representation = new GenericCollectionRepresentation<ProfileRepresentation>();
		List<Profile> results = membership.listProfiles();
		for (Profile profile : results) {
			ProfileRepresentation profileRepresentation = ProfileRepresentation.fromProfile(profile);
			List<String> friends = membership.listMembers(profile.getFriends());
			profileRepresentation.setFriends(friends.toArray(new String[friends.size()]));
			representation.addEntry(profileRepresentation);	
		}
		representation.setOffset(0);
		representation.setSize(results.size());
		representation.setLimit(results.size());
		return Response.ok(representation).build();
	}
	
	/**
	 * @description Search in profile for a fullName matching data
	 * @responseType fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation
	 * @param data {@link String}
	 * 		String to find in profiles
	 * @return {@link GenericCollectionRepresentation}&lt;{@link ProfileRepresentation}&gt;
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 * @throws KeyLockedException
	 * @throws AuthorisationServiceException
	 */
	@POST
	@Path("/search")
	public Response searchProfile(String data) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException, KeyLockedException, AuthorisationServiceException {
		LOGGER.log(Level.INFO, "POST /profiles/search");
		GenericCollectionRepresentation<ProfileRepresentation> representation = new GenericCollectionRepresentation<ProfileRepresentation>();
		List<Profile> results = membership.searchProfile(data);
		for (Profile profile : results) {
			ProfileRepresentation profileRepresentation = ProfileRepresentation.fromProfile(profile);
			List<String> friends = membership.listMembers(profile.getFriends());
			profileRepresentation.setFriends(friends.toArray(new String[friends.size()]));
			representation.addEntry(profileRepresentation);			
		}
		representation.setOffset(0);
		representation.setSize(results.size());
		representation.setLimit(results.size());
		return Response.ok(representation).build();
	}

	/**
	 * @description Return profile for a given key
	 * @responseType fr.ortolang.diffusion.api.rest.profile.ProfileRepresentation
	 * @param key {@link String}
	 * 		Key of wanted profile
	 * @param request {@link Request} 		 
	 * @return {@link ProfileRepresentation}
	 * @throws MembershipServiceException
	 * @throws BrowserServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 */
	@GET
	@Path("/{key}")
	public Response getProfile(@PathParam(value = "key") String key, @Context Request request) throws MembershipServiceException, BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.INFO, "GET /profiles/" + key);
		
		OrtolangObjectState state = browser.getState(key);
		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		cc.setMaxAge(0);
		cc.setMustRevalidate(true);
		Date lmd = new Date(state.getLastModification() / 1000 * 1000);
		ResponseBuilder builder = null;
		if (System.currentTimeMillis() - state.getLastModification() > 1000) {
			builder = request.evaluatePreconditions(lmd);
		}
		
		if(builder == null){
			Profile profile = membership.readProfile(key);
			ProfileRepresentation representation = ProfileRepresentation.fromProfile(profile);
			List<String> friends = membership.listMembers(profile.getFriends());
			representation.setFriends(friends.toArray(new String[friends.size()]));
			builder = Response.ok(representation);
    		builder.lastModified(lmd);
        }

        builder.cacheControl(cc);
        return builder.build();
	}
	
	/**
	 * @description Update a profile
	 * @param key {@link String}
	 * 		Key of the profile to update {@link String}
	 * @param representation {@link ProfileRepresentation}
	 * 		New value
	 * @return {@link Response}
	 * 		Empty response
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 */
	@PUT
	@Path("/{key}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateProfile(@PathParam(value = "key") String key, ProfileRepresentation representation) throws MembershipServiceException, KeyNotFoundException,
			AccessDeniedException {
		LOGGER.log(Level.INFO, "PUT /profiles/" + key);
		Profile profile = membership.updateProfile(key, representation.getGivenName(), representation.getFamilyName(), representation.getEmail(), representation.getEmailVisibility());
		ProfileRepresentation profileRepresentation = ProfileRepresentation.fromProfile(profile);
		return Response.ok(profileRepresentation).build();
	}
	
	/**
	 * @description Return public keys of a profile
	 * @param key {@link String}
	 * 		Key of the profile {@link String}
	 * @return {@link Set}&lt;{@link String}&gt;
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 */
	@GET
	@Path("/{key}/keys")
	public Response getProfilePublicKeys(@PathParam(value = "key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.INFO, "GET /profiles/" + key + "/keys");
		Profile profile = membership.readProfile(key);
		return Response.ok(profile.getPublicKeys()).build();
	}
	
	/**
	 * @description Add a Public key to a profile
	 * @param key {@link String}
	 * 		Key of the profile {@link String}
	 * @param pubkey {@link ProfileKeyRepresentation}
	 * 		New public key value
	 * @return {@link Response}
	 * 		Response with OK status
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 */
	@POST
	@Path("/{key}/keys")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response addProfilePublicKey(@PathParam(value = "key") String key, ProfileKeyRepresentation pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.INFO, "POST /profiles/" + key + "/keys");
		membership.addProfilePublicKey(key, pubkey.getPublicKey());
		return Response.ok().build();
	}
	
	/**
	 * @description Delete a public key of a profile
	 * @param key {@link String}
	 * 		Key of the profile {@link String}
	 * @param pubkey {@link ProfileKeyRepresentation}
	 * 		New public key value
	 * @return {@link Response}
	 * 		Response with OK status
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 */
	@DELETE
	@Path("/{key}/keys")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response removeProfilePublicKey(@PathParam(value = "key") String key, ProfileKeyRepresentation pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.INFO, "DELETE /profiles/" + key + "/keys");
		membership.removeProfilePublicKey(key, pubkey.getPublicKey());
		return Response.ok().build();
	}
	
	/**
	 * @description Return collection of infos of a given profile
	 * @responseType fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation
	 * @param key {@link String}
	 * 		Key of the profile {@link String}
	 * @param filter {@link String}
	 * 		Category of infos to return
	 * @param request {@link Request}
	 * @return {@link GenericCollectionRepresentation}&lt;{@link ProfileDataRepresentation}&gt;
	 * @throws MembershipServiceException
	 * @throws BrowserServiceException
	 * @throws AccessDeniedException
	 * @throws KeyNotFoundException
	 */
	@GET
	@Path("/{key}/infos")
	public Response getInfos(@PathParam(value = "key") String key, @QueryParam(value = "filter") String filter, @Context Request request) throws MembershipServiceException, BrowserServiceException, AccessDeniedException, KeyNotFoundException {
		LOGGER.log(Level.INFO, "GET /profiles/" + key + "/infos");
				
		OrtolangObjectState state = browser.getState(key);
		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		cc.setMaxAge(0);
		cc.setMustRevalidate(true);
		Date lmd = new Date(state.getLastModification() / 1000 * 1000);
		ResponseBuilder builder = null;
		if (System.currentTimeMillis() - state.getLastModification() > 1000) {
			builder = request.evaluatePreconditions(lmd);
		}
		
		if(builder == null){
			List<ProfileData> infos = membership.listProfileInfos(key, filter);	
			GenericCollectionRepresentation<ProfileDataRepresentation> representation = new GenericCollectionRepresentation<ProfileDataRepresentation>();
			for (ProfileData info : infos) {
				representation.addEntry(ProfileDataRepresentation.fromProfileData(info));
			}
			representation.setOffset(0);
			representation.setSize(infos.size());
			representation.setLimit(infos.size());
			builder = Response.ok(representation);
    		builder.lastModified(lmd);
        }

        builder.cacheControl(cc);
        return builder.build();
	}
	
	/**
	 * @description Update infos for a given profile
	 * @param key {@link String}
	 * 		Key of the profile
	 * @param info {@link ProfileDataRepresentation}
	 * 		New infos values
	 * @return {@link Response}
	 * 		Response with OK status
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 * @throws KeyLockedException
	 */
	@POST
	@Path("/{key}/infos")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response updateInfos(@PathParam(value = "key") String key, ProfileDataRepresentation info) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException, KeyLockedException {
		LOGGER.log(Level.INFO, "POST /profiles/" + key + "/infos");
		membership.setProfileInfo(key, info.getName(), info.getValue(), info.getVisibility(), ProfileDataType.valueOf(info.getType()), info.getSource());
		return Response.ok().build();
	}

	/**
	 * @description Add a friend to a profile
	 * @param key {@link String}
	 * 		Key of the profile
	 * @param friendKey {@link String}
	 * 		Key of the profile of the friend to add
	 * @return {@link Response}
	 * 		Response with OK status
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 * @throws KeyAlreadyExistsException
	 * @throws AuthorisationServiceException
	 */
	@POST
	@Path("/{key}/friend")
	public Response addFriend(@PathParam(value = "key") String key, String friendKey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException, KeyAlreadyExistsException, AuthorisationServiceException {
		LOGGER.log(Level.INFO, "POST /profiles/" + key + "/friend");
		String friendGroupKey = membership.readProfile(key).getFriends();
		membership.addMemberInGroup(friendGroupKey, friendKey);
		return Response.ok().build();
	}
	
	/**
	 * @description Remove a friend from a profile
	 * @param key {@link String}
	 * 		Key of the profile
	 * @param friendKey {@link String}
	 * 		Key of the profile of the friend to add
	 * @return {@link Response}
	 * 		Response with OK status
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 */
	@DELETE
	@Path("/{key}/friend")
	public Response removeFriend(@PathParam(value = "key") String key, String friendKey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		LOGGER.log(Level.INFO, "DELETE /profiles/" + key + "/friend");
		membership.removeMemberFromGroup(key, friendKey);
		return Response.ok().build();
	}
	
	/**
	 * @description Return the size of a profile
	 * @param key {@link String}
	 * 		Key of the profile
	 * @return {@link OrtolangObjectSize}
	 * 		Size of the profile
	 * @throws AccessDeniedException
	 * @throws OrtolangException
	 * @throws KeyNotFoundException
	 */
    @GET
    @Path("/{key}/size")
    public Response getProfileSize(@PathParam(value = "key") String key) throws AccessDeniedException, OrtolangException, KeyNotFoundException {
        LOGGER.log(Level.INFO, "GET /profiles/" + key + "/size");
        OrtolangObjectSize size = membership.getSize(key);
        return Response.ok(size).build();
    }

	@GET
	@Path("/{key}/ticket")
	public Response getProfileTicket(@PathParam(value = "key") String key) throws AccessDeniedException, OrtolangException, KeyNotFoundException, MembershipServiceException {
		LOGGER.log(Level.INFO, "GET /profiles/" + key + "/size");
		Profile profile = membership.readProfile(key);
		String ticket = TicketHelper.makeTicket(profile.getId(), profile.getKey());
		JsonObject jsonObject = Json.createObjectBuilder().add("t", ticket).build();
		return Response.ok(jsonObject).build();
	}

}
