package fr.ortolang.diffusion.bootstrap;

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.WorkspaceType;
import fr.ortolang.diffusion.form.FormService;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.entity.AuthorisationPolicyTemplate;
import fr.ortolang.diffusion.store.binary.DataCollisionException;

@Startup
@Singleton(name = BootstrapService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
@RunAs("user")
@RunAsPrincipal(value = MembershipService.SUPERUSER_IDENTIFIER)
public class BootstrapServiceBean implements BootstrapService {

    private static final Logger LOGGER = Logger.getLogger(BootstrapServiceBean.class.getName());

    @EJB
    private RegistryService registry;
    @EJB
    private MembershipService membership;
    @EJB
    private AuthorisationService authorisation;
    @EJB
    private CoreService core;
    @EJB
    private RuntimeService runtime;
    @EJB
    private FormService form;

    public BootstrapServiceBean() {
        LOGGER.log(Level.FINE, "new bootstrap service instance created");
    }

    @PostConstruct
    public void init() {
        try {
            String version = getClass().getPackage().getImplementationVersion();
            LOGGER.log(Level.INFO, "\n\n" 
                    + "      ____  ____  __________  __    ___    _   ________\n" 
                    + "     / __ \\/ __ \\/_  __/ __ \\/ /   /   |  / | / / ____/\n"
                    + "    / / / / /_/ / / / / / / / /   / /| | /  |/ / / __  \n" 
                    + "   / /_/ / _, _/ / / / /_/ / /___/ ___ |/ /|  / /_/ /  \n"
                    + "   \\____/_/ |_| /_/  \\____/_____/_/  |_/_/ |_/\\____/   \n"
                    + (version.contains("SNAPSHOT") ? "\n                                    " : "\n                                             ") + version + "\n");
            OrtolangConfig.getInstance();
            bootstrap();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "error during bootstrap");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void bootstrap() throws BootstrapServiceException {
        LOGGER.log(Level.INFO, "Starting bootstrap...");

        Map<String, List<String>> anonReadRules = new HashMap<String, List<String>>();
        anonReadRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Collections.singletonList("read"));

        try {

            if (!registry.exists(MembershipService.SUPERUSER_IDENTIFIER)) {
                LOGGER.log(Level.FINE, "creating root profile");
                membership.createProfile(MembershipService.SUPERUSER_IDENTIFIER, "Super", "User", "root@ortolang.org", ProfileStatus.ACTIVE);
                membership
                        .addProfilePublicKey(
                                MembershipService.SUPERUSER_IDENTIFIER,
                                "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDqv8kWdXIgWbFJfOiu9fQYiazwfnpZogatgo3278PIAQ4eaj6p+sjQMQX0hew+rHXCqvi6FaG6Lql7fkJv/NJpSyhyCKYNCxmYKwUvaViOuRLDmnpziEX39WDmiWBE0Q+DNuKIZMx3yZNX/BeBp0FfooKkCnZWEDo4pzcYVp2RlwZuEDZQcQ6KP2S9+z2WQPmsE9tcyPNL12hp8tiG8J/XsPXxnn1mgJxyiwQmEYDxXZTAazeewqftz4GU3Xc9qWOa4GXK/2l0GB/XVuFLoyrXve+hnsiFpeIslJuGl0+AAX+lCULjDcA72r4aT30Z4HV+wxiQxk/j+2CtCw/vfeit achile-laptop");
            }

            if (!registry.exists(MembershipService.UNAUTHENTIFIED_IDENTIFIER)) {
                LOGGER.log(Level.FINE, "creating anonymous profile");
                membership.createProfile(MembershipService.UNAUTHENTIFIED_IDENTIFIER, "Anonymous", "User", "anon@ortolang.org", ProfileStatus.ACTIVE);
                LOGGER.log(Level.FINE, "change owner of anonymous profile to root and set anon read rules");
                authorisation.updatePolicyOwner(MembershipService.UNAUTHENTIFIED_IDENTIFIER, MembershipService.SUPERUSER_IDENTIFIER);
                authorisation.setPolicyRules(MembershipService.UNAUTHENTIFIED_IDENTIFIER, anonReadRules);
            }

            if (!registry.exists(MembershipService.MODERATORS_GROUP_KEY)) {
                LOGGER.log(Level.FINE, "creating moderators group");
                membership.createGroup(MembershipService.MODERATORS_GROUP_KEY, "Publication Moderators", "Moderators of the platform check technical aspect of publication");
                membership.addMemberInGroup(MembershipService.MODERATORS_GROUP_KEY, MembershipService.SUPERUSER_IDENTIFIER);
                authorisation.setPolicyRules(MembershipService.MODERATORS_GROUP_KEY, anonReadRules);
            }

            if (!registry.exists(MembershipService.PUBLISHERS_GROUP_KEY)) {
                LOGGER.log(Level.FINE, "creating publishers group");
                membership.createGroup(MembershipService.PUBLISHERS_GROUP_KEY, "Publishers", "Publishers of the platform validates final publication");
                membership.addMemberInGroup(MembershipService.PUBLISHERS_GROUP_KEY, MembershipService.SUPERUSER_IDENTIFIER);
                authorisation.setPolicyRules(MembershipService.PUBLISHERS_GROUP_KEY, anonReadRules);
            }

            if (!registry.exists(MembershipService.REVIEWERS_GROUP_KEY)) {
                LOGGER.log(Level.FINE, "creating reviewers group");
                membership.createGroup(MembershipService.REVIEWERS_GROUP_KEY, "Reviewers", "Reviewers of the platform rate content");
                membership.addMemberInGroup(MembershipService.REVIEWERS_GROUP_KEY, MembershipService.SUPERUSER_IDENTIFIER);
                authorisation.setPolicyRules(MembershipService.REVIEWERS_GROUP_KEY, anonReadRules);
            }

            if (!registry.exists(MembershipService.ESR_GROUP_KEY)) {
                LOGGER.log(Level.FINE, "creating esr group");
                membership.createGroup(MembershipService.ESR_GROUP_KEY, "ESR Members", "People from Superior Teaching and Research Group");
                authorisation.setPolicyRules(MembershipService.ESR_GROUP_KEY, anonReadRules);
            }

            if (!registry.exists(MembershipService.ADMINS_GROUP_KEY)) {
                LOGGER.log(Level.FINE, "creating admins group");
                membership.createGroup(MembershipService.ADMINS_GROUP_KEY, "Administrators", "Administrators of the platform");
                authorisation.setPolicyRules(MembershipService.ADMINS_GROUP_KEY, anonReadRules);
            }

            if (!registry.exists(BootstrapService.WORKSPACE_KEY)) {
                LOGGER.log(Level.FINE, "create system workspace");
                core.createWorkspace(BootstrapService.WORKSPACE_KEY, "system", "System Workspace", WorkspaceType.SYSTEM.toString());
                Properties props = new Properties();
                props.setProperty("bootstrap.status", "done");
                props.setProperty("bootstrap.timestamp", System.currentTimeMillis() + "");
                props.setProperty("bootstrap.version", BootstrapService.VERSION);
                String hash = core.put(new ByteArrayInputStream(props.toString().getBytes()));
                core.createDataObject(BootstrapService.WORKSPACE_KEY, "/bootstrap.txt", hash);
            }

            if (!authorisation.isPolicyTemplateExists(AuthorisationPolicyTemplate.DEFAULT)) {
                LOGGER.log(Level.FINE, "create [" + AuthorisationPolicyTemplate.DEFAULT + "] authorisation policy template");
                String pid = UUID.randomUUID().toString();
                authorisation.createPolicy(pid, MembershipService.SUPERUSER_IDENTIFIER);
                Map<String, List<String>> defaultPolicyRules = new HashMap<String, List<String>>();
                defaultPolicyRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList("read", "download"));
                authorisation.setPolicyRules(pid, defaultPolicyRules);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.DEFAULT, "Default template allows all users to read and download content", pid);
            }

            if (!authorisation.isPolicyTemplateExists(AuthorisationPolicyTemplate.FORALL)) {
                LOGGER.log(Level.FINE, "create [" + AuthorisationPolicyTemplate.FORALL + "] authorisation policy template");
                String pid = UUID.randomUUID().toString();
                authorisation.createPolicy(pid, MembershipService.SUPERUSER_IDENTIFIER);
                Map<String, List<String>> forallPolicyRules = new HashMap<String, List<String>>();
                forallPolicyRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList("read", "download"));
                authorisation.setPolicyRules(pid, forallPolicyRules);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.FORALL, "All users can read and download this content", pid);
            }

            if (!authorisation.isPolicyTemplateExists(AuthorisationPolicyTemplate.AUTHENTIFIED)) {
                LOGGER.log(Level.FINE, "create [" + AuthorisationPolicyTemplate.AUTHENTIFIED + "] authorisation policy template");
                String pid = UUID.randomUUID().toString();
                authorisation.createPolicy(pid, MembershipService.SUPERUSER_IDENTIFIER);
                Map<String, List<String>> authentifiedPolicyRules = new HashMap<String, List<String>>();
                authentifiedPolicyRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Collections.singletonList("read"));
                authentifiedPolicyRules.put(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY, Arrays.asList("read", "download"));
                authorisation.setPolicyRules(pid, authentifiedPolicyRules);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.AUTHENTIFIED, "All users can read this content but download is restricted to authentified users only", pid);
            }

            if (!authorisation.isPolicyTemplateExists(AuthorisationPolicyTemplate.ESR)) {
                LOGGER.log(Level.FINE, "create [" + AuthorisationPolicyTemplate.ESR + "] authorisation policy template");
                String pid = UUID.randomUUID().toString();
                authorisation.createPolicy(pid, MembershipService.SUPERUSER_IDENTIFIER);
                Map<String, List<String>> esrPolicyRules = new HashMap<String, List<String>>();
                esrPolicyRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Collections.singletonList("read"));
                esrPolicyRules.put(MembershipService.ESR_GROUP_KEY, Arrays.asList("read", "download"));
                authorisation.setPolicyRules(pid, esrPolicyRules);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.ESR, "All users can read this content but download is restricted to ESR users only", pid);
            }

            if (!authorisation.isPolicyTemplateExists(AuthorisationPolicyTemplate.RESTRICTED)) {
                LOGGER.log(Level.FINE, "create [" + AuthorisationPolicyTemplate.RESTRICTED + "] authorisation policy template");
                String pid = UUID.randomUUID().toString();
                authorisation.createPolicy(pid, MembershipService.SUPERUSER_IDENTIFIER);
                Map<String, List<String>> restrictedPolicyRules = new HashMap<String, List<String>>();
                restrictedPolicyRules.put("${workspace.members}", Arrays.asList("read", "download"));
                authorisation.setPolicyRules(pid, restrictedPolicyRules);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.RESTRICTED, "Only workspace members can read and download this content, all other users cannot see this content", pid);
            }

            InputStream is = getClass().getClassLoader().getResourceAsStream("forms/" + FormService.IMPORT_ZIP_FORM + ".json");
            String json = IOUtils.toString(is, "UTF-8");
            if (!registry.exists(FormService.IMPORT_ZIP_FORM)) {
                LOGGER.log(Level.FINE, "create form : " + FormService.IMPORT_ZIP_FORM);                
                form.createForm(FormService.IMPORT_ZIP_FORM, "Import Zip Process Start Form", json);
            } else {
                LOGGER.log(Level.FINE, "update form : " + FormService.IMPORT_ZIP_FORM);                
                form.updateForm(FormService.IMPORT_ZIP_FORM, "Import Zip Process Start Form", json);
            }
            is.close();

            is = getClass().getClassLoader().getResourceAsStream("forms/" + FormService.REVIEW_SNAPSHOT_FORM + ".json");
            json = IOUtils.toString(is, "UTF-8");
            if (!registry.exists(FormService.REVIEW_SNAPSHOT_FORM)) {
                LOGGER.log(Level.FINE, "create form : " + FormService.REVIEW_SNAPSHOT_FORM);
                form.createForm(FormService.REVIEW_SNAPSHOT_FORM, "Workspace publication's review form", json);
            } else {
                LOGGER.log(Level.FINE, "update form : " + FormService.REVIEW_SNAPSHOT_FORM);                
                form.updateForm(FormService.REVIEW_SNAPSHOT_FORM, "Workspace publication's review form", json);
            }
            is.close();

            is = getClass().getClassLoader().getResourceAsStream("forms/" + FormService.MODERATE_SNAPSHOT_FORM + ".json");
            json = IOUtils.toString(is, "UTF-8");
            if (!registry.exists(FormService.MODERATE_SNAPSHOT_FORM)) {
                LOGGER.log(Level.FINE, "create form : " + FormService.MODERATE_SNAPSHOT_FORM);
                form.createForm(FormService.MODERATE_SNAPSHOT_FORM, "Workspace publication's moderation form", json);
            } else {
                LOGGER.log(Level.FINE, "update form : " + FormService.MODERATE_SNAPSHOT_FORM);                
                form.updateForm(FormService.MODERATE_SNAPSHOT_FORM, "Workspace publication's moderation form", json);
            }
            is.close();

            is = getClass().getClassLoader().getResourceAsStream("forms/" + FormService.PUBLISH_SNAPSHOT_FORM + ".json");
            json = IOUtils.toString(is, "UTF-8");
            if (!registry.exists(FormService.PUBLISH_SNAPSHOT_FORM)) {
                LOGGER.log(Level.FINE, "create form : " + FormService.PUBLISH_SNAPSHOT_FORM);
                form.createForm(FormService.PUBLISH_SNAPSHOT_FORM, "Workspace publication's form", json);
            }else {
                LOGGER.log(Level.FINE, "update form : " + FormService.MODERATE_SNAPSHOT_FORM);                
                form.updateForm(FormService.PUBLISH_SNAPSHOT_FORM, "Workspace publication's form", json);
            }
            is.close();

            is = getClass().getClassLoader().getResourceAsStream("forms/" + FormService.ITEM_FORM + ".json");
            json = IOUtils.toString(is, "UTF-8");
            if (!registry.exists(FormService.ITEM_FORM)) {
                LOGGER.log(Level.FINE, "create form : " + FormService.ITEM_FORM);
                form.createForm(FormService.ITEM_FORM, "Schema Form for an ORTOLANG item", json);
            }else {
                LOGGER.log(Level.FINE, "update form : " + FormService.ITEM_FORM);                
                form.updateForm(FormService.ITEM_FORM, "Schema Form for an ORTOLANG item", json);
            }
            is.close();

            LOGGER.log(Level.FINE, "import metadataformat schemas");
            InputStream schemaItemInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-item-schema-0.16-with-parts.json");
            String schemaItemHash = core.put(schemaItemInputStream);
            core.createMetadataFormat(MetadataFormat.ITEM, "Les métadonnées de présentation permettent de paramétrer l\'affichage de la ressource dans la partie consultation du site. Nouvelle fonctionnalité : les sous-parties.", 
            		schemaItemHash, "ortolang-item-form", true, true);

            InputStream schemaInputStream2 = getClass().getClassLoader().getResourceAsStream("schema/ortolang-acl-schema.json");
            String schemaHash2 = core.put(schemaInputStream2);
            core.createMetadataFormat(MetadataFormat.ACL, "Les métadonnées de contrôle d'accès permettent de paramétrer la visibilité d'une ressource lors de sa publication.", schemaHash2, "", true,
                    false);

            InputStream schemaWorkspaceInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-workspace-schema.json");
            String schemaWorkspaceHash = core.put(schemaWorkspaceInputStream);
            core.createMetadataFormat(MetadataFormat.WORKSPACE, "Les métadonnées associées à un espace de travail.", schemaWorkspaceHash, "", true, true);

            InputStream schemaPidInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-pid-schema.json");
            String schemaPidHash = core.put(schemaPidInputStream);
            core.createMetadataFormat(MetadataFormat.PID, "Les métadonnées associées aux pids d'un object.", schemaPidHash, "", true, false);

            InputStream schemaThumbInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-thumb-schema.json");
            String schemaThumbHash = core.put(schemaThumbInputStream);
            core.createMetadataFormat(MetadataFormat.THUMB, "Schema for ORTOLANG objects thumbnail", schemaThumbHash, "", false, false);

            InputStream schemaTemplateInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-template-schema.json");
            String schemaTemplateHash = core.put(schemaTemplateInputStream);
            core.createMetadataFormat(MetadataFormat.TEMPLATE, "Schema for ORTOLANG collection template", schemaTemplateHash, "", false, false);

            InputStream schemaTrustRankInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-trustrank-schema.json");
            String schemaTrustRankHash = core.put(schemaTrustRankInputStream);
            core.createMetadataFormat(MetadataFormat.TRUSTRANK, "Schema for applying a trusted notation on item", schemaTrustRankHash, "", true, true);

            InputStream schemaRatingInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-rating-schema.json");
            String schemaRatingHash = core.put(schemaRatingInputStream);
            core.createMetadataFormat(MetadataFormat.RATING, "Schema for applying a rating on item", schemaRatingHash, "", true, true);

            InputStream schemaAudioInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-x-audio.json");
            String schemaAudioHash = core.put(schemaAudioInputStream);
            core.createMetadataFormat(MetadataFormat.AUDIO, "Schema for ORTOLANG audio metadata", schemaAudioHash, "", false, false);

            InputStream schemaVideoInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-x-video.json");
            String schemaVideoHash = core.put(schemaVideoInputStream);
            core.createMetadataFormat(MetadataFormat.VIDEO, "Schema for ORTOLANG video metadata", schemaVideoHash, "", false, false);

            InputStream schemaImageInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-x-image.json");
            String schemaImageHash = core.put(schemaImageInputStream);
            core.createMetadataFormat(MetadataFormat.IMAGE, "Schema for ORTOLANG image metadata", schemaImageHash, "", false, false);

            InputStream schemaXMLInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-x-xml.json");
            String schemaXmlHash = core.put(schemaXMLInputStream);
            core.createMetadataFormat(MetadataFormat.XML, "Schema for ORTOLANG XML metadata", schemaXmlHash, "", false, false);

            InputStream schemaPDFInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-x-pdf.json");
            String schemaPDFHash = core.put(schemaPDFInputStream);
            core.createMetadataFormat(MetadataFormat.PDF, "Schema for ORTOLANG PDF metadata", schemaPDFHash, "", false, false);

            InputStream schemaTextInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-x-text.json");
            String schemaTextHash = core.put(schemaTextInputStream);
            core.createMetadataFormat(MetadataFormat.TEXT, "Schema for ORTOLANG text metadata", schemaTextHash, "", false, false);

            InputStream schemaOfficeInputStream = getClass().getClassLoader().getResourceAsStream("schema/system-x-office.json");
            String schemaOfficeHash = core.put(schemaOfficeInputStream);
            core.createMetadataFormat(MetadataFormat.OFFICE, "Schema for ORTOLANG Office metadata", schemaOfficeHash, "", false, false);

            loadMetadataFormat(MetadataFormat.JSON_DC, "Schema for Dublin Core elements in JSON format", "", true, true);
            
            LOGGER.log(Level.INFO, "reimport process types");
            runtime.importProcessTypes();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "error during bootstrap", e);
            throw new BootstrapServiceException("error during bootstrap", e);
        }

    }
    
    protected void loadMetadataFormat(String mf, String desc, String form, boolean validationNeed, boolean indexable) throws CoreServiceException, DataCollisionException {
    	InputStream schemaInputStream = getClass().getClassLoader().getResourceAsStream("schema/" + mf + ".json");
        String schemaHash = core.put(schemaInputStream);
        core.createMetadataFormat(mf, desc, 
        		schemaHash, form, validationNeed, indexable);
    }

    @Override
    public String getServiceName() {
        return BootstrapService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

}
