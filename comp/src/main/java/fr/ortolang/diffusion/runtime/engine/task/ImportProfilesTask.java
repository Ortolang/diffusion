package fr.ortolang.diffusion.runtime.engine.task;

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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.membership.entity.ProfileDataType;
import fr.ortolang.diffusion.membership.entity.ProfileDataVisibility;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public class ImportProfilesTask extends RuntimeEngineTask {
    private static final Logger LOGGER = Logger.getLogger(ImportProfilesTask.class.getName());

    public static final String NAME = "Import Profiles";

    public ImportProfilesTask() {
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        checkParameters(execution);
        String profilespath = execution.getVariable(PROFILES_PATH_PARAM_NAME, String.class);
        boolean overwrite = false;
        if (execution.hasVariable(PROFILES_OVERWRITE_PARAM_NAME)) {
            overwrite = Boolean.parseBoolean(execution.getVariable(PROFILES_OVERWRITE_PARAM_NAME, String.class));
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        try {
            List<JsonProfile> profiles = Arrays.asList(mapper.readValue(new File(profilespath), JsonProfile[].class));
            LOGGER.log(Level.FINE, "- starting import profiles");
            boolean partial = false;
            boolean exists;
            StringBuilder report = new StringBuilder();
            for (JsonProfile profile : profiles) {
                exists = false;
                try {
                    getMembershipService().readProfile(profile.pro_login);
                    LOGGER.log(Level.FINE, "  profile already exists for username: " + profile.pro_login);
                    exists = true;
                } catch ( KeyNotFoundException | AccessDeniedException | MembershipServiceException e) {
                    //
                }
                if ( !exists ) {
                    try {
                        getMembershipService().createProfile(profile.pro_login, profile.pro_firstname, profile.pro_lastname, profile.pro_emailt, ProfileStatus.ACTIVE);
                    } catch (ProfileAlreadyExistsException | AccessDeniedException | MembershipServiceException e) {
                        partial = true;
                        report.append("creation failed for: ").append(profile.pro_login).append("\r\n");
                        LOGGER.log(Level.SEVERE, "  unable to create profile: " + profile.pro_login, e);
                    }
                }
                if ( exists && overwrite ) {
                    try {
                        getMembershipService().updateProfile(profile.pro_login, profile.pro_firstname, profile.pro_lastname, profile.pro_emailt, null);
                    } catch (KeyNotFoundException | AccessDeniedException | MembershipServiceException e) {
                        partial = true;
                        report.append("update failed for existing profile: ").append(profile.pro_login).append("\r\n");
                        LOGGER.log(Level.SEVERE, "  unable to update profile: " + profile.pro_login, e);
                    }
                }
                if (!exists || (exists && overwrite)) {
                    try {
                        if (profile.pro_genre != null && profile.pro_genre.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "civility", profile.pro_genre, ProfileDataVisibility.EVERYBODY, ProfileDataType.ENUM, "");
                        }
                        if (profile.pro_titre != null && profile.pro_titre.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "title", profile.pro_titre, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        if (profile.pro_firstname != null && profile.pro_firstname.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "given_name", profile.pro_firstname, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        if (profile.pro_lastname != null && profile.pro_lastname.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "family_name", profile.pro_lastname, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        if (profile.pro_emailt != null && profile.pro_emailt.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "professional_email", profile.pro_emailt, ProfileDataVisibility.EVERYBODY, ProfileDataType.EMAIL, "");
                        }
                        if (profile.pro_email != null && profile.pro_email.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "rescue_email", profile.pro_email, ProfileDataVisibility.EVERYBODY, ProfileDataType.EMAIL, "");
                        }
                        if (profile.pro_organisme != null && profile.pro_organisme.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "organisation", profile.pro_organisme, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        if (profile.pro_metier != null && profile.pro_metier.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "job", profile.pro_metier, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        if (profile.pro_domaine_recherche != null && profile.pro_domaine_recherche.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "field_of_research", profile.pro_domaine_recherche, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        String address = profile.pro_adresse + ", " + profile.pro_cp + ", " + profile.pro_ville + ", " + profile.pro_pays_nom;
                        if (address.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "address", address, ProfileDataVisibility.EVERYBODY, ProfileDataType.ADDRESS, "");
                        }
                        if (profile.pro_organisme_url != null && profile.pro_organisme_url.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "website", profile.pro_organisme_url, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        if (profile.pro_telephonet != null && profile.pro_telephonet.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "professional_tel", profile.pro_telephonet, ProfileDataVisibility.EVERYBODY, ProfileDataType.TEL, "");
                        }
                        if (profile.pro_telephone != null && profile.pro_telephone.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "tel", profile.pro_telephone, ProfileDataVisibility.EVERYBODY, ProfileDataType.TEL, "");
                        }
                        if (profile.pro_telecopie != null && profile.pro_telecopie.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "fax", profile.pro_telecopie, ProfileDataVisibility.EVERYBODY, ProfileDataType.TEL, "");
                        }
                        if (profile.pro_langue != null && profile.pro_langue.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "language", profile.pro_langue, ProfileDataVisibility.EVERYBODY, ProfileDataType.ENUM, "");
                        }
                        if (profile.pro_id_orcid != null && profile.pro_id_orcid.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "orcid", profile.pro_id_orcid, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        if (profile.pro_id_viaf != null && profile.pro_id_viaf.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "viaf", profile.pro_id_viaf, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        if (profile.pro_id_idref != null && profile.pro_id_idref.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "myidref", profile.pro_id_idref, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                        if (profile.pro_id_linkedin != null && profile.pro_id_linkedin.length() > 0) {
                            getMembershipService().setProfileInfo(profile.pro_login, "linkedin", profile.pro_id_linkedin, ProfileDataVisibility.EVERYBODY, ProfileDataType.STRING, "");
                        }
                    } catch (KeyNotFoundException | AccessDeniedException | MembershipServiceException e) {
                        partial = true;
                        report.append("unable to set info for profile: ").append(profile.pro_login).append("\r\n");
                        LOGGER.log(Level.SEVERE, "  unable to set profile info for identifier: " + profile.pro_login, e);
                    }
                }
            }

            if (partial) {
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Some profiles has not been imported (see trace for detail)"));
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), report.toString(), null));
            }
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Import Profiles done"));
        } catch (IOException e) {
            throw new RuntimeEngineTaskException("error parsing json file: " + e.getMessage());
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

    private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
        if (!execution.hasVariable(PROFILES_PATH_PARAM_NAME)) {
            throw new RuntimeEngineTaskException("execution variable " + PROFILES_PATH_PARAM_NAME + " is not set");
        }
    }

    private static class JsonProfile {
        public String pro_id = "";
        public String pro_login = "";
        public String pro_genre = "";
        public String pro_titre = "";
        public String pro_firstname = "";
        public String pro_lastname = "";
        public String pro_emailt = "";
        public String pro_email = "";
        public String pro_organisme = "";
        public String pro_metier = "";
        public String pro_domaine_recherche = "";
        public String pro_adresse = "";
        public String pro_cp = "";
        public String pro_ville = "";
        public String pro_pays_id = "";
        public String pro_pays_nom = "";
        public String pro_organisme_url = "";
        public String pro_telephonet = "";
        public String pro_telephone = "";
        public String pro_telecopie = "";
        public String pro_langue = "";
        public String pro_id_orcid = "";
        public String pro_id_viaf = "";
        public String pro_id_idref = "";
        public String pro_id_linkedin = "";

        public JsonProfile() {
        }
    }

}