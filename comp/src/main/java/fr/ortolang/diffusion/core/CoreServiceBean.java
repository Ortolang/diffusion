package fr.ortolang.diffusion.core;

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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.io.IOUtils;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.jboss.ejb3.annotation.SecurityDomain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangEvent.ArgumentsBuilder;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectExportHandler;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectImportHandler;
import fr.ortolang.diffusion.OrtolangObjectPid;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangObjectState.Status;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.CollectionElement;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataElement;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.MetadataSource;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.TagElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.core.entity.WorkspaceAlias;
import fr.ortolang.diffusion.core.entity.WorkspaceType;
import fr.ortolang.diffusion.core.export.CollectionExportHandler;
import fr.ortolang.diffusion.core.export.DataObjectExportHandler;
import fr.ortolang.diffusion.core.export.LinkExportHandler;
import fr.ortolang.diffusion.core.export.MetadataObjectExportHandler;
import fr.ortolang.diffusion.core.export.WorkspaceExportHandler;
import fr.ortolang.diffusion.core.wrapper.CollectionWrapper;
import fr.ortolang.diffusion.core.wrapper.OrtolangObjectWrapper;
import fr.ortolang.diffusion.event.EventService;
import fr.ortolang.diffusion.extraction.ExtractionService;
import fr.ortolang.diffusion.extraction.ExtractionServiceException;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.SecurityService;
import fr.ortolang.diffusion.security.SecurityServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.security.authorisation.entity.AuthorisationPolicyTemplate;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;
import fr.ortolang.diffusion.store.json.OrtolangKeyExtractor;

@Local(CoreService.class)
@Stateless(name = CoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class CoreServiceBean implements CoreService {

    private static final Logger LOGGER = Logger.getLogger(CoreServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { Workspace.OBJECT_TYPE, DataObject.OBJECT_TYPE, Collection.OBJECT_TYPE, Link.OBJECT_TYPE, MetadataObject.OBJECT_TYPE };
    private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { { Workspace.OBJECT_TYPE, "read,update,delete,snapshot" }, { DataObject.OBJECT_TYPE, "read,update,delete,download" },
            { Collection.OBJECT_TYPE, "read,update,delete,download" }, { Link.OBJECT_TYPE, "read,update,delete" }, { MetadataObject.OBJECT_TYPE, "read,update,delete,download" } };

    private static final String[] RESERVED_ALIASES = new String[] { "key", "auth", "export" };
    private static final String[] RESERVED_TAG_NAMES = new String[] { Workspace.HEAD, Workspace.LATEST };

    @EJB
    private RegistryService registry;
    @EJB
    private BinaryStoreService binarystore;
    @EJB
    private MembershipService membership;
    @EJB
    private EventService events;
    @EJB
    private AuthorisationService authorisation;
    @EJB
    private IndexingService indexing;
    @EJB
    private NotificationService notification;
    @EJB
    private ExtractionService extraction;
    @EJB
    private SecurityService security;
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @Resource
    private SessionContext ctx;

    public CoreServiceBean() {
    }

    public RegistryService getRegistryService() {
        return registry;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registry = registryService;
    }

    public BinaryStoreService getBinaryStoreService() {
        return binarystore;
    }

    public void setBinaryStoreService(BinaryStoreService binaryStoreService) {
        this.binarystore = binaryStoreService;
    }

    public NotificationService getNotificationService() {
        return notification;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notification = notificationService;
    }

    public MembershipService getMembershipService() {
        return membership;
    }

    public void setMembershipService(MembershipService membership) {
        this.membership = membership;
    }

    public AuthorisationService getAuthorisationService() {
        return authorisation;
    }

    public void setAuthorisationService(AuthorisationService authorisation) {
        this.authorisation = authorisation;
    }

    public IndexingService getIndexingService() {
        return indexing;
    }

    public void setIndexingService(IndexingService indexing) {
        this.indexing = indexing;
    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public EntityManager getEntityManager() {
        return this.em;
    }

    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }

    public SessionContext getSessionContext() {
        return this.ctx;
    }

    /* Workspace */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Workspace createWorkspace(String wskey, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, AliasAlreadyExistsException {
        WorkspaceAlias alias = new WorkspaceAlias();
        em.persist(alias);
        return createWorkspace(wskey, alias.getValue(), name, type);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Workspace createWorkspace(String wskey, String alias, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, AliasAlreadyExistsException {
        LOGGER.log(Level.FINE, "creating workspace [" + wskey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);

            String members = UUID.randomUUID().toString();
            membership.createGroup(members, name + "'s Members", "Members of a workspace have all permissions on workspace content");
            membership.addMemberInGroup(members, caller);
            Map<String, List<String>> membersrules = authorisation.getPolicyRules(members);
            membersrules.put(MembershipService.MODERATORS_GROUP_KEY, Arrays.asList("read", "update"));
            membersrules.put(MembershipService.PUBLISHERS_GROUP_KEY, Arrays.asList("read"));
            membersrules.put(MembershipService.REVIEWERS_GROUP_KEY, Arrays.asList("read"));
            authorisation.setPolicyRules(members, membersrules);

            String head = UUID.randomUUID().toString();
            Collection collection = new Collection();
            collection.setId(UUID.randomUUID().toString());
            collection.setName("root");
            collection.setRoot(true);
            collection.setClock(1);
            em.persist(collection);

            registry.register(head, collection.getObjectIdentifier(), caller);
            indexing.index(head);

            Map<String, List<String>> rules = new HashMap<String, List<String>>();
            rules.put(members, Arrays.asList("read", "create", "update", "delete", "download"));
            rules.put(MembershipService.MODERATORS_GROUP_KEY, Arrays.asList("read", "update", "download"));
            rules.put(MembershipService.PUBLISHERS_GROUP_KEY, Arrays.asList("read", "download"));
            rules.put(MembershipService.REVIEWERS_GROUP_KEY, Arrays.asList("read", "download"));
            authorisation.createPolicy(head, members);
            authorisation.setPolicyRules(head, rules);

            if (Arrays.asList(RESERVED_ALIASES).contains(alias)) {
                throw new CoreServiceException(alias + " is reserved and cannot be used as an alias");
            }

            List<Workspace> results = em.createNamedQuery("findWorkspaceByAlias", Workspace.class).setParameter("alias", alias).getResultList();
            if (!results.isEmpty()) {
                ctx.setRollbackOnly();
                throw new AliasAlreadyExistsException("a workspace with alias [" + alias + "] already exists in storage");
            }
            PathBuilder palias;
            try {
                palias = PathBuilder.fromPath(alias);
                if (palias.isRoot() || palias.depth() > 1) {
                    throw new InvalidPathException("incorrect depth for an alias path");
                }
            } catch (InvalidPathException e) {
                LOGGER.log(Level.SEVERE, "invalid alias for workspace", e);
                throw new CoreServiceException("alias is invalid", e);
            }
            String id = UUID.randomUUID().toString();
            Workspace workspace = new Workspace();
            workspace.setId(id);
            workspace.setKey(wskey);
            workspace.setAlias(palias.part());
            workspace.setName(name);
            workspace.setType(type);
            workspace.setHead(head);
            workspace.setChanged(true);
            workspace.setMembers(members);
            em.persist(workspace);

            registry.register(wskey, workspace.getObjectIdentifier(), caller);

            Map<String, List<String>> wsrules = new HashMap<String, List<String>>();
            wsrules.put(members, Collections.singletonList("read"));
            wsrules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Collections.singletonList("read"));
            wsrules.put(MembershipService.MODERATORS_GROUP_KEY, Arrays.asList("read", "create", "update", "delete"));
            wsrules.put(MembershipService.PUBLISHERS_GROUP_KEY, Arrays.asList("read"));
            wsrules.put(MembershipService.REVIEWERS_GROUP_KEY, Arrays.asList("read"));
            authorisation.createPolicy(wskey, caller);
            authorisation.setPolicyRules(wskey, wsrules);

            indexing.index(wskey);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(1).addArgument("ws-alias", alias);
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "create"), argsBuilder.build());
            argsBuilder.addArgument("key", head).addArgument("path", "/");
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"), argsBuilder.build());

            return workspace;
        } catch (KeyAlreadyExistsException e) {
            ctx.setRollbackOnly();
            throw e;
        } catch (KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException
                | IndexingServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while creating workspace", e);
            throw new CoreServiceException("unable to create workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Workspace readWorkspace(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "reading workspace [" + wskey + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "read");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            workspace.setKey(wskey);
            return workspace;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while reading workspace", e);
            throw new CoreServiceException("unable to read workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> findWorkspacesForProfile(String profile) throws CoreServiceException, AccessDeniedException {
        LOGGER.log(Level.FINE, "finding workspaces for profile");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);

            List<String> groups = membership.getProfileGroups(profile);
            return findWorkspacesForGroups(groups);
        } catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException | KeyNotFoundException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during finding workspaces for profile", e);
            throw new CoreServiceException("unable to find workspaces for profile", e);
        }
    }

    @Override
    @RolesAllowed("system")
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> systemFindWorkspacesForProfile(String profile) throws CoreServiceException {
        LOGGER.log(Level.FINE, "#SYSTEM# finding workspaces for profile");
        try {
            List<String> groups = Arrays.asList(membership.systemReadProfile(profile).getGroups());
            return findWorkspacesForGroups(groups);
        } catch (MembershipServiceException | RegistryServiceException | KeyNotFoundException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during finding workspaces for profile", e);
            throw new CoreServiceException("unable to find workspaces for profile", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private List<String> findWorkspacesForGroups(List<String> groups) throws RegistryServiceException {
        if (groups.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> keys = new ArrayList<>();
        TypedQuery<Workspace> query = em.createNamedQuery("findWorkspaceByMember", Workspace.class).setParameter("groups", groups);
        List<Workspace> workspaces = query.getResultList();
        for (Workspace workspace : workspaces) {
            OrtolangObjectIdentifier identifier = workspace.getObjectIdentifier();
            try {
                keys.add(registry.lookup(identifier));
            } catch (IdentifierNotRegisteredException e) {
                LOGGER.log(Level.SEVERE, "a workspace with an unregistered identifier has be found : " + identifier);
            }
        }
        return keys;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> findWorkspacesAliasForProfile(String profile) throws CoreServiceException, AccessDeniedException {
        LOGGER.log(Level.FINE, "finding workspace alias for profile");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);

            List<String> groups = membership.getProfileGroups(profile);
            if (groups.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> aliases = new ArrayList<String>();
            TypedQuery<Workspace> query = em.createNamedQuery("findWorkspaceByMember", Workspace.class).setParameter("groups", groups);
            List<Workspace> workspaces = query.getResultList();
            for (Workspace workspace : workspaces) {
                OrtolangObjectIdentifier identifier = workspace.getObjectIdentifier();
                try {
                    if (workspace.getAlias() != null && workspace.getAlias().length() > 0) {
                        registry.lookup(identifier);
                        aliases.add(workspace.getAlias());
                    }
                } catch (IdentifierNotRegisteredException e) {
                    LOGGER.log(Level.SEVERE, "a workspace with an unregistered identifier has be found : " + identifier);
                }
            }

            return aliases;
        } catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException | KeyNotFoundException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during finding workspaces for profile", e);
            throw new CoreServiceException("unable to find workspaces for profile", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> listAllWorkspaceAlias() throws CoreServiceException, AccessDeniedException {
        LOGGER.log(Level.FINE, "listing all workspaces alias");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            authorisation.checkSuperUser(caller);
            TypedQuery<String> query = em.createNamedQuery("listAllWorkspaceAlias", String.class);
            return query.getResultList();
        } catch (AuthorisationServiceException e) {
            LOGGER.log(Level.SEVERE, "unable to list all workspace aliases", e);
            throw new CoreServiceException("unable to list all workspace aliases", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String snapshotWorkspace(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, WorkspaceReadOnlyException, WorkspaceUnchangedException {
        LOGGER.log(Level.FINE, "snapshoting workspace [" + wskey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "update");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, workspace)) {
                throw new WorkspaceReadOnlyException("unable to snapshot workspace with key [" + wskey + "] because it is read only");
            }

            String name = String.valueOf(workspace.getClock());

            if (!workspace.hasChanged()) {
                throw new WorkspaceUnchangedException("unable to snapshot because workspace has no pending modifications since last snapshot");
            }

            try {
                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("wskey", wskey);
                builder.add("snapshotName", name);
                builder.add("wsalias", workspace.getAlias());

                JsonObject jsonObject = builder.build();
                String hash = binarystore.put(new ByteArrayInputStream(jsonObject.toString().getBytes()));

                List<String> mds = findMetadataObjectsForTargetAndName(workspace.getHead(), MetadataFormat.WORKSPACE);

                if (mds.isEmpty()) {
                    LOGGER.log(Level.INFO, "creating workspace metadata for root collection");
                    createMetadataObject(wskey, "/", MetadataFormat.WORKSPACE, hash, null, false);
                } else {
                    LOGGER.log(Level.INFO, "updating workspace metadata for root collection");
                    updateMetadataObject(wskey, "/", MetadataFormat.WORKSPACE, hash, null, false);
                }
            } catch (BinaryStoreServiceException | DataCollisionException | CoreServiceException | KeyNotFoundException | InvalidPathException | AccessDeniedException | MetadataFormatException
                    | PathNotFoundException | KeyAlreadyExistsException e) {
                throw new CoreServiceException("cannot create workspace metadata for collection root : " + e.getMessage());
            }

            workspace.setKey(wskey);
            workspace.incrementClock();

            OrtolangObjectIdentifier hidentifier = registry.lookup(workspace.getHead());
            checkObjectType(hidentifier, Collection.OBJECT_TYPE);
            Collection collection = em.find(Collection.class, hidentifier.getId());
            if (collection == null) {
                throw new CoreServiceException("unable to load head collection with id [" + hidentifier.getId() + "] from storage");
            }
            collection.setKey(workspace.getHead());

            Collection clone = cloneCollection(workspace.getHead(), collection, workspace.getClock());

            workspace.addSnapshot(new SnapshotElement(name, collection.getKey()));
            workspace.setHead(clone.getKey());
            workspace.setChanged(false);
            em.merge(workspace);

            registry.update(wskey);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("ws-alias", workspace.getAlias()).addArgument("snapshot-name", name);
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "snapshot"), argsBuilder.build());
            return name;
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | CloneException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while snapshoting workspace", e);
            throw new CoreServiceException("unable to snapshot workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getLatestSnapshot(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "read");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }

            return String.valueOf(workspace.getClock() - 1);
        } catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while getting latest workspace snapshot", e);
            throw new CoreServiceException("unable to get latest snapshot for workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void systemTagWorkspace(String wskey, String tag, String snapshot) throws CoreServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "#SYSTEM# tagging workspace [" + wskey + "] and snapshot [" + snapshot + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            workspace.setKey(wskey);

            try {
                PathBuilder pname = PathBuilder.newInstance().path(tag);
                if (pname.depth() > 1) {
                    throw new CoreServiceException("tag name is invalid: " + tag);
                }
                if (Arrays.asList(RESERVED_TAG_NAMES).contains(pname.part())) {
                    throw new CoreServiceException(pname.part() + " is reserved and cannot be used as tag name");
                }
                tag = pname.part();
                TagElement tagElement = workspace.findTagByName(tag);
                if (tagElement != null) {
                    workspace.removeTag(tagElement);
                    String oldRoot = workspace.findSnapshotByName(tagElement.getSnapshot()).getKey();
                    indexing.remove(oldRoot);
                }
                if (!workspace.containsSnapshotName(snapshot)) {
                    throw new CoreServiceException("the snapshot with name '" + snapshot + "' does not exists in this workspace");
                }

                workspace.addTag(new TagElement(tag, snapshot));
                em.merge(workspace);
                registry.update(wskey);
                indexing.index(wskey);

                ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("ws-alias", workspace.getAlias()).addArgument("tag-name", tag);
                notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "tag"), argsBuilder.build());

            } catch (InvalidPathException e) {
                throw new CoreServiceException("tag name is invalid: " + tag);
            }

        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while tagging workspace snapshot", e);
            throw new CoreServiceException("unable to tag workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateWorkspace(String wskey, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "updating workspace [" + wskey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "update");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, workspace)) {
                throw new WorkspaceReadOnlyException("unable to update workspace with key [" + wskey + "] because it is read only");
            }
            workspace.setName(name);
            em.merge(workspace);

            registry.update(wskey);
            indexing.index(wskey);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("ws-alias", workspace.getAlias()).addArgument("name", name);
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "update"), argsBuilder.build());
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while updating workspace", e);
            throw new CoreServiceException("unable to update workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Workspace archiveWorkspace(String wskey, Boolean archive) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "archiving workspace [" + wskey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            if (!MembershipService.SUPERUSER_IDENTIFIER.equals(caller)) {
                throw new CoreServiceException("only " + MembershipService.SUPERUSER_IDENTIFIER + " can archive workspace");
            }

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "update");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            workspace.setArchive(archive);
            workspace.setKey(wskey);
            em.merge(workspace);

            registry.update(wskey);
            indexing.index(wskey);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("ws-alias", workspace.getAlias()).addArgument("archive", archive.toString());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "archive"), argsBuilder.build());

            return workspace;
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while archiving workspace", e);
            throw new CoreServiceException("unable to archive workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteWorkspace(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, WorkspaceReadOnlyException {
        deleteWorkspace(wskey, false);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteWorkspace(String wskey, boolean force) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "deleting workspace [" + wskey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            if (force && !MembershipService.SUPERUSER_IDENTIFIER.equals(caller)) {
                throw new CoreServiceException("only " + MembershipService.SUPERUSER_IDENTIFIER + " can force workspace delete");
            }

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "delete");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            if (!force) {
                if (applyReadOnly(caller, subjects, workspace)) {
                    throw new WorkspaceReadOnlyException("unable to delete workspace with key [" + wskey + "] because it is read only");
                }
                if (workspace.getType().equals(WorkspaceType.SYSTEM.name())) {
                    throw new CoreServiceException("unable to delete with key [" + wskey + "] because it is of type: " + WorkspaceType.SYSTEM.name());
                }
                String current = workspace.getHead();
                while (current != null) {
                    String parent = registry.getParent(current);
                    if (parent != null && registry.getPublicationStatus(parent).equals(OrtolangObjectState.Status.PUBLISHED.value())) {
                        throw new CoreServiceException("unable to delete workspace with key [" + wskey + "] because it has a published version");
                    }
                    current = parent;
                }
            }
            workspace.setAlias(null);
            em.merge(workspace);

            membership.deleteGroup(workspace.getMembers());
            registry.delete(wskey);
            indexing.remove(wskey);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(1).addArgument("ws-alias", workspace.getAlias());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "delete"), argsBuilder.build());
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while deleting workspace", e);
            throw new CoreServiceException("unable to delete workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void changeWorkspaceOwner(String wskey, String newOwner) throws CoreServiceException {
        LOGGER.log(Level.FINE, "changing workspace [" + wskey + "] owner to: " + newOwner);
        try {
            Workspace workspace = readWorkspace(wskey);
            security.changeOwner(wskey, newOwner);
            security.changeOwner(workspace.getMembers(), newOwner);
            membership.addMemberInGroup(workspace.getMembers(), newOwner);
        } catch (SecurityServiceException | AccessDeniedException | KeyNotFoundException | MembershipServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while changing workspace owner", e);
            throw new CoreServiceException("unable to change owner of workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void notifyWorkspaceOwner(String wskey, String email, String message) throws CoreServiceException {
        LOGGER.log(Level.FINE, "notify owner of workspace [" + wskey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("email", email).addArgument("message", message);
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "notify-owner"), argsBuilder.build());
        } catch (RegistryServiceException | KeyNotFoundException | NotificationServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while trying to notify workspace owner", e);
            throw new CoreServiceException("unable to notify owner of workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void notifyWorkspaceMembers(String wskey, String email, String message) throws CoreServiceException {
        LOGGER.log(Level.FINE, "notify members of workspace [" + wskey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(2).addArgument("email", email).addArgument("message", message);
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "notify-members"), argsBuilder.build());
        } catch (RegistryServiceException | KeyNotFoundException | NotificationServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while trying to notify workspace members", e);
            throw new CoreServiceException("unable to notify members of workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String resolveWorkspaceAlias(String alias) throws CoreServiceException, AliasNotFoundException {
        LOGGER.log(Level.FINE, "finding workspace for alias: " + alias);
        try {
            TypedQuery<Workspace> query = em.createNamedQuery("findWorkspaceByAlias", Workspace.class).setParameter("alias", alias);
            try {
                Workspace workspace = query.getSingleResult();
                return registry.lookup(workspace.getObjectIdentifier());
            } catch (NoResultException e) {
                throw new AliasNotFoundException(alias);
            }
        } catch (RegistryServiceException | IdentifierNotRegisteredException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during resolving workspace alias: " + alias, e);
            throw new CoreServiceException("unable to resolve workspace alias: " + alias, e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String resolveWorkspacePath(String wskey, String root, String path) throws CoreServiceException, InvalidPathException, PathNotFoundException {
        LOGGER.log(Level.FINE, "resolving into workspace [" + wskey + "] and root [" + root + "] path [" + path + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            PathBuilder ppath = npath.clone().parent();

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            String rroot = ws.getHead();
            if (root != null && root.length() > 0 && !root.equals(Workspace.HEAD)) {
                String snapshot = root;
                TagElement tag = ws.findTagByName(root);
                if (tag != null) {
                    LOGGER.log(Level.FINEST, "root is a tag, resolving tag snapshot");
                    snapshot = tag.getSnapshot();
                }
                SnapshotElement element = ws.findSnapshotByName(snapshot);
                if (element == null) {
                    throw new RootNotFoundException(root);
                } else {
                    rroot = element.getKey();
                }
            }

            if (npath.isRoot()) {
                return rroot;
            }

            Collection parent = readCollectionAtPath(rroot, ppath);
            LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

            CollectionElement element = parent.findElementByName(npath.part());
            if (element == null) {
                throw new PathNotFoundException(npath.build());
            }
            return element.getKey();
        } catch (KeyNotFoundException | RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during resolving path", e);
            throw new CoreServiceException("unable to resolve into workspace [" + wskey + "] path [" + path + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String resolveWorkspaceMetadata(String wskey, String root, String path, String name) throws CoreServiceException, InvalidPathException, AccessDeniedException, PathNotFoundException {
        LOGGER.log(Level.FINE, "resolving into workspace [" + wskey + "] and root [" + root + "] metadata with name [" + name + "] at path [" + path + "]");
        try {
            String key = resolveWorkspacePath(wskey, root, path);
            MetadataElement cmdelement = loadMetadataElement(name, key);
            if (cmdelement == null) {
                throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for at path [" + path + "]");
            }
            return cmdelement.getKey();
        } catch (KeyNotFoundException | RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during resolving metadata", e);
            throw new CoreServiceException("unable to resolve into workspace [" + wskey + "] metadata name [" + name + "] at [" + path + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Set<String> buildWorkspaceReviewList(String wskey, String snapshot) throws CoreServiceException, AccessDeniedException {
        LOGGER.log(Level.FINE, "building review list for workspace [" + wskey + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "read");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            if (!workspace.containsSnapshotName(snapshot)) {
                throw new CoreServiceException("the workspace with key: " + wskey + " does not containt a snapshot with name: " + snapshot);
            }
            String root = workspace.findSnapshotByName(snapshot).getKey();

            Set<String> keys = new HashSet<String>();
            systemListCollectionKeys(root, keys);
            return keys;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during building workspace review list", e);
            throw new CoreServiceException("unexpected error while trying to build workspace review list", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, Map<String, List<String>>> buildWorkspacePublicationMap(String wskey, String snapshot) throws CoreServiceException {
        LOGGER.log(Level.FINE, "building publication map for workspace [" + wskey + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "read");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            if (!workspace.containsSnapshotName(snapshot)) {
                throw new CoreServiceException("the workspace with key: " + wskey + " does not contain a snapshot with name: " + snapshot);
            }
            String root = workspace.findSnapshotByName(snapshot).getKey();

            Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();
            
            AuthorisationPolicyTemplate defaultTemplate = authorisation.getPolicyTemplate(AuthorisationPolicyTemplate.DEFAULT);
            Map<String, String> aclParams = new HashMap<String, String>();
            aclParams.put("${workspace.members}", workspace.getMembers());
            aclParams.put("${workspace.privileged}", workspace.getPrivileged());
            builtPublicationMap(root, map, authorisation.getPolicyRules(defaultTemplate.getTemplate()), aclParams);
            return map;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | OrtolangException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during building workspace publication map", e);
            throw new CoreServiceException("unexpected error while trying to build workspace publication map", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void builtPublicationMap(String key, Map<String, Map<String, List<String>>> map, Map<String, List<String>> current, Map<String, String> params) throws KeyNotFoundException,
            CoreServiceException, OrtolangException {
        Object object = readObject(key);
        if (object instanceof MetadataSource) {
            MetadataElement mde = ((MetadataSource) object).findMetadataByName(MetadataFormat.ACL);
            if (mde != null) {
                LOGGER.log(Level.FINE, "ACL metadata found, load json, find policy template and render it...");
                MetadataObject md = (MetadataObject) readObject(mde.getKey());
                try {
                    JsonReader reader = Json.createReader(binarystore.get(md.getStream()));
                    JsonObject json = reader.readObject();
                    String template = json.getString("template");
                    reader.close();
                    AuthorisationPolicyTemplate policy = authorisation.getPolicyTemplate(template);
                    Map<String, List<String>> rules = authorisation.getPolicyRules(policy.getTemplate());
                    Map<String, List<String>> filtered = new HashMap<String, List<String>>();
                    for (Entry<String, List<String>> entry : rules.entrySet()) {
                        if (params.containsKey(entry.getKey())) {
                            filtered.put(params.get(entry.getKey()), entry.getValue());
                        } else {
                            filtered.put(entry.getKey(), entry.getValue());
                        }
                    }
                    current = filtered;
                } catch (AuthorisationServiceException | BinaryStoreServiceException | DataNotFoundException e) {
                    LOGGER.log(Level.SEVERE, "unable to read acl metadata", e);
                }
            }
        }
        map.put(key, current);
        if (object instanceof MetadataSource) {
            for (MetadataElement element : ((MetadataSource) object).getMetadatas()) {
                map.put(element.getKey(), current);
            }
        }
        if (object instanceof Collection) {
            for (CollectionElement element : ((Collection) object).getElements()) {
                builtPublicationMap(element.getKey(), map, current, params);
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Set<OrtolangObjectPid> buildWorkspacePidList(String wskey, String tag) throws CoreServiceException {
        LOGGER.log(Level.FINE, "building pid list for workspace [" + wskey + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "read");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            if (!workspace.containsTagName(tag)) {
                throw new CoreServiceException("the workspace with key: " + wskey + " does not containt a tag with name: " + tag);
            }
            String snapshot = workspace.findTagByName(tag).getSnapshot();
            String root = workspace.findSnapshotByName(snapshot).getKey();

            Set<OrtolangObjectPid> pids = new HashSet<OrtolangObjectPid>();
            String apiUrlBase = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_SSL) + OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_PATH_CONTENT);
            String marketUrlBase = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.MARKET_SERVER_URL) + "market/item";
            buildHandleList(workspace.getAlias(), tag, root, pids, PathBuilder.newInstance(), apiUrlBase, marketUrlBase);
            return pids;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | OrtolangException | InvalidPathException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during building workspace pid list", e);
            throw new CoreServiceException("unexpected error while trying to build workspace pid list", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void buildHandleList(String wsalias, String tag, String key, Set<OrtolangObjectPid> pids, PathBuilder path, String apiUrlBase, String marketUrlBase) throws CoreServiceException,
            KeyNotFoundException, OrtolangException, InvalidPathException {
        try {
            OrtolangObject object = findObject(key);
            LOGGER.log(Level.FINE, "Generating pid for key: " + key);
            String target = ((path.isRoot()) ? marketUrlBase : apiUrlBase) + "/" + wsalias + "/" + tag + ((path.isRoot()) ? "" : path.build());
            String dynHandle = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX) + "/" + wsalias + ((path.isRoot()) ? "" : path.build());
            String staticHandle = OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX) + "/" + wsalias + "/" + tag + ((path.isRoot()) ? "" : path.build());
            OrtolangObjectPid dpid = new OrtolangObjectPid(OrtolangObjectPid.Type.HANDLE, dynHandle, key, target, false);
            boolean adddpid = true;
            for (OrtolangObjectPid pid : pids) {
                if (pid.getName().equals(dpid.getName()) && pid.isUserbased()) {
                    adddpid = false;
                    break;
                }
            }
            if (adddpid) {
                pids.add(dpid);
            }
            OrtolangObjectPid spid = new OrtolangObjectPid(OrtolangObjectPid.Type.HANDLE, staticHandle, key, target, false);
            boolean addspid = true;
            for (OrtolangObjectPid pid : pids) {
                if (pid.getName().equals(spid.getName()) && pid.isUserbased()) {
                    addspid = false;
                    break;
                }
            }
            if (addspid) {
                pids.add(spid);
            }
            if (object instanceof MetadataSource) {
                MetadataElement mde = ((MetadataSource) object).findMetadataByName(MetadataFormat.PID);
                if (mde != null) {
                    LOGGER.log(Level.FINE, "PID metadata found, load json and generate corresponding pids");
                    MetadataObject md = readMetadataObject(mde.getKey());
                    try {
                        JsonReader reader = Json.createReader(binarystore.get(md.getStream()));
                        JsonObject json = reader.readObject();
                        if (json.containsKey("pids")) {
                            JsonArray jpids = json.getJsonArray("pids");
                            for (int i = 0; i < jpids.size(); i++) {
                                JsonObject jpid = jpids.getJsonObject(i);
                                LOGGER.log(Level.FINE, "Generating metadata based pid for key: " + key);
                                String ctarget = ((path.isRoot()) ? marketUrlBase : apiUrlBase) + "/" + wsalias + "/" + tag + ((path.isRoot()) ? "" : path.build());
                                OrtolangObjectPid upid = new OrtolangObjectPid(OrtolangObjectPid.Type.HANDLE, jpid.getString("value"), key, ctarget, true);
                                Iterator<OrtolangObjectPid> iter = pids.iterator();
                                while (iter.hasNext()) {
                                    OrtolangObjectPid pid = iter.next();
                                    if (pid.getName().equals(upid.getName())) {
                                        iter.remove();
                                    }
                                }
                                pids.add(upid);
                            }
                        }
                        reader.close();
                    } catch (BinaryStoreServiceException | DataNotFoundException e) {
                        LOGGER.log(Level.SEVERE, "unable to read pid metadata", e);
                    }
                }
            }
            if (object instanceof Collection) {
                for (CollectionElement element : ((Collection) object).getElements()) {
                    buildHandleList(wsalias, tag, element.getKey(), pids, path.clone().path(element.getName()), apiUrlBase, marketUrlBase);
                }
            }
        } catch (AccessDeniedException e) {
            LOGGER.log(Level.INFO, "Unable to generate a PID for an object that has been set private.");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, String> listWorkspaceContent(String wskey, String snapshot) throws CoreServiceException {
        LOGGER.log(Level.FINE, "listing content of workspace [" + wskey + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "read");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            String root;
            if (Workspace.HEAD.equals(snapshot)) {
                root = workspace.getHead();
            } else {
                if (!workspace.containsSnapshotName(snapshot)) {
                    throw new CoreServiceException("the workspace with key: " + wskey + " does not containt a snapshot with name: " + snapshot);
                }
                root = workspace.findSnapshotByName(snapshot).getKey();
            }

            Map<String, String> map = new HashMap<String, String>();
            listContent(root, PathBuilder.newInstance(), map);
            return map;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | OrtolangException | InvalidPathException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during listing workspace content", e);
            throw new CoreServiceException("unexpected error while trying to list workspace content", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private void listContent(String key, PathBuilder path, Map<String, String> map) throws OrtolangException, InvalidPathException {
        Object object = findObject(key);
        map.put(path.build(), key);
        if (object instanceof Collection) {
            for (CollectionElement element : ((Collection) object).getElements()) {
                listContent(element.getKey(), path.clone().path(element.getName()), map);
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private CollectionWrapper getWorkspaceContentTree(String key, PathBuilder path, CollectionWrapper parent) throws KeyNotFoundException, InvalidPathException, RegistryServiceException {
        OrtolangObjectIdentifier identifier = registry.lookup(key);
        OrtolangObjectWrapper ortolangObjectWrapper;
        switch (identifier.getType()) {
        case Collection.OBJECT_TYPE:
            Collection collection = em.find(Collection.class, identifier.getId());
            ortolangObjectWrapper = OrtolangObjectWrapper.fromOrtolangObject(collection, path.build());
            for (CollectionElement element : collection.getElements()) {
                getWorkspaceContentTree(element.getKey(), path.clone().path(element.getName()), (CollectionWrapper) ortolangObjectWrapper);
            }
            if (parent != null) {
                parent.addChild(ortolangObjectWrapper);
            }
            return (CollectionWrapper) ortolangObjectWrapper;
        case DataObject.OBJECT_TYPE:
            DataObject dataObject = em.find(DataObject.class, identifier.getId());
            ortolangObjectWrapper = OrtolangObjectWrapper.fromOrtolangObject(dataObject, path.build());
            parent.addChild(ortolangObjectWrapper);
            break;
        case Link.OBJECT_TYPE:
            Link link = em.find(Link.class, identifier.getId());
            ortolangObjectWrapper = OrtolangObjectWrapper.fromOrtolangObject(link, path.build());
            parent.addChild(ortolangObjectWrapper);
            break;
        }
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Change> diffWorkspaceContent(String wskey, String lsnapshot, String rsnapshot) throws CoreServiceException {
        LOGGER.log(Level.FINE, "diff content of workspace [" + wskey + "] between snapshots [" + lsnapshot + "] and [" + rsnapshot + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            authorisation.checkPermission(wskey, subjects, "read");

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            String lroot;
            if (lsnapshot.equals(Workspace.HEAD)) {
                lroot = workspace.getHead();
            } else {
                if (!workspace.containsSnapshotName(lsnapshot)) {
                    throw new CoreServiceException("the workspace with key: " + wskey + " does not containt a snapshot with name: " + lsnapshot);
                }
                lroot = workspace.findSnapshotByName(lsnapshot).getKey();
            }
            String rroot;
            if (rsnapshot.equals(Workspace.HEAD)) {
                rroot = workspace.getHead();
            } else {
                if (!workspace.containsSnapshotName(rsnapshot)) {
                    throw new CoreServiceException("the workspace with key: " + wskey + " does not containt a snapshot with name: " + rsnapshot);
                }
                rroot = workspace.findSnapshotByName(rsnapshot).getKey();
            }

            CollectionWrapper lcontent = getWorkspaceContentTree(lroot, PathBuilder.newInstance(), null);
            CollectionWrapper rcontent = getWorkspaceContentTree(rroot, PathBuilder.newInstance(), null);

            Javers javers = JaversBuilder.javers().build();
            Diff diff = javers.compare(lcontent, rcontent);

            return diff.getChanges();

        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | OrtolangException | InvalidPathException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during diff workspace content", e);
            throw new CoreServiceException("unexpected error while trying to diff workspace content", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String findWorkspaceLatestPublishedSnapshot(String wskey) throws CoreServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "find workspace [" + wskey + "] latest published snapshot");
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            workspace.setKey(wskey);

            String current = workspace.getHead();
            boolean found = false;
            while (!found && current != null) {
                String parent = registry.getParent(current);
                if (parent != null && registry.getPublicationStatus(parent).equals(OrtolangObjectState.Status.PUBLISHED.value())) {
                    found = true;
                }
                current = parent;
            }

            if (current != null) {
                SnapshotElement snapshot = workspace.findSnapshotByKey(current);
                if (snapshot != null) {
                    return snapshot.getName();
                }
            }

            return null;
        } catch (RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while finding workspace latest published snapshot", e);
            throw new CoreServiceException("unable to find latest published snapshot for workspace with key [" + wskey + "]", e);
        }
    }

    /* Collections */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Collection createCollection(String wskey, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, PathNotFoundException,
            PathAlreadyExistsException, WorkspaceReadOnlyException, KeyAlreadyExistsException {
        String key = UUID.randomUUID().toString();
        try {
            return createCollection(wskey, key, path);
        } catch (KeyAlreadyExistsException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.WARNING, "the generated key already exists : " + key);
            throw e;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Collection createCollection(String wskey, String key, String path) throws CoreServiceException, KeyAlreadyExistsException, InvalidPathException, AccessDeniedException,
            PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "creating collection with key [" + key + "] into workspace [" + wskey + "] at path [" + path + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            if (npath.isRoot()) {
                throw new InvalidPathException("forbidden to create the root collection");
            }
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to create collection in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "create");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

            Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
            LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

            if (parent.containsElementName(npath.part())) {
                throw new PathAlreadyExistsException(npath.build());
            }

            String id = UUID.randomUUID().toString();
            Collection collection = new Collection();
            collection.setId(id);
            collection.setKey(key);
            collection.setName(npath.part());
            collection.setRoot(false);
            collection.setClock(ws.getClock());
            em.persist(collection);
            LOGGER.log(Level.FINEST, "collection [" + key + "] created");

            registry.register(key, collection.getObjectIdentifier(), caller);
            indexing.index(key);

            authorisation.clonePolicy(key, ws.getHead());
            LOGGER.log(Level.FINEST, "security policy cloned from head collection to key [" + key + "]");

            parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, collection.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, key));
            em.merge(parent);
            registry.update(parent.getKey());
            LOGGER.log(Level.FINEST, "collection [" + key + "] added to parent [" + parent.getKey() + "]");

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(3).addArgument("ws-alias", ws.getAlias()).addArgument("key", key).addArgument("path", path);
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "create"), argsBuilder.build());

            return collection;
        } catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
                | MembershipServiceException | IndexingServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during collection creation", e);
            ctx.setRollbackOnly();
            throw new CoreServiceException("unable to create collection into workspace [" + wskey + "] at path [" + path + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Collection readCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "reading collection with key [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier cidentifier = registry.lookup(key);
            checkObjectType(cidentifier, Collection.OBJECT_TYPE);
            authorisation.checkPermission(key, subjects, "read");

            Collection collection = em.find(Collection.class, cidentifier.getId());
            if (collection == null) {
                throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
            }
            collection.setKey(key);

            return collection;
        } catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error while reading collection", e);
            throw new CoreServiceException("unable to read collection with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String resolvePathFromCollection(String key, String path) throws KeyNotFoundException, CoreServiceException, AccessDeniedException, InvalidPathException, PathNotFoundException {
        LOGGER.log(Level.FINE, "reading collection with key [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier cidentifier = registry.lookup(key);
            checkObjectType(cidentifier, Collection.OBJECT_TYPE);

            Collection collection = em.find(Collection.class, cidentifier.getId());
            if (collection == null) {
                throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
            }
            collection.setKey(key);

            PathBuilder pathTarget = PathBuilder.fromPath(path);
            PathBuilder parentTarget = pathTarget.clone().parent();

            Collection parent = readCollectionAtPath(key, parentTarget);

            String partTarget = pathTarget.part();

            CollectionElement element = parent.findElementByName(partTarget);
            if (element == null) {
                throw new PathNotFoundException(path);
            }
            authorisation.checkPermission(element.getKey(), subjects, "read");

            return element.getKey();
        } catch (MembershipServiceException | AuthorisationServiceException | RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error while reading collection", e);
            throw new CoreServiceException("unable to resolve path " + path + " from collection with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void moveElements(String wskey, List<String> sources, String destination) throws PathAlreadyExistsException, PathNotFoundException, RegistryServiceException, InvalidPathException,
            AccessDeniedException, WorkspaceReadOnlyException, CoreServiceException, KeyNotFoundException {
        if (!sources.isEmpty()) {
            try {
                String ppath = PathBuilder.fromPath(sources.get(0)).parent().build();
                for (String source : sources) {
                    String sppath = PathBuilder.fromPath(source).parent().build();
                    if (!ppath.equals(sppath)) {
                        throw new InvalidPathException("unable to move elements from different collections");
                    }
                }
                String parentKey = resolveWorkspacePath(wskey, "head", ppath);
                OrtolangObjectIdentifier identifier = registry.lookup(parentKey);
                checkObjectType(identifier, Collection.OBJECT_TYPE);
                Collection collection = readCollection(parentKey);

                for (String source : sources) {
                    CollectionElement collectionElement = collection.findElementByName(PathBuilder.fromPath(source).part());
                    switch (collectionElement.getType()) {
                    case DataObject.OBJECT_TYPE:
                        moveDataObject(wskey, source, destination + PathBuilder.PATH_SEPARATOR + collectionElement.getName());
                        break;
                    case Collection.OBJECT_TYPE:
                        moveCollection(wskey, source, destination + PathBuilder.PATH_SEPARATOR + collectionElement.getName());
                        break;
                    case Link.OBJECT_TYPE:
                        moveLink(wskey, source, destination + PathBuilder.PATH_SEPARATOR + collectionElement.getName());
                    }
                }
            } catch (AccessDeniedException | CoreServiceException | RegistryServiceException | WorkspaceReadOnlyException | InvalidPathException | PathNotFoundException | KeyNotFoundException e) {
                ctx.setRollbackOnly();
                LOGGER.log(Level.SEVERE, "unexpected error while moving workspace elements", e);
                throw e;
            } catch (PathAlreadyExistsException e) {
                ctx.setRollbackOnly();
                throw e;
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteElements(String wskey, List<String> sources, boolean force) throws InvalidPathException, CoreServiceException, PathNotFoundException, AccessDeniedException,
            KeyNotFoundException, WorkspaceReadOnlyException, CollectionNotEmptyException, RegistryServiceException {
        if (!sources.isEmpty()) {
            try {
                String ppath = PathBuilder.fromPath(sources.get(0)).parent().build();
                for (String source : sources) {
                    String sppath = PathBuilder.fromPath(source).parent().build();
                    if (!ppath.equals(sppath)) {
                        throw new InvalidPathException("unable to delete elements from different collections");
                    }
                }
                String parentKey = resolveWorkspacePath(wskey, "head", ppath);
                OrtolangObjectIdentifier identifier = registry.lookup(parentKey);
                checkObjectType(identifier, Collection.OBJECT_TYPE);
                Collection collection = readCollection(parentKey);

                for (String source : sources) {
                    CollectionElement collectionElement = collection.findElementByName(PathBuilder.fromPath(source).part());
                    switch (collectionElement.getType()) {
                    case DataObject.OBJECT_TYPE:
                        deleteDataObject(wskey, source);
                        break;
                    case Collection.OBJECT_TYPE:
                        deleteCollection(wskey, source, force);
                    }
                }
            } catch (InvalidPathException | CoreServiceException | PathNotFoundException | AccessDeniedException | KeyNotFoundException | WorkspaceReadOnlyException | RegistryServiceException e) {
                ctx.setRollbackOnly();
                LOGGER.log(Level.SEVERE, "unexpected error while moving workspace elements", e);
                throw e;
            } catch (CollectionNotEmptyException e) {
                ctx.setRollbackOnly();
                throw e;
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Collection moveCollection(String wskey, String source, String destination) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException,
            PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "moving collection into workspace [" + wskey + "] from path [" + source + "] to path [" + destination + "]");
        try {
            PathBuilder spath = PathBuilder.fromPath(source);
            if (spath.isRoot()) {
                throw new InvalidPathException("unable to move the root collection");
            }
            PathBuilder sppath = spath.clone().parent();

            PathBuilder dpath = PathBuilder.fromPath(destination);
            if (dpath.isRoot()) {
                throw new InvalidPathException("unable to move to the root collection");
            }
            PathBuilder dppath = dpath.clone().parent();

            if (dpath.equals(spath)) {
                throw new InvalidPathException("unable to move into the same path");
            }
            if (spath.isParent(dpath)) {
                throw new InvalidPathException("unable to move into a children of this path");
            }

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to move collection in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "update");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

            Collection sparent = loadCollectionAtPath(ws.getHead(), sppath, ws.getClock());
            CollectionElement selement = sparent.findElementByName(spath.part());
            if (selement == null) {
                throw new PathNotFoundException(spath.build());
            }
            LOGGER.log(Level.FINEST, "source collection element found for path " + spath.build() + ", key: " + selement.getKey());

            OrtolangObjectIdentifier scidentifier = registry.lookup(selement.getKey());
            checkObjectType(scidentifier, Collection.OBJECT_TYPE);
            Collection scollection = em.find(Collection.class, scidentifier.getId());
            if (scollection == null) {
                throw new CoreServiceException("unable to load source collection with id [" + scidentifier.getId() + "] from storage");
            }
            scollection.setKey(selement.getKey());
            LOGGER.log(Level.FINEST, "source collection exists and loaded from storage");

            Collection dparent = loadCollectionAtPath(ws.getHead(), dppath, ws.getClock());
            if (dparent.containsElementName(dpath.part())) {
                throw new PathAlreadyExistsException(dpath.build());
            }

            sparent.removeElement(selement);
            em.merge(sparent);
            registry.update(sparent.getKey());
            LOGGER.log(Level.FINEST, "parent [" + sparent.getKey() + "] has been updated");

            LOGGER.log(Level.FINEST, "destination element does not exists, ok for creating it");
            if (!dpath.part().equals(spath.part())) {
                if (scollection.getClock() < ws.getClock()) {
                    scollection = cloneCollection(ws.getHead(), scollection, ws.getClock());
                }
                scollection.setName(dpath.part());
                em.merge(scollection);
                registry.update(scollection.getKey());
            }
            dparent.addElement(new CollectionElement(Collection.OBJECT_TYPE, scollection.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, scollection.getKey()));
            em.merge(dparent);
            registry.update(dparent.getKey());
            LOGGER.log(Level.FINEST, "collection [" + scollection.getKey() + "] added to destination parent [" + dparent.getKey() + "]");

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(5).addArgument("ws-alias", ws.getAlias()).addArgument("key", scollection.getKey()).addArgument("okey", selement.getKey())
                    .addArgument("src-path", spath.build()).addArgument("dest-path", dpath.build());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "move"), argsBuilder.build());

            return scollection;
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | CloneException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error while moving collection", e);
            throw new CoreServiceException("unable to move collection into workspace [" + wskey + "] from path [" + source + "] to path [" + destination + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteCollection(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, CollectionNotEmptyException,
            PathNotFoundException, WorkspaceReadOnlyException {
        deleteCollection(workspace, path, false);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteCollection(String wskey, String path, boolean force) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, CollectionNotEmptyException,
            PathNotFoundException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "deleting collection into workspace [" + wskey + "] at path [" + path + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            if (npath.isRoot()) {
                throw new InvalidPathException("unable to delete the root collection");
            }
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to delete collection in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "delete");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'delete' permission on the head collection of this workspace");

            Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
            CollectionElement element = parent.findElementByName(npath.part());
            if (element == null) {
                throw new PathNotFoundException(npath.build());
            }
            LOGGER.log(Level.FINEST, "collection element found for path " + npath.build() + ", key: " + element.getKey());

            OrtolangObjectIdentifier cidentifier = registry.lookup(element.getKey());
            checkObjectType(cidentifier, Collection.OBJECT_TYPE);
            Collection leaf = em.find(Collection.class, cidentifier.getId());
            if (leaf == null) {
                throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
            }
            leaf.setKey(element.getKey());
            LOGGER.log(Level.FINEST, "collection exists and loaded from storage");

            if (!leaf.isEmpty() && !force) {
                throw new CollectionNotEmptyException(path);
            }

            parent.removeElement(element);
            em.merge(parent);
            registry.update(parent.getKey());

            LOGGER.log(Level.FINEST, "parent [" + parent.getKey() + "] has been updated");

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            if (leaf.getClock() == ws.getClock()) {
                LOGGER.log(Level.FINEST, "leaf clock [" + leaf.getClock() + "] is the same than workspace, key can be deleted and unindexed");
                for (MetadataElement mde : leaf.getMetadatas()) {
                    registry.delete(mde.getKey());
                }
                registry.delete(leaf.getKey());
                indexing.remove(leaf.getKey());
            }
            deleteCollectionContent(leaf, ws.getClock());

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(3).addArgument("ws-alias", ws.getAlias()).addArgument("key", leaf.getKey()).addArgument("path", npath.build());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, "delete"), argsBuilder.build());
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error while deleting collection", e);
            throw new CoreServiceException("unable to delete collection into workspace [" + wskey + "] at path [" + path + "]", e);
        }
    }

    /* Data Objects */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DataObject createDataObject(String workspace, String path, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException,
            PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException, KeyAlreadyExistsException {
        String key = UUID.randomUUID().toString();
        try {
            return createDataObject(workspace, key, path, hash);
        } catch (KeyAlreadyExistsException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.WARNING, "the generated key already exists : " + key);
            throw e;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DataObject createDataObject(String wskey, String key, String path, String hash) throws CoreServiceException, KeyAlreadyExistsException, InvalidPathException, AccessDeniedException,
            PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "create data object with key [" + key + "] into workspace [" + wskey + "] at path [" + path + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            if (npath.isRoot()) {
                throw new InvalidPathException("forbidden to create an object at root level");
            }
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to create data object in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "create");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

            Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
            LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

            if (parent.containsElementName(npath.part())) {
                throw new PathAlreadyExistsException(npath.build());
            }

            DataObject object = new DataObject();
            object.setId(UUID.randomUUID().toString());
            object.setName(npath.part());
            if (hash != null && hash.length() > 0) {
                object.setSize(binarystore.size(hash));
                object.setMimeType(binarystore.type(hash, npath.part()));
                object.setStream(hash);
            } else {
                object.setSize(0);
                object.setMimeType("application/octet-stream");
                object.setStream("");
            }
            object.setClock(ws.getClock());
            object.setKey(key);
            em.persist(object);
            LOGGER.log(Level.FINEST, "object [" + key + "] created");

            registry.register(key, object.getObjectIdentifier(), caller);
            indexing.index(key);

            authorisation.clonePolicy(key, ws.getHead());
            LOGGER.log(Level.FINEST, "security policy cloned from head collection to key [" + key + "]");

            parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, object.getName(), System.currentTimeMillis(), object.getSize(), object.getMimeType(), key));
            em.merge(parent);
            registry.update(parent.getKey());

            LOGGER.log(Level.FINEST, "object [" + key + "] added to parent [" + parent.getKey() + "]");

            try {
                extraction.extract(object.getKey());
            } catch (ExtractionServiceException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
            }

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(5).addArgument("ws-alias", ws.getAlias()).addArgument("key", key).addArgument("path", npath.build())
                    .addArgument("hash", object.getStream()).addArgument("mimetype", object.getMimeType());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "create"), argsBuilder.build());

            return object;
        } catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
                | MembershipServiceException | BinaryStoreServiceException | DataNotFoundException | IndexingServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during object creation", e);
            ctx.setRollbackOnly();
            throw new CoreServiceException("unable to create object into workspace [" + wskey + "] at path [" + path + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DataObject readDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "reading object with key [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, DataObject.OBJECT_TYPE);
            authorisation.checkPermission(key, subjects, "read");

            DataObject object = em.find(DataObject.class, identifier.getId());
            if (object == null) {
                throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
            }
            object.setKey(key);

            return object;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while reading object", e);
            throw new CoreServiceException("unable to read object with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DataObject updateDataObject(String wskey, String path, String hash) throws CoreServiceException, InvalidPathException, AccessDeniedException, PathNotFoundException,
            WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "updating object into workspace [" + wskey + "] at path [" + path + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            if (npath.isRoot()) {
                throw new InvalidPathException("path is empty");
            }
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to update data object in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "update");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

            String current = resolveWorkspacePath(wskey, Workspace.HEAD, npath.build());
            OrtolangObjectIdentifier cidentifier = registry.lookup(current);
            checkObjectType(cidentifier, DataObject.OBJECT_TYPE);
            DataObject cobject = em.find(DataObject.class, cidentifier.getId());
            if (cobject == null) {
                throw new CoreServiceException("unable to load object with id [" + cidentifier.getId() + "] from storage");
            }
            cobject.setKey(current);
            LOGGER.log(Level.FINEST, "current object loaded");

            if (!hash.equals(cobject.getStream())) {
                Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
                LOGGER.log(Level.FINEST, "parent collection loaded for path " + npath.build());

                CollectionElement element = parent.findElementByName(npath.part());
                if (element == null) {
                    throw new PathNotFoundException(npath.build());
                }
                LOGGER.log(Level.FINEST, "object element found for name " + npath.part());
                if (!element.getType().equals(DataObject.OBJECT_TYPE)) {
                    throw new InvalidPathException("path [" + npath.build() + "] is not a data object");
                }

                OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
                checkObjectType(identifier, DataObject.OBJECT_TYPE);
                DataObject object = em.find(DataObject.class, identifier.getId());
                if (object == null) {
                    throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
                }
                object.setKey(element.getKey());
                if (object.getClock() < ws.getClock()) {
                    DataObject clone = cloneDataObject(ws.getHead(), object, ws.getClock());
                    object = clone;
                }
                if (hash.length() > 0) {
                    object.setSize(binarystore.size(hash));
                    object.setMimeType(binarystore.type(hash, object.getName()));
                    object.setStream(hash);
                } else {
                    object.setSize(0);
                    object.setMimeType("application/octet-stream");
                    object.setStream("");
                }
                parent.removeElement(element);
                CollectionElement celement = new CollectionElement(DataObject.OBJECT_TYPE, object.getName(), System.currentTimeMillis(), object.getSize(), object.getMimeType(), object.getKey());
                parent.addElement(celement);
                em.merge(parent);
                em.merge(object);
                registry.update(parent.getKey());
                registry.update(object.getKey());
                indexing.index(object.getKey());
                LOGGER.log(Level.FINEST, "object updated");

                try {
                    extraction.extract(object.getKey());
                } catch (ExtractionServiceException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }

                ws.setChanged(true);
                em.merge(ws);
                registry.update(ws.getKey());
                LOGGER.log(Level.FINEST, "workspace set changed");

                ArgumentsBuilder argsBuilder = new ArgumentsBuilder(6).addArgument("ws-alias", ws.getAlias()).addArgument("key", object.getKey()).addArgument("okey", element.getKey())
                        .addArgument("path", npath.build()).addArgument("hash", object.getStream()).addArgument("mimetype", object.getMimeType());
                notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "update"), argsBuilder.build());

                return object;
            } else {
                LOGGER.log(Level.FINEST, "no changes detected with current object, nothing to do");
                return cobject;
            }
        } catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException
                | BinaryStoreServiceException | DataNotFoundException | CloneException | IndexingServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while reading object", e);
            throw new CoreServiceException("unable to read object into workspace [" + wskey + "] at path [" + path + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DataObject moveDataObject(String wskey, String source, String destination) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException,
            PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "moving object into workspace [" + wskey + "] from path [" + source + "] to path [" + destination + "]");
        try {
            PathBuilder spath = PathBuilder.fromPath(source);
            if (spath.isRoot()) {
                throw new InvalidPathException("path is empty");
            }
            PathBuilder sppath = spath.clone().parent();

            PathBuilder dpath = PathBuilder.fromPath(destination);
            if (dpath.isRoot()) {
                throw new InvalidPathException("path is empty");
            }
            PathBuilder dppath = dpath.clone().parent();

            if (dpath.equals(spath)) {
                throw new InvalidPathException("unable to move into the same path");
            }
            if (spath.isParent(dpath)) {
                throw new InvalidPathException("unable to move into a children of this path");
            }

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to move data object in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "update");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

            Collection sparent = loadCollectionAtPath(ws.getHead(), sppath, ws.getClock());
            CollectionElement selement = sparent.findElementByName(spath.part());
            if (selement == null) {
                throw new PathNotFoundException(spath.build());
            }
            LOGGER.log(Level.FINEST, "source object element found for name " + spath.part());

            OrtolangObjectIdentifier soidentifier = registry.lookup(selement.getKey());
            checkObjectType(soidentifier, DataObject.OBJECT_TYPE);
            DataObject sobject = em.find(DataObject.class, soidentifier.getId());
            if (sobject == null) {
                throw new CoreServiceException("unable to load source object with id [" + soidentifier.getId() + "] from storage");
            }
            sobject.setKey(selement.getKey());
            LOGGER.log(Level.FINEST, "source object exists and loaded from storage");

            Collection dparent = loadCollectionAtPath(ws.getHead(), dppath, ws.getClock());
            if (dparent.containsElementName(dpath.part())) {
                throw new PathAlreadyExistsException(dpath.build());
            }

            sparent.removeElement(selement);
            em.merge(sparent);
            registry.update(sparent.getKey());
            LOGGER.log(Level.FINEST, "parent [" + sparent.getKey() + "] has been updated");

            LOGGER.log(Level.FINEST, "destination element does not exists, creating it");
            if (!dpath.part().equals(spath.part())) {
                if (sobject.getClock() < ws.getClock()) {
                    sobject = cloneDataObject(ws.getHead(), sobject, ws.getClock());
                }
                sobject.setName(dpath.part());
                em.merge(sobject);
                registry.update(sobject.getKey());
            }
            dparent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, sobject.getName(), System.currentTimeMillis(), sobject.getSize(), sobject.getMimeType(), sobject.getKey()));
            em.merge(dparent);
            registry.update(dparent.getKey());

            LOGGER.log(Level.FINEST, "object [" + sobject.getKey() + "] added to destination parent [" + dparent.getKey() + "]");

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(5).addArgument("ws-alias", ws.getAlias()).addArgument("key", sobject.getKey()).addArgument("okey", selement.getKey())
                    .addArgument("src-path", spath.build()).addArgument("dest-path", dpath.build());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "move"), argsBuilder.build());

            return sobject;
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | CloneException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error while moving object", e);
            throw new CoreServiceException("unable to move object into workspace [" + wskey + "] from path [" + source + "] to path [" + destination + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteDataObject(String wskey, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, PathNotFoundException,
            WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "deleting object into workspace [" + wskey + "] at path [" + path + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            if (npath.isRoot()) {
                throw new InvalidPathException("path is empty");
            }
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to delete data object in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "delete");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'delete' permission on the head collection of this workspace");

            Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
            CollectionElement element = parent.findElementByName(npath.part());
            if (element == null) {
                throw new PathNotFoundException(npath.build());
            }
            LOGGER.log(Level.FINEST, "object element found for path " + npath.build() + ", key: " + element.getKey());

            OrtolangObjectIdentifier oidentifier = registry.lookup(element.getKey());
            checkObjectType(oidentifier, DataObject.OBJECT_TYPE);
            DataObject leaf = em.find(DataObject.class, oidentifier.getId());
            if (leaf == null) {
                throw new CoreServiceException("unable to load object with id [" + oidentifier.getId() + "] from storage");
            }
            leaf.setKey(element.getKey());
            LOGGER.log(Level.FINEST, "object exists and loaded from storage");

            parent.removeElement(element);
            em.merge(parent);
            registry.update(parent.getKey());

            LOGGER.log(Level.FINEST, "parent [" + parent.getKey() + "] has been updated");

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            if (leaf.getClock() == ws.getClock()) {
                LOGGER.log(Level.FINEST, "leaf clock [" + leaf.getClock() + "] is the same than workspace, key can be deleted and unindexed");
                for (MetadataElement mde : leaf.getMetadatas()) {
                    registry.delete(mde.getKey());
                }
                registry.delete(leaf.getKey());
                indexing.remove(leaf.getKey());
            }

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(3).addArgument("ws-alias", ws.getAlias()).addArgument("key", leaf.getKey()).addArgument("path", npath.build());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, "delete"), argsBuilder.build());
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error while deleting object", e);
            throw new CoreServiceException("unable to delete object into workspace [" + wskey + "] at path [" + path + "]", e);
        }
    }

    /* Link */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Link createLink(String workspace, String path, String target) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, PathNotFoundException,
            PathAlreadyExistsException, WorkspaceReadOnlyException, KeyAlreadyExistsException {
        String key = UUID.randomUUID().toString();
        try {
            return createLink(workspace, key, path, target);
        } catch (KeyAlreadyExistsException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.WARNING, "the generated key already exists : " + key);
            throw e;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Link createLink(String wskey, String key, String path, String target) throws CoreServiceException, KeyAlreadyExistsException, InvalidPathException, AccessDeniedException,
            PathNotFoundException, PathAlreadyExistsException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "create link with key [" + key + "] into workspace [" + wskey + "] at path [" + path + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            if (npath.isRoot()) {
                throw new InvalidPathException("path is empty");
            }
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            authorisation.checkPermission(target, subjects, "read");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'read' permissions on the target");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to create link in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "create");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

            Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
            LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());

            if (parent.containsElementName(npath.part())) {
                throw new PathAlreadyExistsException(npath.build());
            }

            String ntarget = PathBuilder.fromPath(target).build();

            Link link = new Link();
            link.setId(UUID.randomUUID().toString());
            link.setKey(key);
            link.setName(npath.part());
            link.setClock(ws.getClock());
            link.setTarget(ntarget);
            em.persist(link);

            registry.register(key, link.getObjectIdentifier(), caller);
            indexing.index(key);

            authorisation.clonePolicy(key, ws.getHead());
            LOGGER.log(Level.FINEST, "security policy cloned from head collection to key [" + key + "]");

            parent.addElement(new CollectionElement(Link.OBJECT_TYPE, link.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, key));
            em.merge(parent);
            registry.update(parent.getKey());
            LOGGER.log(Level.FINEST, "link [" + key + "] added to parent [" + parent.getKey() + "]");

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(3).addArgument("ws-alias", ws.getAlias()).addArgument("key", key).addArgument("path", npath.build());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "create"), argsBuilder.build());

            return link;
        } catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
                | MembershipServiceException | IndexingServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during link creation", e);
            ctx.setRollbackOnly();
            throw new CoreServiceException("unable to create link into workspace [" + wskey + "] at path [" + path + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Link readLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "reading link with key [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, Link.OBJECT_TYPE);
            authorisation.checkPermission(key, subjects, "read");

            Link link = em.find(Link.class, identifier.getId());
            if (link == null) {
                throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
            }
            link.setKey(key);

            return link;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while reading link", e);
            throw new CoreServiceException("unable to read link with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Link updateLink(String wskey, String path, String target) throws CoreServiceException, InvalidPathException, AccessDeniedException, PathNotFoundException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "updating link into workspace [" + wskey + "] at path [" + path + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            if (npath.isRoot()) {
                throw new InvalidPathException("path is empty");
            }
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to update link in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "update");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

            String current = resolveWorkspacePath(wskey, Workspace.HEAD, npath.build());
            OrtolangObjectIdentifier cidentifier = registry.lookup(current);
            checkObjectType(cidentifier, Link.OBJECT_TYPE);
            Link clink = em.find(Link.class, cidentifier.getId());
            if (clink == null) {
                throw new CoreServiceException("unable to load link with id [" + cidentifier.getId() + "] from storage");
            }
            clink.setKey(current);
            LOGGER.log(Level.FINEST, "current link loaded");

            String ntarget = PathBuilder.fromPath(target).build();

            if (!ntarget.equals(clink.getTarget())) {
                Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
                LOGGER.log(Level.FINEST, "parent collection loaded for path " + npath.build());

                CollectionElement element = parent.findElementByName(npath.part());
                if (element == null) {
                    throw new PathNotFoundException(npath.build());
                }
                LOGGER.log(Level.FINEST, "link element found for name " + npath.part());
                if (!element.getType().equals(Link.OBJECT_TYPE)) {
                    throw new InvalidPathException("path [" + npath.build() + "] is not a link");
                }

                OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
                checkObjectType(identifier, Link.OBJECT_TYPE);
                Link link = em.find(Link.class, identifier.getId());
                if (link == null) {
                    throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
                }
                link.setKey(element.getKey());
                link.setTarget(ntarget);
                if (link.getClock() < ws.getClock()) {
                    Link clone = cloneLink(ws.getHead(), link, ws.getClock());
                    parent.removeElement(element);
                    CollectionElement celement = new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, clone.getKey());
                    parent.addElement(celement);
                    link = clone;
                }
                em.merge(parent);
                em.merge(link);
                registry.update(parent.getKey());
                registry.update(link.getKey());
                indexing.index(link.getKey());
                LOGGER.log(Level.FINEST, "link updated");

                ws.setChanged(true);
                em.merge(ws);
                registry.update(ws.getKey());
                LOGGER.log(Level.FINEST, "workspace set changed");

                ArgumentsBuilder argsBuilder = new ArgumentsBuilder(4).addArgument("ws-alias", ws.getAlias()).addArgument("key", link.getKey()).addArgument("okey", element.getKey())
                        .addArgument("path", npath.build());
                notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "update"), argsBuilder.build());

                return link;
            } else {
                LOGGER.log(Level.FINEST, "no changes detected with current link, nothing to do");
                return clink;
            }
        } catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException | CloneException
                | IndexingServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while updating link", e);
            throw new CoreServiceException("unable to update link into workspace [" + wskey + "] at path [" + path + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Link moveLink(String wskey, String source, String destination) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, PathNotFoundException,
            PathAlreadyExistsException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "moving link into workspace [" + wskey + "] from path [" + source + "] to path [" + destination + "]");
        try {
            PathBuilder spath = PathBuilder.fromPath(source);
            if (spath.isRoot()) {
                throw new InvalidPathException("path is empty");
            }
            PathBuilder sppath = spath.clone().parent();

            PathBuilder dpath = PathBuilder.fromPath(destination);
            if (dpath.isRoot()) {
                throw new InvalidPathException("path is empty");
            }
            PathBuilder dppath = dpath.clone().parent();

            if (dpath.equals(spath)) {
                throw new InvalidPathException("unable to move into the same path");
            }
            if (spath.isParent(dpath)) {
                throw new InvalidPathException("unable to move into a children of this path");
            }

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to move link in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "update");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

            Collection sparent = loadCollectionAtPath(ws.getHead(), sppath, ws.getClock());
            CollectionElement selement = sparent.findElementByName(spath.part());
            if (selement == null) {
                throw new PathNotFoundException(spath.build());
            }
            LOGGER.log(Level.FINEST, "source link element found for name " + spath.part());

            OrtolangObjectIdentifier slidentifier = registry.lookup(selement.getKey());
            checkObjectType(slidentifier, Link.OBJECT_TYPE);
            Link slink = em.find(Link.class, slidentifier.getId());
            if (slink == null) {
                throw new CoreServiceException("unable to load source link with id [" + slidentifier.getId() + "] from storage");
            }
            slink.setKey(selement.getKey());
            LOGGER.log(Level.FINEST, "source link exists and loaded from storage");

            Collection dparent = loadCollectionAtPath(ws.getHead(), dppath, ws.getClock());
            if (dparent.containsElementName(dpath.part())) {
                throw new PathAlreadyExistsException(dpath.build());
            }

            sparent.removeElement(selement);
            em.merge(sparent);
            registry.update(sparent.getKey());
            indexing.index(sparent.getKey());
            LOGGER.log(Level.FINEST, "parent [" + sparent.getKey() + "] has been updated");

            if (!dpath.part().equals(spath.part())) {
                if (slink.getClock() < ws.getClock()) {
                    slink = cloneLink(ws.getHead(), slink, ws.getClock());
                }
                slink.setName(dpath.part());
                em.merge(slink);
                registry.update(slink.getKey());
                indexing.index(slink.getKey());
            }

            dparent.addElement(new CollectionElement(Link.OBJECT_TYPE, slink.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, slink.getKey()));
            em.merge(dparent);
            registry.update(dparent.getKey());
            indexing.index(dparent.getKey());
            LOGGER.log(Level.FINEST, "link [" + slink.getKey() + "] added to destination parent [" + dparent.getKey() + "]");

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(5).addArgument("ws-alias", ws.getAlias()).addArgument("key", slink.getKey()).addArgument("okey", selement.getKey())
                    .addArgument("src-path", spath.build()).addArgument("dest-path", dpath.build());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "move"), argsBuilder.build());

            return slink;
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | CloneException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error while moving link", e);
            throw new CoreServiceException("unable to move link into workspace [" + wskey + "] from path [" + source + "] to path [" + destination + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteLink(String wskey, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException, PathNotFoundException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "deleting link into workspace [" + wskey + "] at path [" + path + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            if (npath.isRoot()) {
                throw new InvalidPathException("path is empty");
            }
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to delete link in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "delete");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'delete' permission on the head collection of this workspace");

            Collection parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
            CollectionElement element = parent.findElementByName(npath.part());
            if (element == null) {
                throw new PathNotFoundException(npath.build());
            }
            LOGGER.log(Level.FINEST, "link element found for name " + npath.part());

            OrtolangObjectIdentifier lidentifier = registry.lookup(element.getKey());
            checkObjectType(lidentifier, Link.OBJECT_TYPE);
            Link leaf = em.find(Link.class, lidentifier.getId());
            if (leaf == null) {
                throw new CoreServiceException("unable to load link with id [" + lidentifier.getId() + "] from storage");
            }
            leaf.setKey(element.getKey());
            LOGGER.log(Level.FINEST, "link exists and loaded from storage");

            parent.removeElement(element);
            em.merge(parent);
            registry.update(parent.getKey());
            indexing.index(parent.getKey());
            LOGGER.log(Level.FINEST, "parent [" + parent.getKey() + "] has been updated");

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            if (leaf.getClock() == ws.getClock()) {
                LOGGER.log(Level.FINEST, "leaf clock [" + leaf.getClock() + "] is the same than workspace, key can be deleted and unindexed");
                for (MetadataElement mde : leaf.getMetadatas()) {
                    registry.delete(mde.getKey());
                }
                registry.delete(leaf.getKey());
                indexing.remove(leaf.getKey());
            }

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(3).addArgument("ws-alias", ws.getAlias()).addArgument("key", leaf.getKey()).addArgument("path", npath.build());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Link.OBJECT_TYPE, "delete"), argsBuilder.build());
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | MembershipServiceException | AuthorisationServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error while deleting link", e);
            throw new CoreServiceException("unable to delete link into workspace [" + wskey + "] at path [" + path + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> findLinksForTarget(String target) throws CoreServiceException {
        LOGGER.log(Level.FINE, "finding links for target [" + target + "]");
        try {
            TypedQuery<Link> query = em.createNamedQuery("findLinksForTarget", Link.class).setParameter("target", target);
            List<Link> links = query.getResultList();
            List<String> results = new ArrayList<String>();
            for (Link link : links) {
                String key = registry.lookup(link.getObjectIdentifier());
                results.add(key);
            }
            return results;
        } catch (RegistryServiceException | IdentifierNotRegisteredException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during finding links for target", e);
            throw new CoreServiceException("unable to find link for target [" + target + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String resolveLinkTarget(String target) throws CoreServiceException, InvalidPathException, PathNotFoundException, AccessDeniedException, AliasNotFoundException {
        LOGGER.log(Level.FINE, "resolving link target [" + target + "]");
        PathBuilder tpath = PathBuilder.fromPath(target);
        String[] tparts = tpath.buildParts();
        if (tparts.length < 2) {
            throw new CoreServiceException("unable to resolve target, path must contains at least an alias and a version");
        }
        String wskey = resolveWorkspaceAlias(tparts[0]);
        String root = tparts[1];
        String path = tpath.relativize(2).build();
        if (root.equals(Workspace.HEAD) || root.equals(Workspace.LATEST)) {
            throw new CoreServiceException("unable to resolve target due to " + Workspace.HEAD + " or " + Workspace.LATEST + " version reference");
        }
        return resolveWorkspacePath(wskey, root, path);
    }

    /* Metadatas */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public MetadataObject createMetadataObject(String workspace, String path, String name, String hash, String filename, boolean purgeChildren) throws CoreServiceException, KeyNotFoundException,
            InvalidPathException, AccessDeniedException, MetadataFormatException, PathNotFoundException, WorkspaceReadOnlyException, KeyAlreadyExistsException {
        String key = UUID.randomUUID().toString();
        try {
            return createMetadataObject(workspace, key, path, name, hash, filename, purgeChildren);
        } catch (KeyAlreadyExistsException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.WARNING, "the generated key already exists : " + key);
            throw e;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public MetadataObject createMetadataObject(String wskey, String key, String path, String name, String hash, String filename, boolean purgeChildren) throws CoreServiceException,
            KeyAlreadyExistsException, InvalidPathException, MetadataFormatException, PathNotFoundException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "create metadataobject with key [" + key + "] into workspace [" + wskey + "] for path [" + path + "] with name [" + name + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to create metadata object in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "create");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

            String tkey;
            Collection parent = null;
            CollectionElement element = null;
            if (npath.isRoot()) {
                tkey = ws.getHead();
            } else {
                parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
                LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());
                element = parent.findElementByName(npath.part());
                if (element == null) {
                    throw new PathNotFoundException(npath.build());
                }
                LOGGER.log(Level.FINEST, "collection element found for name " + npath.part());
                tkey = element.getKey();
            }

            OrtolangObjectIdentifier tidentifier = registry.lookup(tkey);
            if (!tidentifier.getType().equals(Link.OBJECT_TYPE) && !tidentifier.getType().equals(Collection.OBJECT_TYPE) && !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
                throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
            }

            MetadataObject meta = new MetadataObject();
            meta.setId(UUID.randomUUID().toString());
            meta.setName(name);

            MetadataFormat format = getMetadataFormat(name);
            if (format == null) {
                LOGGER.log(Level.SEVERE, "Unable to find a metadata format for name: " + name);
                throw new CoreServiceException("unknown metadata format for name: " + name);
            }

            if (hash != null && hash.length() > 0) {
                if (format.isValidationNeeded()) {
                    validateMetadata(hash, format);
                }
                meta.setSize(binarystore.size(hash));
                if (filename != null) {
                    meta.setContentType(binarystore.type(hash, filename));
                } else {
                    meta.setContentType(binarystore.type(hash));
                }
                meta.setStream(hash);
            } else {
                meta.setSize(0);
                meta.setContentType("application/octet-stream");
                meta.setStream("");
            }

            meta.setFormat(format.getId());
            meta.setTarget(tkey);
            meta.setKey(key);
            em.persist(meta);

            registry.register(key, meta.getObjectIdentifier(), caller);

            authorisation.clonePolicy(key, ws.getHead());

            switch (tidentifier.getType()) {
            case Collection.OBJECT_TYPE:
                Collection collection = em.find(Collection.class, tidentifier.getId());
                if (collection == null) {
                    ctx.setRollbackOnly();
                    throw new CoreServiceException("unable to load collection with id [" + tidentifier.getId() + "] from storage");
                }
                collection.setKey(tkey);
                for (MetadataElement mde : collection.getMetadatas()) {
                    if (mde.getName().equals(name)) {
                        ctx.setRollbackOnly();
                        throw new CoreServiceException("a metadata object with name [" + name + "] already exists for collection at path [" + npath.build() + "]");
                    }
                }
                if (collection.getClock() < ws.getClock()) {
                    Collection clone = cloneCollection(ws.getHead(), collection, ws.getClock());
                    tkey = clone.getKey();
                    meta.setTarget(tkey);
                    if (parent != null) {
                        parent.removeElement(element);
                        parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, clone.getKey()));
                        em.merge(parent);
                        registry.update(parent.getKey());
                    }
                    collection = clone;
                }
                collection.addMetadata(new MetadataElement(name, key));
                em.merge(collection);
                if (purgeChildren) {
                    LOGGER.log(Level.FINE, "Purging children metadata");
                    purgeChildrenMetadata(collection, wskey, path, name);
                }
                break;
            case DataObject.OBJECT_TYPE:
                DataObject object = em.find(DataObject.class, tidentifier.getId());
                if (object == null) {
                    ctx.setRollbackOnly();
                    throw new CoreServiceException("unable to load object with id [" + tidentifier.getId() + "] from storage");
                }
                object.setKey(tkey);
                for (MetadataElement mde : object.getMetadatas()) {
                    if (mde.getName().equals(name)) {
                        ctx.setRollbackOnly();
                        throw new CoreServiceException("a metadata object with name [" + name + "] already exists for object at path [" + npath.build() + "]");
                    }
                }
                if (object.getClock() < ws.getClock()) {
                    DataObject clone = cloneDataObject(ws.getHead(), object, ws.getClock());
                    tkey = clone.getKey();
                    meta.setTarget(tkey);
                    if (parent == null) {
                        throw new CoreServiceException("An object should have a parent");
                    }
                    parent.removeElement(element);
                    parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getSize(), clone.getMimeType(), clone.getKey()));
                    em.merge(parent);
                    registry.update(parent.getKey());
                    object = clone;
                }
                object.addMetadata(new MetadataElement(name, key));
                em.merge(object);
                break;
            case Link.OBJECT_TYPE:
                Link link = em.find(Link.class, tidentifier.getId());
                if (link == null) {
                    ctx.setRollbackOnly();
                    throw new CoreServiceException("unable to load link with id [" + tidentifier.getId() + "] from storage");
                }
                link.setKey(tkey);
                for (MetadataElement mde : link.getMetadatas()) {
                    if (mde.getName().equals(name)) {
                        ctx.setRollbackOnly();
                        throw new CoreServiceException("a metadata object with name [" + name + "] already exists for link at path [" + npath.build() + "]");
                    }
                }
                if (link.getClock() < ws.getClock()) {
                    Link clone = cloneLink(ws.getHead(), link, ws.getClock());
                    tkey = clone.getKey();
                    meta.setTarget(tkey);
                    if (parent == null) {
                        throw new CoreServiceException("A link should have a parent");
                    }
                    parent.removeElement(element);
                    parent.addElement(new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, clone.getKey()));
                    em.merge(parent);
                    registry.update(parent.getKey());
                    link = clone;
                }
                link.addMetadata(new MetadataElement(name, key));
                em.merge(link);
                break;
            }
            em.merge(meta);

            indexing.index(tkey);

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(5).addArgument("ws-alias", ws.getAlias()).addArgument("key", key).addArgument("tkey", tkey).addArgument("path", npath.build())
                    .addArgument("name", name);
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "create"), argsBuilder.build());

            return meta;
        } catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException
                | MembershipServiceException | BinaryStoreServiceException | DataNotFoundException | CloneException | IndexingServiceException | OrtolangException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred during metadata creation", e);
            throw new CoreServiceException("unable to create metadata into workspace [" + wskey + "] for path [" + path + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public MetadataObject readMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "reading metadata for key [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(key, subjects, "read");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, MetadataObject.OBJECT_TYPE);

            MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
            if (meta == null) {
                throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
            }
            meta.setKey(key);

            return meta;
        } catch (RegistryServiceException | AuthorisationServiceException | MembershipServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during reading metadata", e);
            throw new CoreServiceException("unable to read metadata with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public MetadataObject updateMetadataObject(String wskey, String path, String name, String hash, String filename, boolean purgeChildren) throws CoreServiceException, KeyNotFoundException,
            InvalidPathException, AccessDeniedException, MetadataFormatException, PathNotFoundException, WorkspaceReadOnlyException {
        return updateMetadataObject(wskey, path, name, hash, filename, purgeChildren, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public MetadataObject updateMetadataObject(String wskey, String path, String name, String hash, String filename, boolean purgeChildren, String format) throws CoreServiceException,
            InvalidPathException, MetadataFormatException, PathNotFoundException, WorkspaceReadOnlyException {
        LOGGER.log(Level.FINE, "updating metadata content into workspace [" + wskey + "] for path [" + path + "] and name [" + name + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to update metadata object in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "update");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'update' permission on the head collection of this workspace");

            String current = resolveWorkspacePath(wskey, Workspace.HEAD, npath.build());
            OrtolangObjectIdentifier ctidentifier = registry.lookup(current);
            if (!ctidentifier.getType().equals(Link.OBJECT_TYPE) && !ctidentifier.getType().equals(Collection.OBJECT_TYPE) && !ctidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
                throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
            }
            MetadataElement cmdelement = null;
            switch (ctidentifier.getType()) {
            case Collection.OBJECT_TYPE:
                Collection collection = em.find(Collection.class, ctidentifier.getId());
                if (collection == null) {
                    throw new CoreServiceException("unable to load collection with id [" + ctidentifier.getId() + "] from storage");
                }
                if (collection.findMetadataByName(name) == null) {
                    throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for collection at path [" + npath.build() + "]");
                }
                cmdelement = collection.findMetadataByName(name);
                break;
            case DataObject.OBJECT_TYPE:
                DataObject object = em.find(DataObject.class, ctidentifier.getId());
                if (object == null) {
                    throw new CoreServiceException("unable to load object with id [" + ctidentifier.getId() + "] from storage");
                }
                if (object.findMetadataByName(name) == null) {
                    throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for object at path [" + npath.build() + "]");
                }
                cmdelement = object.findMetadataByName(name);
                break;
            case Link.OBJECT_TYPE:
                Link link = em.find(Link.class, ctidentifier.getId());
                if (link == null) {
                    throw new CoreServiceException("unable to load link with id [" + ctidentifier.getId() + "] from storage");
                }
                if (link.findMetadataByName(name) == null) {
                    throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for link at path [" + npath.build() + "]");
                }
                cmdelement = link.findMetadataByName(name);
                break;
            }
            if (cmdelement == null) {
                throw new CoreServiceException("unable to find current metadata target into workspace [" + wskey + "] for path [" + npath.build() + "] and name [" + name + "]");
            }
            OrtolangObjectIdentifier cidentifier = registry.lookup(cmdelement.getKey());
            checkObjectType(cidentifier, MetadataObject.OBJECT_TYPE);
            MetadataObject cmeta = em.find(MetadataObject.class, cidentifier.getId());
            if (cmeta == null) {
                throw new CoreServiceException("unable to load metadata with id [" + cidentifier.getId() + "] from storage");
            }
            if (!cmeta.getStream().equals(hash)) {
                String tkey = ws.getHead();
                Collection parent = null;
                CollectionElement element = null;
                if (!npath.isRoot()) {
                    parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
                    LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());
                    element = parent.findElementByName(npath.part());
                    if (element == null) {
                        throw new PathNotFoundException(npath.build());
                    }
                    LOGGER.log(Level.FINEST, "collection element found for name " + npath.part());
                    tkey = element.getKey();
                }

                OrtolangObjectIdentifier tidentifier = registry.lookup(tkey);
                if (!tidentifier.getType().equals(Link.OBJECT_TYPE) && !tidentifier.getType().equals(Collection.OBJECT_TYPE) && !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
                    throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
                }

                MetadataElement mdelement = null;
                Collection collection = null;
                switch (tidentifier.getType()) {
                case Collection.OBJECT_TYPE:
                    collection = em.find(Collection.class, tidentifier.getId());
                    if (collection == null) {
                        throw new CoreServiceException("unable to load collection with id [" + tidentifier.getId() + "] from storage");
                    }
                    collection.setKey(tkey);
                    if (collection.findMetadataByName(name) == null) {
                        throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for collection at path [" + npath.build() + "]");
                    }
                    if (collection.getClock() < ws.getClock()) {
                        Collection clone = cloneCollection(ws.getHead(), collection, ws.getClock());
                        tkey = clone.getKey();
                        if (parent != null) {
                            parent.removeElement(element);
                            parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, clone.getKey()));
                        }
                        collection = clone;
                    }
                    mdelement = collection.findMetadataByName(name);
                    break;
                case DataObject.OBJECT_TYPE:
                    DataObject object = em.find(DataObject.class, tidentifier.getId());
                    if (object == null) {
                        throw new CoreServiceException("unable to load object with id [" + tidentifier.getId() + "] from storage");
                    }
                    object.setKey(tkey);
                    if (object.findMetadataByName(name) == null) {
                        throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for object at path [" + npath.build() + "]");
                    }
                    if (object.getClock() < ws.getClock()) {
                        DataObject clone = cloneDataObject(ws.getHead(), object, ws.getClock());
                        tkey = clone.getKey();
                        if (parent == null) {
                            throw new CoreServiceException("An object should have a parent");
                        }
                        parent.removeElement(element);
                        parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getSize(), clone.getMimeType(), clone.getKey()));
                        object = clone;
                    }
                    mdelement = object.findMetadataByName(name);
                    break;
                case Link.OBJECT_TYPE:
                    Link link = em.find(Link.class, tidentifier.getId());
                    if (link == null) {
                        throw new CoreServiceException("unable to load link with id [" + tidentifier.getId() + "] from storage");
                    }
                    link.setKey(tkey);
                    if (link.findMetadataByName(name) == null) {
                        throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for link at path [" + npath.build() + "]");
                    }
                    if (link.getClock() < ws.getClock()) {
                        Link clone = cloneLink(ws.getHead(), link, ws.getClock());
                        tkey = clone.getKey();
                        if (parent == null) {
                            throw new CoreServiceException("A link should have a parent");
                        }
                        parent.removeElement(element);
                        parent.addElement(new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, clone.getKey()));
                        link = clone;
                    }
                    mdelement = link.findMetadataByName(name);
                    break;
                }

                if (mdelement == null) {
                    throw new CoreServiceException("unable to find metadata object into workspace [" + wskey + "] for path [" + npath.build() + "] and name [" + name + "]");
                }
                OrtolangObjectIdentifier identifier = registry.lookup(mdelement.getKey());
                checkObjectType(identifier, MetadataObject.OBJECT_TYPE);
                MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
                if (meta == null) {
                    throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
                }
                meta.setKey(mdelement.getKey());

                if (hash != null && hash.length() > 0) {
                    if (filename != null) {
                        meta.setContentType(binarystore.type(hash, filename));
                    } else {
                        meta.setContentType(binarystore.type(hash));
                    }

                    if (format != null) {
                        meta.setFormat(format);
                    }
                    MetadataFormat metadataFormat = findMetadataFormatById(meta.getFormat());
                    if (metadataFormat == null) {
                        LOGGER.log(Level.SEVERE, "Unable to find a metadata format for name: " + name);
                        throw new CoreServiceException("unknown metadata format for name: " + name);
                    }
                    if (metadataFormat.isValidationNeeded()) {
                        validateMetadata(hash, metadataFormat);
                    }
                    meta.setSize(binarystore.size(hash));
                    meta.setStream(hash);
                    meta.setTarget(tkey);
                } else {
                    throw new CoreServiceException("unable to update a metadata with an empty content (hash is null)");
                }
                em.merge(meta);

                registry.update(mdelement.getKey());
                indexing.index(tkey);

                if (collection != null && purgeChildren) {
                    LOGGER.log(Level.FINE, "Purging children metadata");
                    purgeChildrenMetadata(collection, wskey, path, name);
                }

                ws.setChanged(true);
                em.merge(ws);
                registry.update(ws.getKey());
                LOGGER.log(Level.FINEST, "workspace set changed");

                ArgumentsBuilder argsBuilder = new ArgumentsBuilder(5).addArgument("ws-alias", ws.getAlias()).addArgument("key", mdelement.getKey()).addArgument("tkey", tkey)
                        .addArgument("path", npath.build()).addArgument("name", name);
                notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "update"), argsBuilder.build());

                return meta;
            } else {
                LOGGER.log(Level.FINEST, "no changes detected with current metadata object, nothing to do");
                return cmeta;
            }
        } catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException
                | BinaryStoreServiceException | DataNotFoundException | CloneException | IndexingServiceException | OrtolangException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred during metadata creation", e);
            throw new CoreServiceException("unable to create metadata into workspace [" + wskey + "] for path [" + path + "] and name [" + name + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteMetadataObject(String wskey, String path, String name, boolean recursive) throws CoreServiceException, InvalidPathException, WorkspaceReadOnlyException, PathNotFoundException {
        LOGGER.log(Level.FINE, "deleting metadataobject into workspace [" + wskey + "] for path [" + path + "] with name [" + name + "]");
        try {
            PathBuilder npath = PathBuilder.fromPath(path);
            PathBuilder ppath = npath.clone().parent();

            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);
            LOGGER.log(Level.FINEST, "user [" + caller + "] is authentified");

            OrtolangObjectIdentifier wsidentifier = registry.lookup(wskey);
            checkObjectType(wsidentifier, Workspace.OBJECT_TYPE);
            LOGGER.log(Level.FINEST, "workspace with key [" + wskey + "] exists");

            Workspace ws = em.find(Workspace.class, wsidentifier.getId());
            if (ws == null) {
                throw new CoreServiceException("unable to load workspace with id [" + wsidentifier.getId() + "] from storage");
            }
            if (applyReadOnly(caller, subjects, ws)) {
                throw new WorkspaceReadOnlyException("unable to delete meta data object in workspace with key [" + wskey + "] because it is read only");
            }
            ws.setKey(wskey);
            LOGGER.log(Level.FINEST, "workspace loaded");

            authorisation.checkPermission(ws.getHead(), subjects, "delete");
            LOGGER.log(Level.FINEST, "user [" + caller + "] has 'create' permission on the head collection of this workspace");

            String current = resolveWorkspacePath(wskey, Workspace.HEAD, npath.build());
            MetadataElement cmdelement = loadMetadataElement(name, current);
            if (cmdelement == null) {
                throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for at path [" + npath.build() + "]");
            }
            OrtolangObjectIdentifier cidentifier = registry.lookup(cmdelement.getKey());
            checkObjectType(cidentifier, MetadataObject.OBJECT_TYPE);
            MetadataObject cmeta = em.find(MetadataObject.class, cidentifier.getId());
            if (cmeta == null) {
                throw new CoreServiceException("unable to load metadata with id [" + cidentifier.getId() + "] from storage");
            }

            Collection parent = null;
            CollectionElement element = null;
            String tkey;
            if (npath.isRoot()) {
                tkey = ws.getHead();
            } else {
                parent = loadCollectionAtPath(ws.getHead(), ppath, ws.getClock());
                LOGGER.log(Level.FINEST, "parent collection loaded for path " + ppath.build());
                element = parent.findElementByName(npath.part());
                if (element == null) {
                    throw new PathNotFoundException(npath.build());
                }
                LOGGER.log(Level.FINEST, "collection element found for name " + npath.part());
                tkey = element.getKey();
            }

            OrtolangObjectIdentifier tidentifier = registry.lookup(tkey);
            if (!tidentifier.getType().equals(Link.OBJECT_TYPE) && !tidentifier.getType().equals(Collection.OBJECT_TYPE) && !tidentifier.getType().equals(DataObject.OBJECT_TYPE)) {
                throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
            }

            MetadataElement mdelement;
            switch (tidentifier.getType()) {
            case Collection.OBJECT_TYPE:
                Collection collection = em.find(Collection.class, tidentifier.getId());
                if (collection == null) {
                    throw new CoreServiceException("unable to load collection with id [" + tidentifier.getId() + "] from storage");
                }
                collection.setKey(tkey);
                if (collection.findMetadataByName(name) == null) {
                    throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for collection at path [" + npath.build() + "]");
                }
                if (collection.getClock() < ws.getClock()) {
                    Collection clone = cloneCollection(ws.getHead(), collection, ws.getClock());
                    tkey = clone.getKey();
                    if (parent != null) {
                        parent.removeElement(element);
                        parent.addElement(new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, clone.getKey()));
                    }
                    collection = clone;
                }
                mdelement = collection.findMetadataByName(name);
                if (recursive) {
                    LOGGER.log(Level.FINE, "Removing children metadata");
                    purgeChildrenMetadata(collection, wskey, path, name);
                }
                collection.removeMetadata(mdelement);
                em.merge(collection);
                break;
            case DataObject.OBJECT_TYPE:
                DataObject object = em.find(DataObject.class, tidentifier.getId());
                if (object == null) {
                    throw new CoreServiceException("unable to load object with id [" + tidentifier.getId() + "] from storage");
                }
                object.setKey(tkey);
                if (object.findMetadataByName(name) == null) {
                    throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for object at path [" + npath.build() + "]");
                }
                if (object.getClock() < ws.getClock()) {
                    DataObject clone = cloneDataObject(ws.getHead(), object, ws.getClock());
                    tkey = clone.getKey();
                    if (parent == null) {
                        throw new CoreServiceException("An object should have a parent");
                    }
                    parent.removeElement(element);
                    parent.addElement(new CollectionElement(DataObject.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), clone.getSize(), clone.getMimeType(), clone.getKey()));
                    object = clone;
                }
                mdelement = object.findMetadataByName(name);
                object.removeMetadata(mdelement);
                em.merge(object);
                break;
            case Link.OBJECT_TYPE:
                Link link = em.find(Link.class, tidentifier.getId());
                if (link == null) {
                    throw new CoreServiceException("unable to load link with id [" + tidentifier.getId() + "] from storage");
                }
                link.setKey(tkey);
                if (link.findMetadataByName(name) == null) {
                    throw new CoreServiceException("a metadata object with name [" + name + "] does not exists for link at path [" + npath.build() + "]");
                }
                if (link.getClock() < ws.getClock()) {
                    Link clone = cloneLink(ws.getHead(), link, ws.getClock());
                    tkey = clone.getKey();
                    if (parent == null) {
                        throw new CoreServiceException("A link should have a parent");
                    }
                    parent.removeElement(element);
                    parent.addElement(new CollectionElement(Link.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Link.MIME_TYPE, clone.getKey()));
                    link = clone;
                }
                mdelement = link.findMetadataByName(name);
                link.removeMetadata(mdelement);
                em.merge(link);
                break;
            default:
                throw new CoreServiceException("Metadata target should be a Metadata Source not a " + tidentifier.getType());
            }

            if (mdelement == null) {
                throw new CoreServiceException("unable to find metadata object into workspace [" + wskey + "] for path [" + npath.build() + "] and name [" + name + "]");
            }

            registry.delete(mdelement.getKey());
            indexing.remove(mdelement.getKey());

            indexing.index(tkey);

            ws.setChanged(true);
            em.merge(ws);
            registry.update(ws.getKey());
            LOGGER.log(Level.FINEST, "workspace set changed");

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(5).addArgument("ws-alias", ws.getAlias()).addArgument("key", mdelement.getKey()).addArgument("tkey", tkey)
                    .addArgument("path", npath.build()).addArgument("name", name);
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, "delete"), argsBuilder.build());
        } catch (KeyLockedException | KeyNotFoundException | RegistryServiceException | NotificationServiceException | AuthorisationServiceException | MembershipServiceException | CloneException
                | IndexingServiceException | OrtolangException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred during metadata deletion", e);
            throw new CoreServiceException("unable to delete metadata into workspace [" + wskey + "] for path [" + path + "]", e);
        }
    }

    private MetadataElement loadMetadataElement(String name, String target) throws CoreServiceException, KeyNotFoundException, RegistryServiceException {
        OrtolangObjectIdentifier targetIdentifier = registry.lookup(target);
        switch (targetIdentifier.getType()) {
        case Collection.OBJECT_TYPE:
            Collection collection = em.find(Collection.class, targetIdentifier.getId());
            if (collection == null) {
                throw new CoreServiceException("unable to load collection with id [" + targetIdentifier.getId() + "] from storage");
            }
            return collection.findMetadataByName(name);
        case DataObject.OBJECT_TYPE:
            DataObject object = em.find(DataObject.class, targetIdentifier.getId());
            if (object == null) {
                throw new CoreServiceException("unable to load object with id [" + targetIdentifier.getId() + "] from storage");
            }
            return object.findMetadataByName(name);
        case Link.OBJECT_TYPE:
            Link link = em.find(Link.class, targetIdentifier.getId());
            if (link == null) {
                throw new CoreServiceException("unable to load link with id [" + targetIdentifier.getId() + "] from storage");
            }
            return link.findMetadataByName(name);
        default:
            throw new CoreServiceException("Metadata target should be a Metadata Source not a " + targetIdentifier.getType());
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String readPublicationPolicy(String key) throws KeyNotFoundException, RegistryServiceException, DataNotFoundException, BinaryStoreServiceException, IOException, CoreServiceException {
        OrtolangObjectIdentifier identifier = registry.lookup(key);
        OrtolangObject object;
        switch (identifier.getType()) {
        case Collection.OBJECT_TYPE:
            object = em.find(Collection.class, identifier.getId());
            break;
        case DataObject.OBJECT_TYPE:
            object = em.find(DataObject.class, identifier.getId());
            break;
        case Link.OBJECT_TYPE:
            object = em.find(Link.class, identifier.getId());
            break;
        default:
            throw new CoreServiceException("Cannot read publication policy of an object of type: " + identifier.getType());
        }
        MetadataElement metadataElement = null;
        if (object != null) {
            metadataElement = ((MetadataSource) object).findMetadataByName(MetadataFormat.ACL);
        }
        if (metadataElement != null) {
            OrtolangObjectIdentifier mdIdentifier = registry.lookup(metadataElement.getKey());
            MetadataObject metadataObject = em.find(MetadataObject.class, mdIdentifier.getId());
            ObjectMapper mapper = new ObjectMapper();
            PublicationPolicy publicationPolicy = mapper.readValue(binarystore.getFile(metadataObject.getStream()), PublicationPolicy.class);
            return publicationPolicy.getTemplate();
        }
        return null;
    }

    @Override
    public String readPublicationPolicy(String wskey, String root, String path) throws KeyNotFoundException, RegistryServiceException, DataNotFoundException, BinaryStoreServiceException, IOException,
            CoreServiceException, AccessDeniedException, InvalidPathException, PathNotFoundException {
        String key = resolveWorkspacePath(wskey, root, path);
        String publicationPolicy = readPublicationPolicy(key);
        if (publicationPolicy == null) {
            PathBuilder pathBuilder = PathBuilder.fromPath(path);
            if (pathBuilder.isRoot()) {
                return AuthorisationPolicyTemplate.FORALL;
            }
            return readPublicationPolicy(wskey, root, pathBuilder.parent().build());
        }
        return publicationPolicy;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> findMetadataObjectsForTarget(String target) throws CoreServiceException, AccessDeniedException {
        LOGGER.log(Level.FINE, "finding metadata for target [" + target + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(target, subjects, "read");

            TypedQuery<MetadataObject> query = em.createNamedQuery("findMetadataObjectsForTarget", MetadataObject.class).setParameter("target", target);
            List<MetadataObject> mdos = query.getResultList();
            List<String> results = new ArrayList<String>();
            for (MetadataObject mdo : mdos) {
                String key = registry.lookup(mdo.getObjectIdentifier());
                results.add(key);
            }
            return results;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | IdentifierNotRegisteredException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during finding metadata", e);
            throw new CoreServiceException("unable to find metadata for target [" + target + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<String> findMetadataObjectsForTargetAndName(String target, String name) throws CoreServiceException, AccessDeniedException {
        LOGGER.log(Level.FINE, "finding metadata for target [" + target + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkPermission(target, subjects, "read");

            TypedQuery<MetadataObject> query = em.createNamedQuery("findMetadataObjectsForTargetAndName", MetadataObject.class).setParameter("target", target).setParameter("name", name);
            List<MetadataObject> mdos = query.getResultList();
            List<String> results = new ArrayList<String>();
            for (MetadataObject mdo : mdos) {
                String key = registry.lookup(mdo.getObjectIdentifier());
                results.add(key);
            }
            return results;
        } catch (RegistryServiceException | MembershipServiceException | AuthorisationServiceException | KeyNotFoundException | IdentifierNotRegisteredException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during finding metadata", e);
            throw new CoreServiceException("unable to find metadata for target [" + target + "]", e);
        }
    }

    /* MetadataFormat */

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String createMetadataFormat(String name, String description, String schema, String form, boolean validationNeeded, boolean indexable) throws CoreServiceException {
        LOGGER.log(Level.FINE, "creating metadataformat with name [" + name + "]");
        try {
            MetadataFormat newmdf = new MetadataFormat();
            newmdf.setName(name);
            newmdf.setDescription(description);
            newmdf.setForm(form);
            newmdf.setValidationNeeded(validationNeeded);
            newmdf.setIndexable(indexable);
            if (schema != null && schema.length() > 0) {
                newmdf.setSize(binarystore.size(schema));
                newmdf.setMimeType(binarystore.type(schema));
                newmdf.setSchema(schema);
            } else {
                newmdf.setSize(0);
                newmdf.setMimeType("application/octet-stream");
                newmdf.setSchema("");
            }
            MetadataFormat mdf = getMetadataFormat(name);
            if (mdf != null) {
                if (mdf.equals(newmdf)) {
                    LOGGER.log(Level.INFO, "Already imported metadata format: " + mdf.getId());
                    return mdf.getId();
                }
                LOGGER.log(Level.FINE, "metadata format version already exists, creating new version");
                newmdf.setSerial(mdf.getSerial() + 1);
            }
            newmdf.setId(name + ":" + newmdf.getSerial());
            em.persist(newmdf);
            return newmdf.getId();
        } catch (BinaryStoreServiceException | DataNotFoundException e) {
            LOGGER.log(Level.SEVERE, "unexpected error during create metadata format", e);
            throw new CoreServiceException("unexpected error during create metadata format", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<MetadataFormat> listMetadataFormat() throws CoreServiceException {
        List<MetadataFormat> formats = listAllMetadataFormat();
        Map<String, MetadataFormat> latest = new HashMap<String, MetadataFormat>();
        for (MetadataFormat format : formats) {
            if (!latest.containsKey(format.getName())) {
                latest.put(format.getName(), format);
            } else if (latest.get(format.getName()).getSerial() < format.getSerial()) {
                latest.put(format.getName(), format);
            }
        }
        List<MetadataFormat> filteredformats = new ArrayList<MetadataFormat>();
        filteredformats.addAll(latest.values());
        return filteredformats;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<MetadataFormat> listAllMetadataFormat() {
        TypedQuery<MetadataFormat> query = em.createNamedQuery("listMetadataFormat", MetadataFormat.class);
        return query.getResultList();
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public MetadataFormat getMetadataFormat(String name) {
        LOGGER.log(Level.FINE, "reading metadata format for name [" + name + "]");
        TypedQuery<MetadataFormat> query = em.createNamedQuery("findMetadataFormatForName", MetadataFormat.class).setParameter("name", name);
        List<MetadataFormat> formats = query.getResultList();
        if (formats.isEmpty()) {
            return null;
        }
        return formats.get(0);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public MetadataFormat findMetadataFormatById(String id) throws CoreServiceException {
        MetadataFormat format = em.find(MetadataFormat.class, id);
        if (format == null) {
            throw new CoreServiceException("unable to find a metadata format for id " + id + " in the storage");
        }
        return format;
    }

    private void validateMetadata(String metadata, MetadataFormat format) throws MetadataFormatException {
        try {
            if (format.getSchema() != null && format.getSchema().length() > 0) {
                JsonNode jsonSchema = JsonLoader.fromReader(new InputStreamReader(binarystore.get(format.getSchema())));
                JsonNode jsonFile = JsonLoader.fromReader(new InputStreamReader(binarystore.get(metadata)));
                JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
                JsonSchema schema = factory.getJsonSchema(jsonSchema);

                ProcessingReport report = schema.validate(jsonFile);
                LOGGER.log(Level.FINE, report.toString());

                if (!report.isSuccess()) {
                    LOGGER.log(Level.WARNING, "error during validating metadata format " + format.getName() + ": " + report.toString());
                    throw new MetadataFormatException(report.toString());
                }
            } else {
                LOGGER.log(Level.SEVERE, "unexpected error occurred during validating metadata with metadata format [" + format + "] : schema not found");
                throw new MetadataFormatException("unable to validate metadata with metadata format [" + format + "] : schema not found");
            }
        } catch (IOException | ProcessingException | DataNotFoundException | BinaryStoreServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during validating metadata with metadata format [" + format + "] : " + e.getMessage());
            throw new MetadataFormatException("unable to validate metadata with metadata format [" + format + "] : " + e.getMessage());
        }
    }

    /* BinaryContent */

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public String put(InputStream data) throws CoreServiceException, DataCollisionException {
        LOGGER.log(Level.FINE, "putting binary content in store");
        try {
            return binarystore.put(data);
        } catch (BinaryStoreServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during putting binary content", e);
            throw new CoreServiceException("unable to put binary content", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public InputStream download(String key) throws CoreServiceException, DataNotFoundException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "download content from store for object with key [" + key + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            String hash;
            switch (identifier.getType()) {
            case DataObject.OBJECT_TYPE: {
                authorisation.checkPermission(key, subjects, "download");
                DataObject object = em.find(DataObject.class, identifier.getId());
                if (object == null) {
                    throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
                }
                hash = object.getStream();
                break;
            }
            case MetadataObject.OBJECT_TYPE: {
                authorisation.checkPermission(key, subjects, "read");
                MetadataObject object = em.find(MetadataObject.class, identifier.getId());
                if (object == null) {
                    throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
                }
                hash = object.getStream();
                break;
            }
            default:
                throw new CoreServiceException("unable to find downloadable content for key [" + key + "]");
            }
            if (hash != null && hash.length() > 0) {
                InputStream stream = binarystore.get(hash);
                ArgumentsBuilder argumentsBuilder = new ArgumentsBuilder("hash", hash);
                notification.throwEvent(key, caller, identifier.getType(), OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, identifier.getType(), "download"), argumentsBuilder.build());
                return stream;
            } else {
                throw new DataNotFoundException("there is no preview available for this data object");
            }
        } catch (NotificationServiceException | BinaryStoreServiceException | MembershipServiceException | RegistryServiceException | AuthorisationServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred during getting preview content", e);
            throw new CoreServiceException("unable to get preview content", e);
        }
    }

    /* Service */

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String>();
        try {
            infos.put(INFO_WORKSPACES_ALL, Long.toString(registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE), null)));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_WORKSPACES_ALL, e);
        }
        try {
            infos.put(INFO_COLLECTIONS_ALL, Long.toString(registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE), null)));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_COLLECTIONS_ALL, e);
        }
        try {
            infos.put(INFO_OBJECTS_ALL, Long.toString(registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE), null)));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_OBJECTS_ALL, e);
        }
        try {
            infos.put(INFO_WORKSPACES_PUBLISHED, Long.toString(registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE), Status.PUBLISHED)));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_WORKSPACES_PUBLISHED, e);
        }
        try {
            infos.put(INFO_COLLECTIONS_PUBLISHED, Long.toString(registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE), Status.PUBLISHED)));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_COLLECTIONS_PUBLISHED, e);
        }
        try {
            infos.put(INFO_OBJECTS_PUBLISHED, Long.toString(registry.count(OrtolangObjectIdentifier.buildJPQLFilterPattern(CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE), Status.PUBLISHED)));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_OBJECTS_PUBLISHED, e);
        }
        return infos;
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        for (int i = 0; i < OBJECT_PERMISSIONS_LIST.length; i++) {
            if (OBJECT_PERMISSIONS_LIST[i][0].equals(type)) {
                return OBJECT_PERMISSIONS_LIST[i][1].split(",");
            }
        }
        throw new OrtolangException("Unable to find object permissions list for object type : " + type);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OrtolangObject findObject(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);

            if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            switch (identifier.getType()) {
            case Workspace.OBJECT_TYPE:
                return readWorkspace(key);
            case DataObject.OBJECT_TYPE:
                return readDataObject(key);
            case Collection.OBJECT_TYPE:
                return readCollection(key);
            case Link.OBJECT_TYPE:
                return readLink(key);
            case MetadataObject.OBJECT_TYPE:
                return readMetadataObject(key);
            }

            throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
        } catch (CoreServiceException | RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        LOGGER.log(Level.FINE, "calculating size for object with key [" + key + "]");
        try {
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            OrtolangObjectIdentifier cidentifier = registry.lookup(key);
            if (!cidentifier.getService().equals(CoreService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + cidentifier + " does not refer to service " + getServiceName());
            }
            OrtolangObjectSize ortolangObjectSize = new OrtolangObjectSize();
            switch (cidentifier.getType()) {
            case DataObject.OBJECT_TYPE: {
                authorisation.checkPermission(key, subjects, "read");
                DataObject dataObject = em.find(DataObject.class, cidentifier.getId());
                ortolangObjectSize.addElement(DataObject.OBJECT_TYPE, dataObject.getSize());
                break;
            }
            case Collection.OBJECT_TYPE: {
                ortolangObjectSize = getCollectionSize(key, cidentifier, ortolangObjectSize, subjects);
                break;
            }
            case Workspace.OBJECT_TYPE: {
                authorisation.checkPermission(key, subjects, "read");
                Workspace workspace = em.find(Workspace.class, cidentifier.getId());
                ortolangObjectSize = getCollectionSize(workspace.getHead(), registry.lookup(workspace.getHead()), ortolangObjectSize, subjects);
                for (SnapshotElement snapshotElement : workspace.getSnapshots()) {
                    ortolangObjectSize = getCollectionSize(snapshotElement.getKey(), registry.lookup(snapshotElement.getKey()), ortolangObjectSize, subjects);
                }
                ortolangObjectSize.addElement("members", workspace.getMembers().split(",").length);
                ortolangObjectSize.addElement("snapshots", workspace.getSnapshots().size());
                break;
            }
            }
            return ortolangObjectSize;
        } catch (CoreServiceException | MembershipServiceException | RegistryServiceException | AuthorisationServiceException | AccessDeniedException | KeyNotFoundException e) {
            LOGGER.log(Level.SEVERE, "unexpected error while calculating object size", e);
            throw new OrtolangException("unable to calculate size for object with key [" + key + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<OrtolangObject> findObjectByBinaryHash(String hash) throws OrtolangException {
        try {
            TypedQuery<DataObject> query = em.createNamedQuery("findObjectByBinaryHash", DataObject.class).setParameter("hash", hash);
            List<DataObject> objects = query.getResultList();
            for (DataObject object : objects) {
                object.setKey(registry.lookup(object.getObjectIdentifier()));
            }
            TypedQuery<MetadataObject> query2 = em.createNamedQuery("findMetadataObjectByBinaryHash", MetadataObject.class).setParameter("hash", hash);
            List<MetadataObject> objects2 = query2.getResultList();
            for (MetadataObject mobject : objects2) {
                mobject.setKey(registry.lookup(mobject.getObjectIdentifier()));
            }
            List<OrtolangObject> oobjects = new ArrayList<OrtolangObject>();
            oobjects.addAll(objects);
            oobjects.addAll(objects2);
            return oobjects;
        } catch (Exception e) {
            throw new OrtolangException("unable to find an object for hash " + hash);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IndexablePlainTextContent getIndexablePlainTextContent(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }
            IndexablePlainTextContent content = new IndexablePlainTextContent();

            if (identifier.getType().equals(Workspace.OBJECT_TYPE)) {
                Workspace workspace = em.find(Workspace.class, identifier.getId());
                if (workspace == null) {
                    throw new OrtolangException("unable to load workspace with id [" + identifier.getId() + "] from storage");
                }
                if (workspace.getAlias() != null) {
                    content.addContentPart(workspace.getAlias());
                }
                if (workspace.getName() != null) {
                    content.setName(workspace.getName());
                    content.addContentPart(workspace.getName());
                }
            }

            if (identifier.getType().equals(DataObject.OBJECT_TYPE)) {
                DataObject object = em.find(DataObject.class, identifier.getId());
                if (object == null) {
                    throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
                }
                if (object.getName() != null) {
                    content.setName(object.getName());
                    content.addContentPart(object.getName());
                }
                if (object.getMimeType() != null) {
                    content.addContentPart(object.getMimeType());
                }

                for (MetadataElement mde : object.getMetadatas()) {
                    OrtolangObjectIdentifier mdeIdentifier = registry.lookup(mde.getKey());
                    MetadataObject metadata = em.find(MetadataObject.class, mdeIdentifier.getId());
                    if (metadata == null) {
                        throw new OrtolangException("unable to load metadata with id [" + mdeIdentifier.getId() + "] from storage");
                    }
                    MetadataFormat format = em.find(MetadataFormat.class, metadata.getFormat());
                    if (format == null) {
                        LOGGER.log(Level.WARNING, "unable to get metadata format with id : " + metadata.getFormat());
                        break;
                    }
                    try {
                        if (format.isIndexable() && metadata.getStream() != null && metadata.getStream().length() > 0) {
                            content.addContentPart(binarystore.extract(metadata.getStream()));
                        }
                    } catch (DataNotFoundException | BinaryStoreServiceException e) {
                        LOGGER.log(Level.WARNING, "unable to extract plain text for key : " + mde.getKey(), e);
                    }
                }
            }

            if (identifier.getType().equals(Collection.OBJECT_TYPE)) {
                Collection collection = em.find(Collection.class, identifier.getId());
                if (collection == null) {
                    throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
                }
                if (collection.getName() != null) {
                    content.setName(collection.getName());
                    content.addContentPart(collection.getName());
                }

                for (MetadataElement mde : collection.getMetadatas()) {
                    OrtolangObjectIdentifier mdeIdentifier = registry.lookup(mde.getKey());
                    MetadataObject metadata = em.find(MetadataObject.class, mdeIdentifier.getId());
                    if (metadata == null) {
                        throw new OrtolangException("unable to load metadata with id [" + mdeIdentifier.getId() + "] from storage");
                    }
                    MetadataFormat format = em.find(MetadataFormat.class, metadata.getFormat());
                    if (format == null) {
                        LOGGER.log(Level.WARNING, "unable to get metadata format with id : " + metadata.getFormat());
                        break;
                    }
                    try {
                        if (format.isIndexable() && metadata.getStream() != null && metadata.getStream().length() > 0) {
                            content.addContentPart(binarystore.extract(metadata.getStream()));
                        }
                    } catch (DataNotFoundException | BinaryStoreServiceException e) {
                        LOGGER.log(Level.WARNING, "unable to extract plain text for key : " + mde.getKey(), e);
                    }
                }
            }

            if (identifier.getType().equals(Link.OBJECT_TYPE)) {
                Link link = em.find(Link.class, identifier.getId());
                if (link == null) {
                    throw new OrtolangException("unable to load reference with id [" + identifier.getId() + "] from storage");
                }
                if (link.getName() != null) {
                    content.setName(link.getName());
                    content.addContentPart(link.getName());
                }

                for (MetadataElement mde : link.getMetadatas()) {
                    OrtolangObjectIdentifier mdeIdentifier = registry.lookup(mde.getKey());
                    MetadataObject metadata = em.find(MetadataObject.class, mdeIdentifier.getId());
                    if (metadata == null) {
                        throw new OrtolangException("unable to load metadata with id [" + mdeIdentifier.getId() + "] from storage");
                    }
                    MetadataFormat format = em.find(MetadataFormat.class, metadata.getFormat());
                    if (format == null) {
                        LOGGER.log(Level.WARNING, "unable to get metadata format with id : " + metadata.getFormat());
                        break;
                    }
                    try {
                        if (format.isIndexable() && metadata.getStream() != null && metadata.getStream().length() > 0) {
                            content.addContentPart(binarystore.extract(metadata.getStream()));
                        }
                    } catch (DataNotFoundException | BinaryStoreServiceException e) {
                        LOGGER.log(Level.WARNING, "unable to extract plain text for key : " + mde.getKey(), e);
                    }
                }
            }

            return content;
        } catch (KeyNotFoundException | RegistryServiceException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IndexableJsonContent getIndexableJsonContent(String key) throws OrtolangException, NotIndexableContentException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }
            String publicationStatus = registry.getPublicationStatus(key);
            if (!publicationStatus.equals(OrtolangObjectState.Status.PUBLISHED.value()) && !identifier.getType().equals(Workspace.OBJECT_TYPE)) {
                throw new NotIndexableContentException();
            }
            IndexableJsonContent content = new IndexableJsonContent();

            if (identifier.getType().equals(Collection.OBJECT_TYPE) && publicationStatus.equals(OrtolangObjectState.Status.PUBLISHED.value())) {
                Collection collection = em.find(Collection.class, identifier.getId());
                if (collection == null) {
                    throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
                }

                putMetadataContent(collection, content);
            }

            if (identifier.getType().equals(DataObject.OBJECT_TYPE) && publicationStatus.equals(OrtolangObjectState.Status.PUBLISHED.value())) {
                DataObject object = em.find(DataObject.class, identifier.getId());
                if (object == null) {
                    throw new OrtolangException("unable to load object with id [" + identifier.getId() + "] from storage");
                }

                putMetadataContent(object, content);
            }

            if (identifier.getType().equals(Workspace.OBJECT_TYPE)) {
                Workspace workspace = em.find(Workspace.class, identifier.getId());
                if (workspace == null) {
                    throw new OrtolangException("unable to load workspace with id [" + identifier.getId() + "] from storage");
                }
                if (workspace.hasSnapshot()) {
                    String latestSnapshotName = findWorkspaceLatestPublishedSnapshot(key);
                    JsonObjectBuilder builder = Json.createObjectBuilder();
                    builder.add("wskey", key);
                    builder.add("wsalias", workspace.getAlias());
                    builder.add("archive", workspace.isArchive());
                    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                    JsonObjectBuilder objectBuilder;
                    for (TagElement tagElement : workspace.getTags()) {
                        objectBuilder = Json.createObjectBuilder();
                        objectBuilder.add("name", tagElement.getName());
                        objectBuilder.add("snapshot", tagElement.getSnapshot());
                        objectBuilder.add("key", workspace.findSnapshotByName(tagElement.getSnapshot()).getKey());
                        arrayBuilder.add(objectBuilder);

                        if (tagElement.getSnapshot().equals(latestSnapshotName)) {
                            builder.add("latestSnapshot", OrtolangKeyExtractor.getMarker(workspace.findSnapshotByName(tagElement.getSnapshot()).getKey()));
                        }
                    }
                    builder.add("tags", arrayBuilder);
                    content.put(MetadataFormat.WORKSPACE, builder.build().toString());
                }
            }

            return content;
        } catch (RegistryServiceException | KeyNotFoundException | CoreServiceException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    private void putMetadataContent(MetadataSource source, IndexableJsonContent content) throws OrtolangException, RegistryServiceException, KeyNotFoundException {
    	for (MetadataElement mde : source.getMetadatas()) {
            OrtolangObjectIdentifier mdeIdentifier = registry.lookup(mde.getKey());
            MetadataObject metadata = em.find(MetadataObject.class, mdeIdentifier.getId());
            if (metadata == null) {
                throw new OrtolangException("unable to load metadata with id [" + mdeIdentifier.getId() + "] from storage");
            }
            MetadataFormat format = em.find(MetadataFormat.class, metadata.getFormat());
            if (format == null) {
                LOGGER.log(Level.WARNING, "unable to get metadata format with id : " + metadata.getFormat());
                break;
            }
            try {
                if (format.isIndexable() && metadata.getStream() != null && metadata.getStream().length() > 0) {
                    content.put(metadata.getName(), getContent(binarystore.get(metadata.getStream())));
                }
            } catch (DataNotFoundException | BinaryStoreServiceException | IOException e) {
                LOGGER.log(Level.WARNING, "unable to extract json text for key : " + mde.getKey(), e);
            }
        }
    }
    
    private String getContent(InputStream is) throws IOException {
        String content = null;
        try {
            content = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "  unable to get content from stream", e);
        } finally {
            is.close();
        }
        return content;
    }

    private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws CoreServiceException {
        if (!identifier.getService().equals(getServiceName())) {
            throw new CoreServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
        }

        if (!identifier.getType().equals(objectType)) {
            throw new CoreServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
        }
    }

    @Override
    public OrtolangObjectExportHandler getObjectExportHandler(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            switch (identifier.getType()) {
            case Workspace.OBJECT_TYPE:
                Workspace workspace = em.find(Workspace.class, identifier.getId());
                if (workspace == null) {
                    throw new OrtolangException("unable to load workspace with id [" + identifier.getId() + "] from storage");
                }
                return new WorkspaceExportHandler(workspace);
            case Collection.OBJECT_TYPE:
                Collection collection = em.find(Collection.class, identifier.getId());
                if (collection == null) {
                    throw new OrtolangException("unable to load collection with id [" + identifier.getId() + "] from storage");
                }
                return new CollectionExportHandler(collection);
            case DataObject.OBJECT_TYPE:
                DataObject object = em.find(DataObject.class, identifier.getId());
                if (object == null) {
                    throw new OrtolangException("unable to load dataobject with id [" + identifier.getId() + "] from storage");
                }
                return new DataObjectExportHandler(object);
            case MetadataObject.OBJECT_TYPE:
                MetadataObject metadata = em.find(MetadataObject.class, identifier.getId());
                if (metadata == null) {
                    throw new OrtolangException("unable to load metadata with id [" + identifier.getId() + "] from storage");
                }
                return new MetadataObjectExportHandler(metadata);
            case Link.OBJECT_TYPE:
                Link link = em.find(Link.class, identifier.getId());
                if (link == null) {
                    throw new OrtolangException("unable to load link with id [" + identifier.getId() + "] from storage");
                }
                return new LinkExportHandler(link);
            }

        } catch (RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to build object export handler " + key, e);
        }
        throw new OrtolangException("unable to build object export handler for key " + key);
    }

    @Override
    public OrtolangObjectImportHandler getObjectImportHandler() throws OrtolangException {
        throw new OrtolangException("NOT IMPLEMENTED");
    }

    // System operations
    @Override
    @RolesAllowed({ "admin", "system" })
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Workspace systemReadWorkspace(String wskey) throws CoreServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "#SYSTEM# reading workspace for key [" + wskey + "]");
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            workspace.setKey(wskey);

            return workspace;
        } catch (RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while reading workspace", e);
            throw new CoreServiceException("unable to read workspace with key [" + wskey + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Set<String> systemListWorkspaceKeys(String wskey) throws CoreServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "#SYSTEM# listing workspace keys [" + wskey + "]");
        Set<String> keys = new HashSet<String>();
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);
            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            keys = systemListCollectionKeys(workspace.getHead(), keys);
            for (SnapshotElement snapshot : workspace.getSnapshots()) {
                keys = systemListCollectionKeys(snapshot.getKey(), keys);
            }

            return keys;
        } catch (RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occurred while listing workspace keys", e);
            throw new CoreServiceException("unable to list keys for workspace with key [" + wskey + "]", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private Set<String> systemListCollectionKeys(String key, Set<String> keys) throws RegistryServiceException, KeyNotFoundException, CoreServiceException {
        OrtolangObjectIdentifier cidentifier = registry.lookup(key);
        checkObjectType(cidentifier, Collection.OBJECT_TYPE);
        Collection collection = em.find(Collection.class, cidentifier.getId());
        if (collection == null) {
            throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
        }
        if (keys.add(key)) {
            for (MetadataElement element : collection.getMetadatas()) {
                keys.add(element.getKey());
            }

            for (CollectionElement element : collection.getElements()) {
                keys.add(element.getKey());
                if (element.getType().equals(Collection.OBJECT_TYPE)) {
                    systemListCollectionKeys(element.getKey(), keys);
                }
                if (element.getType().equals(DataObject.OBJECT_TYPE)) {
                    OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
                    checkObjectType(identifier, DataObject.OBJECT_TYPE);
                    DataObject object = em.find(DataObject.class, identifier.getId());
                    if (object == null) {
                        throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
                    }
                    keys.add(element.getKey());
                    for (MetadataElement mde : object.getMetadatas()) {
                        keys.add(mde.getKey());
                    }
                }
                if (element.getType().equals(Link.OBJECT_TYPE)) {
                    OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
                    checkObjectType(identifier, Link.OBJECT_TYPE);
                    Link link = em.find(Link.class, identifier.getId());
                    if (link == null) {
                        throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
                    }
                    keys.add(element.getKey());
                    for (MetadataElement mde : link.getMetadatas()) {
                        keys.add(mde.getKey());
                    }
                }
            }
        }

        return keys;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void systemSetWorkspaceReadOnly(String wskey, boolean readonly) throws CoreServiceException, KeyNotFoundException, NotificationServiceException {
        LOGGER.log(Level.FINE, "#SYSTEM# setting workspace [" + wskey + "] read only to [" + readonly + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            workspace.setReadOnly(readonly);
            em.merge(workspace);

            registry.update(wskey);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(1).addArgument("ws-alias", workspace.getAlias());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, readonly ? "lock" : "unlock"),
                    argsBuilder.build());
        } catch (KeyLockedException | RegistryServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while setting workspace read only mode to [" + readonly + "]", e);
            throw new CoreServiceException("unable to set workspace with key [" + wskey + "] read only mode to  [" + readonly + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void systemUpdateWorkspace(String wskey, String alias, boolean changed, String head, String members, String privileged, boolean readOnly, String type) throws CoreServiceException,
            KeyNotFoundException, NotificationServiceException {
        LOGGER.log(Level.FINE, "#SYSTEM# updating workspace [" + wskey + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            OrtolangObjectIdentifier identifier = registry.lookup(wskey);
            checkObjectType(identifier, Workspace.OBJECT_TYPE);

            Workspace workspace = em.find(Workspace.class, identifier.getId());
            if (workspace == null) {
                throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
            }
            workspace.setAlias(alias);
            workspace.setChanged(changed);
            workspace.setHead(head);
            workspace.setMembers(members);
            workspace.setPrivileged(privileged);
            workspace.setReadOnly(readOnly);
            workspace.setType(type);
            em.merge(workspace);

            registry.update(wskey);

            ArgumentsBuilder argsBuilder = new ArgumentsBuilder(1).addArgument("ws-alias", workspace.getAlias());
            notification.throwEvent(wskey, caller, Workspace.OBJECT_TYPE, OrtolangEvent.buildEventType(CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, "update"), argsBuilder.build());
        } catch (KeyLockedException | RegistryServiceException e) {
            ctx.setRollbackOnly();
            LOGGER.log(Level.SEVERE, "unexpected error occurred while setting workspace alias to [" + alias + "]", e);
            throw new CoreServiceException("unable to set workspace with key [" + wskey + "] alias to  [" + alias + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void systemCreateMetadata(String tkey, String name, String hash, String filename) throws KeyNotFoundException, CoreServiceException, MetadataFormatException, DataNotFoundException,
            BinaryStoreServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException, RegistryServiceException, AuthorisationServiceException, IndexingServiceException {
        LOGGER.log(Level.FINE, "#SYSTEM# create metadata for key [" + tkey + "]");
        if (!name.startsWith("system-")) {
            throw new CoreServiceException("only system metadata can be added this way.");
        }
        OrtolangObjectIdentifier identifier = registry.lookup(tkey);
        if (!identifier.getService().equals(SERVICE_NAME)) {
            throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
        }
        OrtolangObject object;
        switch (identifier.getType()) {
        case DataObject.OBJECT_TYPE:
            object = em.find(DataObject.class, identifier.getId());
            break;
        case Link.OBJECT_TYPE:
            object = em.find(Link.class, identifier.getId());
            break;
        case Collection.OBJECT_TYPE:
            object = em.find(Collection.class, identifier.getId());
            break;
        default:
            throw new CoreServiceException("metadata target can only be a Link, a DataObject or a Collection.");
        }
        MetadataElement metadataElement = ((MetadataSource) object).findMetadataByName(name);
        if (metadataElement != null) {
            ((MetadataSource) object).removeMetadata(metadataElement);
        }
        MetadataObject meta = new MetadataObject();
        meta.setId(UUID.randomUUID().toString());
        meta.setName(name);

        MetadataFormat format = getMetadataFormat(name);
        if (format == null) {
            LOGGER.log(Level.SEVERE, "Unable to find a metadata format for name: " + name);
            throw new CoreServiceException("unknown metadata format for name: " + name);
        }

        if (hash != null && hash.length() > 0) {
            if (format.isValidationNeeded()) {
                validateMetadata(hash, format);
            }
            meta.setSize(binarystore.size(hash));
            if (filename != null) {
                meta.setContentType(binarystore.type(hash, filename));
            } else {
                meta.setContentType(binarystore.type(hash));
            }
            meta.setStream(hash);
        } else {
            meta.setSize(0);
            meta.setContentType("application/octet-stream");
            meta.setStream("");
        }

        meta.setFormat(format.getId());
        meta.setTarget(tkey);
        meta.setKey(UUID.randomUUID().toString());
        em.persist(meta);

        registry.register(meta.getKey(), meta.getObjectIdentifier(), "system");

        registry.refresh(tkey);
        indexing.index(tkey);
        authorisation.clonePolicy(meta.getKey(), tkey);

        ((MetadataSource) object).addMetadata(new MetadataElement(name, meta.getKey()));
        em.merge(object);
    }

    /* ### Internal operations ### */

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private OrtolangObject readObject(String key) throws CoreServiceException, KeyNotFoundException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);

            if (!identifier.getService().equals(CoreService.SERVICE_NAME)) {
                throw new CoreServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            switch (identifier.getType()) {
            case Workspace.OBJECT_TYPE:
                Workspace workspace = em.find(Workspace.class, identifier.getId());
                if (workspace == null) {
                    throw new CoreServiceException("unable to load workspace with id [" + identifier.getId() + "] from storage");
                }
                workspace.setKey(key);
                return workspace;
            case DataObject.OBJECT_TYPE:
                DataObject object = em.find(DataObject.class, identifier.getId());
                if (object == null) {
                    throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
                }
                object.setKey(key);
                return object;
            case Collection.OBJECT_TYPE:
                Collection collection = em.find(Collection.class, identifier.getId());
                if (collection == null) {
                    throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
                }
                collection.setKey(key);
                return collection;
            case Link.OBJECT_TYPE:
                Link link = em.find(Link.class, identifier.getId());
                if (link == null) {
                    throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
                }
                link.setKey(key);
                return link;
            case MetadataObject.OBJECT_TYPE:
                MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
                if (meta == null) {
                    throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
                }
                meta.setKey(key);
                return meta;
            default:
                throw new CoreServiceException("object identifier " + identifier + " refer to an unknown type for service " + getServiceName());
            }

        } catch (RegistryServiceException e) {
            throw new CoreServiceException("unable to read object with key " + key);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private Collection readCollectionAtPath(String root, PathBuilder path) throws PathNotFoundException, CoreServiceException, RegistryServiceException, KeyNotFoundException, InvalidPathException {
        LOGGER.log(Level.FINE, "Reading tree from root [" + root + "] to path [" + path.build() + "]");
        String[] parts = path.buildParts();
        Collection node;
        PathBuilder current = PathBuilder.newInstance();

        OrtolangObjectIdentifier ridentifier = registry.lookup(root);
        checkObjectType(ridentifier, Collection.OBJECT_TYPE);
        node = em.find(Collection.class, ridentifier.getId());
        if (node == null) {
            throw new CoreServiceException("unable to load root collection with id [" + ridentifier.getId() + "] from storage");
        }
        node.setKey(root);
        if (!node.isRoot()) {
            throw new CoreServiceException("root collection [" + root + "] is not flagged as a root collection");
        }

        for (int i = 0; i < parts.length; i++) {
            current.path(parts[i]);
            CollectionElement element = node.findElementByName(parts[i]);
            if (element == null) {
                throw new PathNotFoundException(path.build());
            }
            OrtolangObjectIdentifier cidentifier = registry.lookup(element.getKey());
            checkObjectType(cidentifier, Collection.OBJECT_TYPE);
            node = em.find(Collection.class, cidentifier.getId());
            if (node == null) {
                throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
            }
            node.setKey(element.getKey());
            if (node.isRoot()) {
                LOGGER.log(Level.SEVERE, "WRONG ROOT FLAG found for collection key [" + element.getKey() + "] at path [" + current.build() + "] with root [" + root + "]");
                throw new CoreServiceException("Internal Problem : collection [" + parts[i] + "] is a root collection but is not a root node");
            }
        }

        return node;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private Collection loadCollectionAtPath(String root, PathBuilder path, int clock) throws CoreServiceException, RegistryServiceException, KeyNotFoundException, InvalidPathException,
            PathNotFoundException {
        LOGGER.log(Level.FINE, "Loading tree from root [" + root + "] to path [" + path.build() + "] with clock [" + clock + "]");
        try {
            String[] parts = path.buildParts();
            Collection parent;
            Collection leaf;
            PathBuilder current = PathBuilder.newInstance();

            OrtolangObjectIdentifier ridentifier = registry.lookup(root);
            checkObjectType(ridentifier, Collection.OBJECT_TYPE);
            leaf = em.find(Collection.class, ridentifier.getId());
            if (leaf == null) {
                throw new CoreServiceException("unable to load root collection with id [" + ridentifier.getId() + "] from storage");
            }
            leaf.setKey(root);
            if (!leaf.isRoot()) {
                throw new CoreServiceException("root collection [" + root + "] is not flagged as a root collection");
            }
            if (leaf.getClock() < clock) {
                LOGGER.log(Level.SEVERE, "WRONG CLOCK found for root collection key [ " + root + "]");
                throw new CoreServiceException("root collection [" + root + "] clock is not good");
            }

            for (int i = 0; i < parts.length; i++) {
                parent = leaf;
                current.path(parts[i]);
                CollectionElement element = parent.findElementByName(parts[i]);
                if (element == null) {
                    throw new PathNotFoundException(path.build());
                }
                OrtolangObjectIdentifier cidentifier = registry.lookup(element.getKey());
                checkObjectType(cidentifier, Collection.OBJECT_TYPE);
                leaf = em.find(Collection.class, cidentifier.getId());
                if (leaf == null) {
                    throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
                }
                leaf.setKey(element.getKey());
                if (leaf.isRoot()) {
                    LOGGER.log(Level.SEVERE, "WRONG ROOT FLAG found for collection key [" + element.getKey() + "] at path [" + current.build() + "] with root [" + root + "]");
                    throw new CoreServiceException("Internal Problem : collection [" + parts[i] + "] is a root collection but is not a root node");
                }
                if (leaf.getClock() < clock) {
                    Collection clone = cloneCollection(root, leaf, clock);
                    parent.removeElement(element);
                    CollectionElement celement = new CollectionElement(Collection.OBJECT_TYPE, clone.getName(), System.currentTimeMillis(), 0, Collection.MIME_TYPE, clone.getKey());
                    parent.addElement(celement);
                    registry.update(parent.getKey());
                    leaf = clone;
                }
            }

            return leaf;
        } catch (KeyLockedException | CloneException e) {
            ctx.setRollbackOnly();
            throw new CoreServiceException("unexpected error during loading collection at path " + path.build(), e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private Collection cloneCollection(String root, Collection origin, int clock) throws CloneException {
        LOGGER.log(Level.FINE, "cloning collection for origin [" + origin.getKey() + "]");
        try {
            String key = UUID.randomUUID().toString();

            Collection clone = new Collection();
            clone.setId(UUID.randomUUID().toString());
            clone.setName(origin.getName());
            clone.setRoot(origin.isRoot());
            clone.setClock(clock);
            Set<CollectionElement> elements = new HashSet<CollectionElement>();
            elements.addAll(origin.getElements());
            clone.setElements(elements);
            Set<MetadataElement> metadatas = new HashSet<MetadataElement>();
            for (MetadataElement mde : origin.getMetadatas()) {
                MetadataObject mdclone = cloneMetadataObject(root, mde.getKey(), key);
                metadatas.add(new MetadataElement(mde.getName(), mdclone.getKey()));
            }
            clone.setMetadatas(metadatas);
            clone.setKey(key);
            em.persist(clone);

            registry.register(key, clone.getObjectIdentifier(), origin.getKey(), true);
            indexing.index(key);
            authorisation.clonePolicy(key, root);

            return clone;
        } catch (RegistryServiceException | KeyAlreadyExistsException | IdentifierAlreadyRegisteredException | KeyNotFoundException | AuthorisationServiceException | IndexingServiceException e) {
            throw new CloneException("unable to clone collection with origin key [" + origin + "]", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private DataObject cloneDataObject(String root, DataObject origin, int clock) throws CloneException {
        LOGGER.log(Level.FINE, "cloning object for origin [" + origin.getKey() + "]");
        try {
            String key = UUID.randomUUID().toString();

            DataObject clone = new DataObject();
            clone.setId(UUID.randomUUID().toString());
            clone.setName(origin.getName());
            clone.setSize(origin.getSize());
            clone.setMimeType(origin.getMimeType());
            clone.setStream(origin.getStream());
            clone.setClock(clock);
            Set<MetadataElement> metadatas = new HashSet<MetadataElement>();
            for (MetadataElement mde : origin.getMetadatas()) {
                MetadataObject mdclone = cloneMetadataObject(root, mde.getKey(), key);
                metadatas.add(new MetadataElement(mde.getName(), mdclone.getKey()));
            }
            clone.setMetadatas(metadatas);
            clone.setKey(key);
            em.persist(clone);

            registry.register(key, clone.getObjectIdentifier(), origin.getKey(), true);
            indexing.index(key);
            authorisation.clonePolicy(key, root);

            return clone;
        } catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | KeyAlreadyExistsException | KeyNotFoundException | IndexingServiceException e) {
            throw new CloneException("unable to clone object with origin [" + origin + "]", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private Link cloneLink(String root, Link origin, int clock) throws CloneException {
        LOGGER.log(Level.FINE, "cloning link for origin [" + origin + "]");
        try {
            String key = UUID.randomUUID().toString();

            Link clone = new Link();
            clone.setId(UUID.randomUUID().toString());
            clone.setName(origin.getName());
            clone.setTarget(origin.getTarget());
            clone.setClock(clock);
            Set<MetadataElement> metadatas = new HashSet<MetadataElement>();
            for (MetadataElement mde : origin.getMetadatas()) {
                MetadataObject mdclone = cloneMetadataObject(root, mde.getKey(), key);
                metadatas.add(new MetadataElement(mde.getName(), mdclone.getKey()));
            }
            clone.setMetadatas(metadatas);
            clone.setKey(key);
            em.persist(clone);

            registry.register(key, clone.getObjectIdentifier(), origin.getKey(), true);
            indexing.index(key);
            authorisation.clonePolicy(key, root);

            return clone;
        } catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | KeyAlreadyExistsException | KeyNotFoundException | IndexingServiceException e) {
            throw new CloneException("unable to clone link with origin [" + origin + "]", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private MetadataObject cloneMetadataObject(String root, String origin, String target) throws CloneException {
        LOGGER.log(Level.FINE, "clone metadata for origin [" + origin + "], target [" + target + "]");
        try {
            String key = UUID.randomUUID().toString();

            OrtolangObjectIdentifier identifier = registry.lookup(origin);
            checkObjectType(identifier, MetadataObject.OBJECT_TYPE);

            MetadataObject meta = em.find(MetadataObject.class, identifier.getId());
            if (meta == null) {
                throw new CoreServiceException("unable to load metadata with id [" + identifier.getId() + "] from storage");
            }

            MetadataObject clone = new MetadataObject();
            clone.setId(UUID.randomUUID().toString());
            clone.setName(meta.getName());
            clone.setTarget(target);
            clone.setSize(meta.getSize());
            clone.setFormat(meta.getFormat());
            clone.setContentType(meta.getContentType());
            clone.setStream(meta.getStream());
            clone.setKey(key);
            em.persist(clone);

            registry.register(key, clone.getObjectIdentifier(), origin, true);
            indexing.index(target);
            authorisation.clonePolicy(key, root);

            return clone;
        } catch (RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | CoreServiceException | KeyNotFoundException | KeyAlreadyExistsException
                | IndexingServiceException e) {
            throw new CloneException("unable to clone metadata with origin [" + origin + "] and target [" + target + "]", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    private OrtolangObjectSize getCollectionSize(String key, OrtolangObjectIdentifier cidentifier, OrtolangObjectSize ortolangObjectSize, List<String> subjects) throws KeyNotFoundException,
            RegistryServiceException, CoreServiceException {
        LOGGER.log(Level.FINE, "calculating collection size for collection with key [" + key + "]");
        try {
            authorisation.checkPermission(key, subjects, "read");

            Collection collection = em.find(Collection.class, cidentifier.getId());
            if (collection == null) {
                throw new CoreServiceException("unable to load collection with id [" + cidentifier.getId() + "] from storage");
            }

            for (CollectionElement element : collection.getElements()) {
                if (element.getType().equals(DataObject.OBJECT_TYPE)) {
                    try {
                        authorisation.checkPermission(element.getKey(), subjects, "read");
                        ortolangObjectSize.addElement(element.getType(), element.getSize());
                    } catch (AuthorisationServiceException | AccessDeniedException e) {
                        ortolangObjectSize.setPartial(true);
                    }
                } else if (element.getType().equals(Collection.OBJECT_TYPE)) {
                    ortolangObjectSize.addElement(element.getType(), 0);
                    OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
                    ortolangObjectSize = getCollectionSize(element.getKey(), identifier, ortolangObjectSize, subjects);
                }
            }
        } catch (AuthorisationServiceException | AccessDeniedException e) {
            ortolangObjectSize.setPartial(true);
        }
        return ortolangObjectSize;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void deleteCollectionContent(Collection collection, int clock) throws CoreServiceException, RegistryServiceException, KeyNotFoundException, KeyLockedException, IndexingServiceException {
        LOGGER.log(Level.FINE, "delete content for collection with id [" + collection.getId() + "]");
        for (CollectionElement element : collection.getElements()) {
            switch (element.getType()) {
            case Collection.OBJECT_TYPE: {
                OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
                checkObjectType(identifier, Collection.OBJECT_TYPE);
                Collection coll = em.find(Collection.class, identifier.getId());
                if (coll == null) {
                    throw new CoreServiceException("unable to load collection with id [" + identifier.getId() + "] from storage");
                }
                if (coll.getClock() == clock) {
                    deleteCollectionContent(coll, clock);
                    LOGGER.log(Level.FINEST, "collection clock [" + coll.getClock() + "] is the same, key can be deleted and unindexed");
                    for (MetadataElement mde : coll.getMetadatas()) {
                        registry.delete(mde.getKey());
                    }
                    registry.delete(element.getKey());
                    indexing.remove(element.getKey());
                }
                break;
            }
            case DataObject.OBJECT_TYPE: {
                OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
                checkObjectType(identifier, DataObject.OBJECT_TYPE);
                DataObject object = em.find(DataObject.class, identifier.getId());
                if (object == null) {
                    throw new CoreServiceException("unable to load object with id [" + identifier.getId() + "] from storage");
                }
                if (object.getClock() == clock) {
                    LOGGER.log(Level.FINEST, "object clock [" + object.getClock() + "] is the same, key can be deleted and unindexed");
                    for (MetadataElement mde : object.getMetadatas()) {
                        registry.delete(mde.getKey());
                    }
                    registry.delete(element.getKey());
                    indexing.remove(element.getKey());
                }
                break;
            }
            case Link.OBJECT_TYPE: {
                OrtolangObjectIdentifier identifier = registry.lookup(element.getKey());
                checkObjectType(identifier, Link.OBJECT_TYPE);
                Link link = em.find(Link.class, identifier.getId());
                if (link == null) {
                    throw new CoreServiceException("unable to load link with id [" + identifier.getId() + "] from storage");
                }
                if (link.getClock() == clock) {
                    LOGGER.log(Level.FINEST, "link clock [" + link.getClock() + "] is the same, key can be deleted and unindexed");
                    for (MetadataElement mde : link.getMetadatas()) {
                        registry.delete(mde.getKey());
                    }
                    registry.delete(element.getKey());
                    indexing.remove(element.getKey());
                }
                break;
            }
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void purgeChildrenMetadata(Collection collection, String wskey, String path, String name) throws OrtolangException, KeyNotFoundException, InvalidPathException, PathNotFoundException,
            WorkspaceReadOnlyException, CoreServiceException, RegistryServiceException {
        LOGGER.log(Level.FINE, "purging children metadata for path: " + path + " and name: " + name);
        MetadataElement metadataElement;
        OrtolangObjectIdentifier identifier;
        for (CollectionElement collectionElement : collection.getElements()) {
            metadataElement = null;
            identifier = registry.lookup(collectionElement.getKey());
            switch (collectionElement.getType()) {
            case Collection.OBJECT_TYPE:
                Collection subCollection = em.find(Collection.class, identifier.getId());
                metadataElement = subCollection.findMetadataByName(name);
                LOGGER.log(Level.FINE, "path is a collection, calling recursively purgeChildrenMetadata()");
                purgeChildrenMetadata(subCollection, wskey, path + "/" + subCollection.getName(), name);
                break;
            case DataObject.OBJECT_TYPE:
                DataObject dataObject = em.find(DataObject.class, identifier.getId());
                metadataElement = dataObject.findMetadataByName(name);
                break;
            case Link.OBJECT_TYPE:
                Link link = em.find(Link.class, identifier.getId());
                metadataElement = link.findMetadataByName(name);
                break;
            }
            if (metadataElement != null) {
                LOGGER.log(Level.FINE, "metadata to purge: " + metadataElement.getKey());
                deleteMetadataObject(wskey, path + "/" + collectionElement.getName(), name, false);
            }
        }
    }

    @Override
    public void extractMetadata(String wskey, String path, String mimeType) throws CoreServiceException, OrtolangException, InvalidPathException, PathNotFoundException, ExtractionServiceException,
            KeyNotFoundException {
        String key = resolveWorkspacePath(wskey, Workspace.HEAD, path);
        OrtolangObject object = findObject(key);
        if (object instanceof DataObject) {
            extraction.extract(key);
        } else if (object instanceof Collection) {
            extractMetadata((Collection) object, wskey, path);
        }
    }

    private void extractMetadata(Collection collection, String wskey, String path) throws ExtractionServiceException, CoreServiceException, AccessDeniedException, KeyNotFoundException {
        for (CollectionElement collectionElement : collection.getElements()) {
            if (collectionElement.getType().equals(DataObject.OBJECT_TYPE)) {
                extraction.extract(collectionElement.getKey());
            } else if (collectionElement.getType().equals(Collection.OBJECT_TYPE)) {
                extractMetadata(readCollection(collection.getKey()), wskey, path + "/" + collectionElement.getName());
            }
        }
    }

    private boolean applyReadOnly(String caller, List<String> subjects, Workspace workspace) {
        if (!caller.equals(MembershipService.SUPERUSER_IDENTIFIER) && !subjects.contains(MembershipService.MODERATORS_GROUP_KEY) && workspace.isReadOnly()) {
            return true;
        }
        return false;
    }

    private static class PublicationPolicy {

        private String template;

        public String getTemplate() {
            return template;
        }

    }

}
