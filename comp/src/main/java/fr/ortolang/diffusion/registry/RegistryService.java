package fr.ortolang.diffusion.registry;

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

import java.util.List;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.entity.RegistryEntry;


/**
 * <p>
 * <b>RegistryService</b> for ORTOLANG Diffusion Server.<br>
 * This service is central to the platform and holds all references to objects managed by diffusion platform.
 * Services are responsible for registering their objects in this registry using the provided API.
 * Services must comply with the DiffusionObjectIdentifier format in order to provide object references that allows
 * the platform to retrieve the good object service and type.  
 * </p>
 * <p>
 * The name used for registry storage concerns the services that are using this registry. The registry is only here to 
 * avoid duplication of registered keys.
 * </p>
 * 
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @version 1.0
 */
public interface RegistryService extends OrtolangService {
	
	String SERVICE_NAME = "registry";
	
	String INFO_SIZE = "entries.all";
	String INFO_DELETED = "entries.deleted";
	String INFO_HIDDEN = "entries.hidden";
	String INFO_PUBLISHED = "entries.published";
    
	void register(String key, OrtolangObjectIdentifier identifier, String author) throws RegistryServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException;
	
	void register(String key, OrtolangObjectIdentifier identifier, String parent, boolean inherit) throws RegistryServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException, KeyNotFoundException;
	
	void update(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	long getCreationDate(String key) throws RegistryServiceException, KeyNotFoundException;
	
	long getLastModificationDate(String key) throws RegistryServiceException, KeyNotFoundException;
	
	String getAuthor(String key) throws RegistryServiceException, KeyNotFoundException;
	
	void hide(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	void show(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	boolean isHidden(String key) throws RegistryServiceException, KeyNotFoundException;
	
	void delete(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	void delete(String key, boolean force) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	void lock(String key, String owner) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	void unlock(String key, String owner) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	boolean isLocked(String key) throws RegistryServiceException, KeyNotFoundException;
	
	String getLock(String key) throws RegistryServiceException, KeyNotFoundException;
	
	void setPublicationStatus(String key, String status) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	String getPublicationStatus(String key) throws RegistryServiceException, KeyNotFoundException;
	
	boolean hasChildren(String key) throws RegistryServiceException, KeyNotFoundException;
	
	String getChildren(String key) throws RegistryServiceException, KeyNotFoundException;
	
	String getParent(String key) throws RegistryServiceException, KeyNotFoundException;
	
	void setProperty(String key, String name, String value) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	String getProperty(String key, String name) throws RegistryServiceException, KeyNotFoundException, PropertyNotFoundException;
	
	List<OrtolangObjectProperty> getProperties(String key) throws RegistryServiceException, KeyNotFoundException;
	
	OrtolangObjectIdentifier lookup(String key) throws RegistryServiceException, KeyNotFoundException;
	
	String lookup(OrtolangObjectIdentifier identifier) throws RegistryServiceException, IdentifierNotRegisteredException;
	
	List<String> list(int offset, int limit, String identifierFilter, OrtolangObjectState.Status statusFilter) throws RegistryServiceException;
	
	long count(String identifierFilter, OrtolangObjectState.Status statusFilter) throws RegistryServiceException;
	

	List<RegistryEntry> systemListEntries(String keyFilter, String identifierFilter) throws RegistryServiceException;
	
	RegistryEntry systemReadEntry(String key) throws RegistryServiceException, KeyNotFoundException;
	
	long systemCountAllEntries(String identifierFilter) throws RegistryServiceException;
    
    long systemCountDeletedEntries(String identifierFilter) throws RegistryServiceException;
    
    long systemCountHiddenEntries(String identifierFilter) throws RegistryServiceException;
    	
}