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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ortolang.diffusion.*;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.extraction.ExtractionServiceException;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

import org.javers.core.diff.Change;

public interface CoreService extends OrtolangObjectProviderService, OrtolangBinaryService, OrtolangIndexableService {

    String SERVICE_NAME = "core";

    String INFO_WORKSPACES_ALL = "workspaces.all";
    String INFO_WORKSPACES_PUBLISHED = "workspaces.published";
    // String INFO_WORKSPACES_DELETED = "workspaces.deleted";
    String INFO_COLLECTIONS_ALL = "collections.all";
    String INFO_COLLECTIONS_PUBLISHED = "collections.published";
    // String INFO_COLLECTIONS_DELETED = "collections.deleted";
    String INFO_OBJECTS_ALL = "objects.all";
    String INFO_OBJECTS_PUBLISHED = "objects.published";

    // String INFO_OBJECTS_DELETED = "objects.deleted";

    /* Workspace */

    Workspace createWorkspace(String wskey, String alias, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, AliasAlreadyExistsException;

    Workspace createWorkspace(String wskey, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException, AliasAlreadyExistsException;

    Workspace readWorkspace(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    void updateWorkspace(String wskey, String name) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException;

    Workspace archiveWorkspace(String wskey, Boolean archive) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, WorkspaceReadOnlyException;

    String snapshotWorkspace(String wskey) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException, WorkspaceUnchangedException;

    String getLatestSnapshot(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    void deleteWorkspace(String wskey) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException;

    void deleteWorkspace(String wskey, boolean force) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException;

    List<String> findWorkspacesForProfile(String profile) throws CoreServiceException, AccessDeniedException;

    List<String> findWorkspacesAliasForProfile(String profile) throws CoreServiceException, AccessDeniedException;

    Map<String, Map<String, List<String>>> buildWorkspacePublicationMap(String wskey, String snapshot) throws CoreServiceException;

    Set<String> buildWorkspaceReviewList(String wskey, String snapshot) throws CoreServiceException, AccessDeniedException;

    Set<OrtolangObjectPid> buildWorkspacePidList(String wskey, String tag) throws CoreServiceException;

    Map<String, String> listWorkspaceContent(String wskey, String snapshot) throws CoreServiceException;

    List<Change> diffWorkspaceContent(String wskey, String lsnapshot, String rsnapshot) throws CoreServiceException;

    List<String> listAllWorkspaceAlias() throws CoreServiceException, AccessDeniedException;

    String resolveWorkspaceAlias(String alias) throws CoreServiceException, AliasNotFoundException;

    String resolveWorkspacePath(String wskey, String root, String path) throws CoreServiceException, InvalidPathException, PathNotFoundException;

    String resolveWorkspaceMetadata(String wskey, String root, String path, String name) throws CoreServiceException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    String findWorkspaceLatestPublishedSnapshot(String wskey) throws CoreServiceException, KeyNotFoundException;

    void changeWorkspaceOwner(String wskey, String newOwner) throws CoreServiceException;

    void notifyWorkspaceOwner(String wskey, String sender, String message) throws CoreServiceException;

    void notifyWorkspaceMembers(String wskey, String sender, String message) throws CoreServiceException;

    void moveElements(String wskey, List<String> sources, String destination) throws InvalidPathException, CoreServiceException, PathNotFoundException, AccessDeniedException, KeyNotFoundException,
            RegistryServiceException, WorkspaceReadOnlyException, PathAlreadyExistsException;

    void deleteElements(String wskey, List<String> sources, boolean force) throws InvalidPathException, CoreServiceException, PathNotFoundException, AccessDeniedException, KeyNotFoundException,
            WorkspaceReadOnlyException, CollectionNotEmptyException, RegistryServiceException;

    Group addMember(String wskey, String member) throws CoreServiceException, AccessDeniedException, KeyNotFoundException, MembershipServiceException, NotificationServiceException;

    /* Collection */

    Collection createCollection(String wskey, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException,
            PathAlreadyExistsException, AccessDeniedException, KeyAlreadyExistsException;

    Collection createCollection(String wskey, String key, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyAlreadyExistsException, InvalidPathException, PathNotFoundException,
            PathAlreadyExistsException, AccessDeniedException;

    Collection readCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    Collection moveCollection(String wskey, String from, String to) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException,
            PathAlreadyExistsException, AccessDeniedException;

    void deleteCollection(String wskey, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException,
            CollectionNotEmptyException;

    void deleteCollection(String wskey, String path, boolean force) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException,
            AccessDeniedException, CollectionNotEmptyException;

    String resolvePathFromCollection(String key, String path) throws KeyNotFoundException, CoreServiceException, AccessDeniedException, PathNotFoundException, InvalidPathException;

    /* DataObject */

    DataObject createDataObject(String wskey, String path, String hash) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException,
            PathAlreadyExistsException, AccessDeniedException, KeyAlreadyExistsException;

    DataObject createDataObject(String wskey, String key, String path, String hash) throws CoreServiceException, WorkspaceReadOnlyException, KeyAlreadyExistsException, InvalidPathException,
            PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    DataObject readDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    DataObject updateDataObject(String wskey, String path, String hash) throws CoreServiceException, WorkspaceReadOnlyException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    DataObject moveDataObject(String wskey, String from, String to) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException,
            PathAlreadyExistsException, AccessDeniedException;

    void deleteDataObject(String wskey, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    /* Link */

    Link createLink(String wskey, String path, String target) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException,
            PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    Link createLink(String wskey, String key, String path, String target) throws CoreServiceException, WorkspaceReadOnlyException, KeyAlreadyExistsException, InvalidPathException,
            PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    Link updateLink(String wskey, String path, String target) throws CoreServiceException, WorkspaceReadOnlyException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    Link moveLink(String wskey, String from, String to) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException,
            PathAlreadyExistsException, AccessDeniedException;

    void deleteLink(String wskey, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    Link readLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    List<String> findLinksForTarget(String target) throws CoreServiceException;

    String resolveLinkTarget(String target) throws CoreServiceException, AliasNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    /* MetadataObject */

    MetadataObject createMetadataObject(String wskey, String path, String name, String hash, String filename, boolean purgeChildren) throws CoreServiceException, WorkspaceReadOnlyException,
            KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException, MetadataFormatException, KeyAlreadyExistsException;

    MetadataObject createMetadataObject(String wskey, String key, String path, String name, String hash, String filename, boolean purgeChildren) throws CoreServiceException,
            WorkspaceReadOnlyException, KeyAlreadyExistsException, InvalidPathException, PathNotFoundException, MetadataFormatException;

    List<String> findMetadataObjectsForTarget(String target) throws CoreServiceException, AccessDeniedException;

    List<String> findMetadataObjectsForTargetAndName(String target, String name) throws CoreServiceException, AccessDeniedException;

    MetadataObject readMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    MetadataObject updateMetadataObject(String wskey, String path, String name, String hash, String filename, boolean purgeChildren) throws CoreServiceException, WorkspaceReadOnlyException,
            KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException, MetadataFormatException;

    MetadataObject updateMetadataObject(String wskey, String path, String name, String hash, String filename, boolean purgeChildren, String format) throws CoreServiceException,
            WorkspaceReadOnlyException, InvalidPathException, PathNotFoundException, MetadataFormatException;

    void deleteMetadataObject(String wskey, String path, String name, boolean recursive) throws CoreServiceException, WorkspaceReadOnlyException, InvalidPathException, PathNotFoundException;

    String readPublicationPolicy(String key) throws KeyNotFoundException, RegistryServiceException, DataNotFoundException, BinaryStoreServiceException, IOException, CoreServiceException;

    String readPublicationPolicy(String wskey, String root, String path) throws KeyNotFoundException, RegistryServiceException, DataNotFoundException, BinaryStoreServiceException, IOException,
            CoreServiceException, AccessDeniedException, InvalidPathException, PathNotFoundException;

    /* MetadataFormat */

    String createMetadataFormat(String name, String description, String schema, String form, boolean validationNeeded, boolean indexable) throws CoreServiceException;

    MetadataFormat getMetadataFormat(String name);

    MetadataFormat findMetadataFormatById(String id) throws CoreServiceException;

    List<MetadataFormat> listMetadataFormat() throws CoreServiceException;

    List<MetadataFormat> listAllMetadataFormat();

    /* BinaryContent */

    InputStream download(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException;

    String put(InputStream data) throws CoreServiceException, DataCollisionException;

    void extractMetadata(String wskey, String path, String mimeType) throws CoreServiceException, OrtolangException, InvalidPathException, PathNotFoundException, ExtractionServiceException,
            KeyNotFoundException;

    /* System */

    Set<String> systemListWorkspaceKeys(String wskey) throws CoreServiceException, KeyNotFoundException;

    void systemSetWorkspaceReadOnly(String wskey, boolean readonly) throws CoreServiceException, KeyNotFoundException, NotificationServiceException;

    void systemUpdateWorkspace(String wskey, String alias, boolean changed, String head, String members, String privileged, boolean readOnly, String type) throws CoreServiceException,
            KeyNotFoundException, NotificationServiceException;

    void systemTagWorkspace(String wskey, String tag, String snapshot) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException;

    void systemCreateMetadata(String key, String name, String hash, String filename) throws KeyNotFoundException, CoreServiceException, MetadataFormatException, DataNotFoundException,
            BinaryStoreServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException, RegistryServiceException, AuthorisationServiceException, IndexingServiceException;

    Workspace systemReadWorkspace(String wskey) throws CoreServiceException, KeyNotFoundException;

    List<String> systemFindWorkspacesForProfile(String profile) throws CoreServiceException;
}
