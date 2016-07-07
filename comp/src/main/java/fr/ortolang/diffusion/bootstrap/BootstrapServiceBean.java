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
import java.io.IOException;
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
import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import fr.ortolang.diffusion.core.*;
import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.WorkspaceType;
import fr.ortolang.diffusion.form.FormService;
import fr.ortolang.diffusion.form.FormServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.ProfileAlreadyExistsException;
import fr.ortolang.diffusion.membership.entity.ProfileStatus;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.RuntimeService;
import fr.ortolang.diffusion.runtime.RuntimeServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.security.authorisation.entity.AuthorisationPolicyTemplate;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.json.JsonStoreService;

@Startup
@Singleton(name = BootstrapService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
@RunAs("user")
@RunAsPrincipal(value = MembershipService.SUPERUSER_IDENTIFIER)
public class BootstrapServiceBean implements BootstrapService {

    private static final Logger LOGGER = Logger.getLogger(BootstrapServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { };
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] { };

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
    @EJB
    private JsonStoreService jsonStore;
    @Resource
    private SessionContext ctx;

    public BootstrapServiceBean() {
        LOGGER.log(Level.FINE, "new bootstrap service instance created");
    }

    @PostConstruct
    public void init() {
        try {
            OrtolangConfig.getInstance();
            bootstrap();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "error during bootstrap");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void bootstrap() throws BootstrapServiceException {
        LOGGER.log(Level.INFO, "checking bootstrap status...");
        try {
            registry.lookup(BootstrapService.WORKSPACE_KEY);
            LOGGER.log(Level.INFO, "bootstrap key found, skipping initial creation.");
        } catch (KeyNotFoundException e) {
            try {
                LOGGER.log(Level.INFO, "bootstrap key not found, creating  platform initial content...");

                Map<String, List<String>> anonReadRules = new HashMap<String, List<String>>();
                anonReadRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Collections.singletonList("read"));

                LOGGER.log(Level.FINE, "creating root profile");
                membership.createProfile(MembershipService.SUPERUSER_IDENTIFIER, "Super", "User", "root@ortolang.org", ProfileStatus.ACTIVE);
                membership.addProfilePublicKey(MembershipService.SUPERUSER_IDENTIFIER, "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDqv8kWdXIgWbFJfOiu9fQYiazwfnpZogatgo3278PIAQ4eaj6p+sjQMQX0hew+rHXCqvi6FaG6Lql7fkJv/NJpSyhyCKYNCxmYKwUvaViOuRLDmnpziEX39WDmiWBE0Q+DNuKIZMx3yZNX/BeBp0FfooKkCnZWEDo4pzcYVp2RlwZuEDZQcQ6KP2S9+z2WQPmsE9tcyPNL12hp8tiG8J/XsPXxnn1mgJxyiwQmEYDxXZTAazeewqftz4GU3Xc9qWOa4GXK/2l0GB/XVuFLoyrXve+hnsiFpeIslJuGl0+AAX+lCULjDcA72r4aT30Z4HV+wxiQxk/j+2CtCw/vfeit achile-laptop");

                LOGGER.log(Level.FINE, "creating anonymous profile");
                membership.createProfile(MembershipService.UNAUTHENTIFIED_IDENTIFIER, "Anonymous", "User", "anon@ortolang.org", ProfileStatus.ACTIVE);
                LOGGER.log(Level.FINE, "change owner of anonymous profile to root and set anon read rules");
                authorisation.updatePolicyOwner(MembershipService.UNAUTHENTIFIED_IDENTIFIER, MembershipService.SUPERUSER_IDENTIFIER);
                authorisation.setPolicyRules(MembershipService.UNAUTHENTIFIED_IDENTIFIER, anonReadRules);

                LOGGER.log(Level.FINE, "creating moderators group");
                membership.createGroup(MembershipService.MODERATOR_GROUP_KEY, "Publication Moderators", "Moderators of the platform can publish content");
                membership.addMemberInGroup(MembershipService.MODERATOR_GROUP_KEY, MembershipService.SUPERUSER_IDENTIFIER);
                authorisation.setPolicyRules(MembershipService.MODERATOR_GROUP_KEY, anonReadRules);

                LOGGER.log(Level.FINE, "creating esr group");
                membership.createGroup(MembershipService.ESR_GROUP_KEY, "ESR Members", "People from Superior Teaching and Research Group");
                authorisation.setPolicyRules(MembershipService.ESR_GROUP_KEY, anonReadRules);
                
                LOGGER.log(Level.FINE, "creating admins group");
                membership.createGroup(MembershipService.ADMIN_GROUP_KEY, "Administrators", "Administrators of the platform");
                authorisation.setPolicyRules(MembershipService.ADMIN_GROUP_KEY, anonReadRules);

                LOGGER.log(Level.FINE, "create system workspace");
                core.createWorkspace(BootstrapService.WORKSPACE_KEY, "system", "System Workspace", WorkspaceType.SYSTEM.toString());
                Properties props = new Properties();
                props.setProperty("bootstrap.status", "done");
                props.setProperty("bootstrap.timestamp", System.currentTimeMillis() + "");
                props.setProperty("bootstrap.version", BootstrapService.VERSION);
                String hash = core.put(new ByteArrayInputStream(props.toString().getBytes()));
                core.createDataObject(BootstrapService.WORKSPACE_KEY, "/bootstrap.txt", hash);
                LOGGER.log(Level.FINE, "create [" + AuthorisationPolicyTemplate.FORALL + "] authorisation policy template");
                String forallPolicyKey = UUID.randomUUID().toString();
                authorisation.createPolicy(forallPolicyKey, MembershipService.SUPERUSER_IDENTIFIER);
                Map<String, List<String>> forallPolicyRules = new HashMap<String, List<String>>();
                forallPolicyRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList("read", "download"));
                authorisation.setPolicyRules(forallPolicyKey, forallPolicyRules);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.FORALL, "All users can read and download this content", forallPolicyKey);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.DEFAULT, "Default template allows all users to read and download content", forallPolicyKey);

                LOGGER.log(Level.FINE, "create [" + AuthorisationPolicyTemplate.AUTHENTIFIED + "] authorisation policy template");
                String authentifiedPolicyKey = UUID.randomUUID().toString();
                authorisation.createPolicy(authentifiedPolicyKey, MembershipService.SUPERUSER_IDENTIFIER);
                Map<String, List<String>> authentifiedPolicyRules = new HashMap<String, List<String>>();
                authentifiedPolicyRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Collections.singletonList("read"));
                authentifiedPolicyRules.put(MembershipService.ALL_AUTHENTIFIED_GROUP_KEY, Arrays.asList("read", "download"));
                authorisation.setPolicyRules(authentifiedPolicyKey, authentifiedPolicyRules);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.AUTHENTIFIED, "All users can read this content but download is restricted to authentified users only", authentifiedPolicyKey);

                LOGGER.log(Level.FINE, "create [" + AuthorisationPolicyTemplate.ESR + "] authorisation policy template");
                String esrPolicyKey = UUID.randomUUID().toString();
                authorisation.createPolicy(esrPolicyKey, MembershipService.SUPERUSER_IDENTIFIER);
                Map<String, List<String>> esrPolicyRules = new HashMap<String, List<String>>();
                esrPolicyRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Collections.singletonList("read"));
                esrPolicyRules.put(MembershipService.ESR_GROUP_KEY, Arrays.asList("read", "download"));
                authorisation.setPolicyRules(esrPolicyKey, esrPolicyRules);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.ESR, "All users can read this content but download is restricted to ESR users only", esrPolicyKey);

                LOGGER.log(Level.FINE, "create [" + AuthorisationPolicyTemplate.RESTRICTED + "] authorisation policy template");
                String restrictedPolicyKey = UUID.randomUUID().toString();
                authorisation.createPolicy(restrictedPolicyKey, MembershipService.SUPERUSER_IDENTIFIER);
                Map<String, List<String>> restrictedPolicyRules = new HashMap<String, List<String>>();
                restrictedPolicyRules.put("${workspace.members}", Arrays.asList("read", "download"));
                authorisation.setPolicyRules(restrictedPolicyKey, restrictedPolicyRules);
                authorisation.createPolicyTemplate(AuthorisationPolicyTemplate.RESTRICTED, "Only workspace members can read and download this content, all other users cannot see this content", restrictedPolicyKey);

                LOGGER.log(Level.FINE, "import forms");
                LOGGER.log(Level.FINE, "import form : test-process-start-form");
                InputStream is = getClass().getClassLoader().getResourceAsStream("forms/test-process-start-form.json");
                String jsonDefinition = IOUtils.toString(is, "UTF-8");
                form.createForm("test-process-start-form", "Test Process Start Form", jsonDefinition);
                LOGGER.log(Level.FINE, "import form : test-process-confirm-form");
                InputStream is2 = getClass().getClassLoader().getResourceAsStream("forms/test-process-confirm-form.json");
                String jsonDefinition2 = IOUtils.toString(is2, "UTF-8");
                form.createForm("test-process-confirm-form", "Test Process Confirm Form", jsonDefinition2);
                LOGGER.log(Level.FINE, "import form : import-zip-form");
                InputStream is3 = getClass().getClassLoader().getResourceAsStream("forms/import-zip-process-start-form.json");
                String jsonDefinition3 = IOUtils.toString(is3, "UTF-8");
                form.createForm("import-zip-process-start-form", "Import Zip Process Start Form", jsonDefinition3);
                LOGGER.log(Level.FINE, "import form : review-snapshot-form");
                InputStream is4 = getClass().getClassLoader().getResourceAsStream("forms/review-snapshot-form.json");
                String jsonDefinition4 = IOUtils.toString(is4, "UTF-8");
                form.createForm("review-snapshot-form", "Review Snapshot Form", jsonDefinition4);
                LOGGER.log(Level.FINE, "import form : test-process-confirm-form");
                InputStream is5 = getClass().getClassLoader().getResourceAsStream("forms/ortolang-item-form.json");
                String jsonDefinition5 = IOUtils.toString(is5, "UTF-8");
                form.createForm("ortolang-item-form", "Schema Form for an ORTOLANG item", jsonDefinition5);

                LOGGER.log(Level.INFO, "bootstrap done.");
            } catch (MembershipServiceException | ProfileAlreadyExistsException | AuthorisationServiceException | CoreServiceException | KeyAlreadyExistsException | IOException | AliasAlreadyExistsException
                    | AccessDeniedException | KeyNotFoundException | InvalidPathException | DataCollisionException | FormServiceException | PathNotFoundException | PathAlreadyExistsException | WorkspaceReadOnlyException e1) {
                LOGGER.log(Level.SEVERE, "unexpected error occurred while bootstrapping platform", e1);
                throw new BootstrapServiceException("unable to bootstrap platform", e1);
            }

        } catch (RegistryServiceException e) {
            throw new BootstrapServiceException("unable to check platform bootstrap status", e);
        }

        try {
            LOGGER.log(Level.INFO, "import process types");
            runtime.importProcessTypes();
            
            LOGGER.log(Level.FINE, "import metadataformat schemas");
            InputStream schemaItemInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-item-schema-0.15-with-object-language-copy.json");
            String schemaItemHash = core.put(schemaItemInputStream);
            core.createMetadataFormat(MetadataFormat.ITEM, "Les métadonnées de présentation permettent de paramétrer l\'affichage de la ressource dans la partie consultation du site.", schemaItemHash, "ortolang-item-form", true, true);

            InputStream schemaInputStream2 = getClass().getClassLoader().getResourceAsStream("schema/ortolang-acl-schema.json");
            String schemaHash2 = core.put(schemaInputStream2);
            core.createMetadataFormat(MetadataFormat.ACL, "Les métadonnées de contrôle d'accès permettent de paramétrer la visibilité d'une ressource lors de sa publication.", schemaHash2, "", true, false);

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
        } catch (RuntimeServiceException | CoreServiceException | DataCollisionException e1) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while importing process types and metadataformat schemas", e1);
        }
    }

    @Override
    public String getServiceName() {
        return BootstrapService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        //TODO parse the bootstrap file to read status and infos.
        return Collections.emptyMap();
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        return OBJECT_PERMISSIONS_LIST;
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }
    
    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }

}
