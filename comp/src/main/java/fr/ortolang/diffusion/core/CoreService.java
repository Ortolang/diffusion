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

import fr.ortolang.diffusion.OrtolangBinaryService;
import fr.ortolang.diffusion.OrtolangIndexableService;
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
	public static final String[] OBJECT_TYPE_LIST = new String[] { Workspace.OBJECT_TYPE, DataObject.OBJECT_TYPE, Collection.OBJECT_TYPE, Link.OBJECT_TYPE, MetadataObject.OBJECT_TYPE };
	public static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { 
		{ Workspace.OBJECT_TYPE, "read,update,delete,snapshot" },
		{ DataObject.OBJECT_TYPE, "read,update,delete,download" },
		{ Collection.OBJECT_TYPE, "read,update,delete,download" },
		{ Link.OBJECT_TYPE, "read,update,delete,download" },
		{ MetadataObject.OBJECT_TYPE, "read,update,delete,download"}}; 
	
	/* Workspace */
	
	public void createWorkspace(String workspace, String id, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;
	
	public void createWorkspace(String workspace, String name, String type) throws CoreServiceException, KeyAlreadyExistsException, AccessDeniedException;

	public Workspace readWorkspace(String workspace) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

	public void updateWorkspace(String workspace, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;

	public void snapshotWorkspace(String workspace, String name) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void deleteWorkspace(String workspace) throws CoreServiceException, KeyNotFoundException, KeyNotFoundException, AccessDeniedException;

	public List<String> findWorkspacesForProfile(String profile) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public String resolveWorkspacePath(String workspace, String root, String path) throws CoreServiceException, InvalidPathException, AccessDeniedException;
	
	public String resolveWorkspaceMetadata(String workspace, String root, String path, String name) throws CoreServiceException, InvalidPathException, AccessDeniedException;
	
	/*Collection*/
	
	public void createCollection(String workspace, String path, String description) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public Collection readCollection(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateCollection(String workspace, String path, String description) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void moveCollection(String workspace, String from, String to) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void deleteCollection(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;

	public String resolvePathFromCollection(String key, String path) throws KeyNotFoundException, CoreServiceException, AccessDeniedException, InvalidPathException;
	
	/*DataObject*/
	
	public void createDataObject(String workspace, String path, String description, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public DataObject readDataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateDataObject(String workspace, String path, String description, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void moveDataObject(String workspace, String from, String to) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void deleteDataObject(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	/*Link*/
	
	public void createLink(String workspace, String path, String target) throws CoreServiceException, KeyNotFoundException, KeyAlreadyExistsException, InvalidPathException, AccessDeniedException;
	
	public List<String> findLinksForTarget(String target) throws CoreServiceException, AccessDeniedException;
	
	public void moveLink(String workspace, String from, String to) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void deleteLink(String workspace, String path) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public Link readLink(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	/*MetadataObject*/
	
	public void createMetadataObject(String workspace, String path, String name, String format, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public List<String> findMetadataObjectsForTarget(String target) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public MetadataObject readMetadataObject(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void updateMetadataObject(String workspace, String path, String name, String format, String hash) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;
	
	public void deleteMetadataObject(String workspace, String path, String name) throws CoreServiceException, KeyNotFoundException, InvalidPathException, AccessDeniedException;

	/*MetadataFormat*/
	
	public List<MetadataFormat> listMetadataFormat() throws CoreServiceException;
	
	public MetadataFormat readMetadataFormat(String key) throws CoreServiceException, KeyNotFoundException;
	
	public void createMetadataFormat(String name, String description, String hash) throws CoreServiceException;

	public List<String> findMetadataFormatByName(String name) throws CoreServiceException, KeyNotFoundException;
	
	public boolean validateMetadata(MetadataObject metadata, String metadataFormat) throws CoreServiceException, KeyNotFoundException, AccessDeniedException;
	
	/*BinaryContent*/
	
	public InputStream download(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException;
	
	public InputStream preview(String key) throws CoreServiceException, KeyNotFoundException, AccessDeniedException, DataNotFoundException;
	
	public String put(InputStream data) throws CoreServiceException, DataCollisionException;
	
}
