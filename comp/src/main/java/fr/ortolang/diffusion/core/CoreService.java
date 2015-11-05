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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ortolang.diffusion.OrtolangBinaryService;
import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangObjectPid;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

public interface CoreService extends OrtolangService, OrtolangBinaryService, OrtolangIndexableService {

    public static final String SERVICE_NAME = "core";

    public static final String INFO_WORKSPACES_ALL = "workspaces.all";
    //TODO public static final String INFO_WORKSPACES_PUBLISHED = "workspaces.published";
    //TODO public static final String INFO_WORKSPACES_DELETED = "workspaces.deleted";
    public static final String INFO_COLLECTIONS_ALL = "collections.all";
    public static final String INFO_OBJECTS_ALL = "objects.all";
	
	/* Workspace */

    public Workspace createWorkspace(String wskey, String alias, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;

    public Workspace createWorkspace(String wskey, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;

    public Workspace readWorkspace(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public void updateWorkspace(String wskey, String name) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException;
    
    public String snapshotWorkspace(String wskey) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException;

    public void tagWorkspace(String wskey, String tag, String snapshot) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException;

    public void deleteWorkspace(String wskey) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException;

    public void deleteWorkspace(String wskey, boolean force) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, AccessDeniedException;

    public List<String> findWorkspacesForProfile(String profile) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public List<String> findWorkspacesAliasForProfile(String profile) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public Map<String, Map<String, List<String>>> buildWorkspacePublicationMap(String wskey, String snapshot) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public Set<String> buildWorkspaceReviewList(String wskey, String snapshot) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public Set<OrtolangObjectPid> buildWorkspacePidList(String wskey, String tag) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public Map<String, String> listWorkspaceContent(String wskey, String snapshot) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public List<String> listAllWorkspaceAlias() throws CoreServiceException, AccessDeniedException;

    public String resolveWorkspaceAlias(String alias) throws CoreServiceException, AccessDeniedException, AliasNotFoundException;

    public String resolveWorkspacePath(String wskey, String root, String path) throws CoreServiceException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    public String resolveWorkspaceMetadata(String wskey, String root, String path, String name) throws CoreServiceException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    public String findWorkspaceLatestPublishedSnapshot(String wskey) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	/*Collection*/

    public void createCollection(String wskey, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    public void createCollection(String wskey, String key, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    public Collection readCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public void moveCollection(String wskey, String from, String to) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    public void deleteCollection(String wskey, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException, CollectionNotEmptyException;

    public void deleteCollection(String wskey, String path, boolean force) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException, CollectionNotEmptyException;

    public String resolvePathFromCollection(String key, String path) throws KeyNotFoundException, CoreServiceException, AccessDeniedException, PathNotFoundException, InvalidPathException;
	
	/*DataObject*/

    public void createDataObject(String wskey, String path, String hash) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    public void createDataObject(String wskey, String key, String path, String hash) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    public DataObject readDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public void updateDataObject(String wskey, String path, String hash) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    public void moveDataObject(String wskey, String from, String to) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    public void deleteDataObject(String wskey, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException;
	
	/*Link*/

    public void createLink(String wskey, String path, String target) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    public void createLink(String wskey, String key, String path, String target) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    public void updateLink(String wskey, String path, String target)  throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    public void moveLink(String wskey, String from, String to) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, PathAlreadyExistsException, AccessDeniedException;

    public void deleteLink(String wskey, String path) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException;

    public Link readLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public List<String> findLinksForTarget(String target) throws CoreServiceException, AccessDeniedException;

    public String resolveLinkTarget(String target) throws CoreServiceException, AliasNotFoundException, InvalidPathException, KeyNotFoundException, PathNotFoundException, AccessDeniedException;
    
    /*MetadataObject*/

    public void createMetadataObject(String wskey, String path, String name, String hash, String filename) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException, MetadataFormatException;

    public void createMetadataObject(String wskey, String key, String path, String name, String hash, String filename) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException, PathNotFoundException, AccessDeniedException, MetadataFormatException;

    public List<String> findMetadataObjectsForTarget(String target) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public List<String> findMetadataObjectsForTargetAndName(String target, String name) throws CoreServiceException, AccessDeniedException;

    public MetadataObject readMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

    public void updateMetadataObject(String wskey, String path, String name, String hash, String filename) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException, MetadataFormatException;

    public void deleteMetadataObject(String wskey, String path, String name) throws CoreServiceException, WorkspaceReadOnlyException, KeyNotFoundException, InvalidPathException, PathNotFoundException, AccessDeniedException;

	/*MetadataFormat*/

    public String createMetadataFormat(String name, String description, String schema, String form, boolean validationNeeded) throws CoreServiceException;

    public MetadataFormat getMetadataFormat(String name) throws CoreServiceException;

    public MetadataFormat findMetadataFormatById(String id) throws CoreServiceException;

    public List<MetadataFormat> listMetadataFormat() throws CoreServiceException;

    public List<MetadataFormat> listAllMetadataFormat() throws CoreServiceException;
	
	/*BinaryContent*/

    public InputStream download(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException;

    public String put(InputStream data) throws CoreServiceException, DataCollisionException;
	
	/*System*/

    public Set<String> systemListWorkspaceKeys(String wskey) throws CoreServiceException, KeyNotFoundException;
    
    public void systemSetWorkspaceReadOnly(String wskey, boolean readonly) throws CoreServiceException, KeyNotFoundException;

}
