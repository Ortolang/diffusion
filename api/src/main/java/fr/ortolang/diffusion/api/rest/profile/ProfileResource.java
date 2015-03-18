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

import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileData;
import fr.ortolang.diffusion.membership.entity.ProfileDataType;
import fr.ortolang.diffusion.membership.entity.ProfileDataVisibility;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @resourceDescription Operations on Profiles
 */
@Path("/profiles")
@Produces({ MediaType.APPLICATION_JSON })
public class ProfileResource {

	private Logger logger = Logger.getLogger(ProfileResource.class.getName());

	@EJB
	private BrowserService browser;
	@EJB
	private MembershipService membership;

	public ProfileResource() {
	}

	/**
	 * @responseType fr.ortolang.diffusion.api.rest.profile.ProfileRepresentation
	 * @return {@link fr.ortolang.diffusion.api.rest.profile.ProfileRepresentation}
	 * @throws MembershipServiceException
	 * @throws KeyNotFoundException
	 * @throws ProfileAlreadyExistsException
	 * @throws AccessDeniedException
	 */
	@GET
	@Path("/connected")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getConnected() throws MembershipServiceException, KeyNotFoundException, ProfileAlreadyExistsException, AccessDeniedException {
		logger.log(Level.INFO, "GET /profiles/connected");
		String key = membership.getProfileKeyForConnectedIdentifier();
		Profile profile;
		try {
			profile = membership.readProfile(key);
		} catch (KeyNotFoundException e) {
			profile = membership.createProfile("", "", "");
		}
		ProfileRepresentation representation = ProfileRepresentation.fromProfile(profile);
		return Response.ok(representation).build();
	}
	
	@GET
	@Path("/list")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getProfiles() throws MembershipServiceException, KeyNotFoundException, AccessDeniedException, AuthorisationServiceException {
		logger.log(Level.INFO, "GET /profiles/list");
		List<Profile> profiles;
		profiles = membership.listProfiles();
		return Response.ok(profiles).build();
	}
	
	@POST
	@Path("/search")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchProfile(String data) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException, KeyLockedException, AuthorisationServiceException {
		logger.log(Level.INFO, "POST /profiles/search");
		logger.log(Level.INFO, data.toString());
		List<Profile> result = membership.search(data);
		return Response.ok(result).build();
	}

	/**
	 * @responseType fr.ortolang.diffusion.api.rest.profile.ProfileRepresentation
	 * @param key
	 * @param request
	 * @return {@link fr.ortolang.diffusion.api.rest.profile.ProfileRepresentation}
	 * @throws MembershipServiceException
	 * @throws BrowserServiceException
	 * @throws KeyNotFoundException
	 * @throws AccessDeniedException
	 */
	@GET
	@Path("/{key}")
	@Template(template = "profiles/detail.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getProfile(@PathParam(value = "key") String key, @Context Request request) throws MembershipServiceException, BrowserServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "GET /profiles/" + key);
		
		OrtolangObjectState state = browser.getState(key);
		CacheControl cc = new CacheControl();
		cc.setPrivate(true);
		if ( state.isLocked() ) {
			cc.setMaxAge(31536000);
			cc.setMustRevalidate(false);
		} else {
			cc.setMaxAge(0);
			cc.setMustRevalidate(true);
		}
		Date lmd = new Date((state.getLastModification()/1000)*1000);
		ResponseBuilder builder = request.evaluatePreconditions(lmd);
		
		if(builder == null){
			Profile profile = membership.readProfile(key);
			ProfileRepresentation representation = ProfileRepresentation.fromProfile(profile);
			builder = Response.ok(representation);
    		builder.lastModified(lmd);
        }

        builder.cacheControl(cc);
        Response response = builder.build();
        return response;
	}
	
	@GET
	@Path("/{key}/friends")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getFriends(@PathParam(value = "key") String key, @Context Request request) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
		logger.log(Level.INFO, "GET /profiles/" + key + "/friends");
				
		List<String> friends = membership.listFriends(key);		
		return Response.ok(friends).build();
	}
	
	@GET
	@Path("/{key}/infos")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getInfos(@PathParam(value = "key") String key, @Context Request request) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
		logger.log(Level.INFO, "GET /profiles/" + key + "/infos");
				
		List<ProfileDataRepresentation> infosRepresentation = new ArrayList<ProfileDataRepresentation>();
		Map<String, ProfileData> infos = membership.listInfos(key);		
		for(Entry<String, ProfileData> entry : infos.entrySet()) {			
		    ProfileData data = entry.getValue();
			ProfileDataRepresentation dataRepresentation = ProfileDataRepresentation.fromProfileData(data);
			infosRepresentation.add(dataRepresentation);
		}	
		return Response.ok(infosRepresentation).build();
	}
	

	@GET
	@Path("/{key}/infos/{name}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getInfo(@PathParam(value = "key") String key, @PathParam(value = "name") String name, @Context Request request) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException, RegistryServiceException {
		logger.log(Level.INFO, "GET /profiles/" + key + "/infos/" + name);
				
		ProfileData info = membership.readInfo(key, name);		
		ProfileDataRepresentation dataRepresentation = ProfileDataRepresentation.fromProfileData(info);

		return Response.ok(dataRepresentation).build();
	}

	@POST
	@Path("/{key}/infos")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
	public Response updateInfos(@PathParam(value = "key") String key, ProfileDataRepresentation info) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException, KeyLockedException {
		logger.log(Level.INFO, "POST /profiles/" + key + "/infos");
		membership.updateInfo(key, info.getName(), info.getValue(), ProfileDataVisibility.valueOf(info.getVisibility()), ProfileDataType.valueOf(info.getType()), info.getSource());
		return Response.ok().build();
	}
	

	@GET
	@Path("/{key}/aboutme/{name}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getAboutMe(@PathParam(value = "key") String key, @PathParam(value = "name") String name, @Context Request request) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException, RegistryServiceException, NotificationServiceException {
		logger.log(Level.INFO, "GET /profiles/" + key + "/aboutme/" + name);
				
		ProfileData presentation = membership.readAboutMe(key, name);	
		ProfileDataRepresentation dataRepresentation = ProfileDataRepresentation.fromProfileData(presentation);
	
		return Response.ok(dataRepresentation).build();
	}

	@POST
	@Path("/{key}/aboutme")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
	public Response updatePresentation(@PathParam(value = "key") String key, ProfileDataRepresentation presentation) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException, KeyLockedException, RegistryServiceException, NotificationServiceException {
		logger.log(Level.INFO, "POST /profiles/" + key + "/aboutme");
		membership.updateAboutMe(key, presentation.getName(), presentation.getValue(), ProfileDataVisibility.valueOf(presentation.getVisibility()), ProfileDataType.valueOf(presentation.getType()), presentation.getSource());
		return Response.ok().build();
	}

	@GET
	@Path("/{key}/settings")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getSettings(@PathParam(value = "key") String key, @Context Request request) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException {
		logger.log(Level.INFO, "GET /profiles/" + key + "/settings");
				
		List<ProfileDataRepresentation> infosRepresentation = new ArrayList<ProfileDataRepresentation>();
		Map<String, ProfileData> settings = membership.listSettings(key);		
		for(Entry<String, ProfileData> entry : settings.entrySet()) {			
		    ProfileData data = entry.getValue();
			ProfileDataRepresentation dataRepresentation = ProfileDataRepresentation.fromProfileData(data);
			infosRepresentation.add(dataRepresentation);
		}	
		return Response.ok(infosRepresentation).build();
	}
	

	@GET
	@Path("/{key}/settings/{name}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getSettings(@PathParam(value = "key") String key, @PathParam(value = "name") String name, @Context Request request) throws MembershipServiceException, AccessDeniedException, KeyNotFoundException, RegistryServiceException {
		logger.log(Level.INFO, "GET /profiles/" + key + "/settings/" + name);
				
		ProfileData setting = membership.readSetting(key, name);		
		ProfileDataRepresentation dataRepresentation = ProfileDataRepresentation.fromProfileData(setting);

		return Response.ok(dataRepresentation).build();
	}

	@POST
	@Path("/{key}/settings")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED })
	public Response updateSettings(@PathParam(value = "key") String key, ProfileDataRepresentation setting) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException, KeyLockedException {
		logger.log(Level.INFO, "POST /profiles/" + key + "/settings");
		membership.updateSetting(key, setting.getName(), setting.getValue(), ProfileDataVisibility.valueOf(setting.getVisibility()), ProfileDataType.valueOf(setting.getType()), setting.getSource());
		return Response.ok().build();
	}

	@PUT
	@Path("/{key}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateProfile(@PathParam(value = "key") String key, ProfileRepresentation representation) throws MembershipServiceException, KeyNotFoundException,
			AccessDeniedException {
		logger.log(Level.INFO, "PUT /profiles/" + key);
		membership.updateProfile(key, representation.getGivenName(), representation.getFamilyName(), representation.getEmail());
		return Response.noContent().build();
	}
	
	@GET
	@Path("/{key}/keys")
	@Template(template = "profiles/keys.vm", types = { MediaType.TEXT_HTML })
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML })
	public Response getProfilePublicKeys(@PathParam(value = "key") String key) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "GET /profiles/" + key + "/keys");
		Profile profile = membership.readProfile(key);
		return Response.ok(profile.getPublicKeys()).build();
	}
	
	@POST
	@Path("/{key}/keys")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response addProfilePublicKey(@PathParam(value = "key") String key, ProfileKeyRepresentation pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "POST /profiles/" + key + "/keys");
		membership.addProfilePublicKey(key, pubkey.getPublicKey());
		return Response.ok().build();
	}
	
	@DELETE
	@Path("/{key}/keys")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response removeProfilePublicKey(@PathParam(value = "key") String key, ProfileKeyRepresentation pubkey) throws MembershipServiceException, KeyNotFoundException, AccessDeniedException {
		logger.log(Level.INFO, "POST /profiles/" + key + "/keys");
		membership.removeProfilePublicKey(key, pubkey.getPublicKey());
		return Response.ok().build();
	}

}
