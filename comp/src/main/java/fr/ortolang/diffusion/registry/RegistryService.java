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

import javax.ejb.Local;

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;


/**
 * <p>
 * <b>RegistryService</b> for ORTOLANG Diffusion Server.<br/>
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
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
@Local
public interface RegistryService {
	
	public static final String SERVICE_NAME = "registry";
	
	public void register(String key, OrtolangObjectIdentifier identifier, String author) throws RegistryServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException;
	
	public void register(String key, OrtolangObjectIdentifier identifier, String parent, boolean inherit) throws RegistryServiceException, KeyAlreadyExistsException, IdentifierAlreadyRegisteredException, KeyNotFoundException;
	
	public void update(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	public long getCreationDate(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public long getLastModificationDate(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public String getAuthor(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public void hide(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	public void show(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	public boolean isHidden(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public void delete(String key) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	public void delete(String key, boolean force) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	public void lock(String key, String owner) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	public boolean isLocked(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public String getLock(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public void setPublicationStatus(String key, String status) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	public String getPublicationStatus(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public boolean hasChildren(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public String getChildren(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public String getParent(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public void setProperty(String key, String name, String value) throws RegistryServiceException, KeyNotFoundException, KeyLockedException;
	
	public String getProperty(String key, String name) throws RegistryServiceException, KeyNotFoundException, PropertyNotFoundException;
	
	public List<OrtolangObjectProperty> getProperties(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public OrtolangObjectIdentifier lookup(String key) throws RegistryServiceException, KeyNotFoundException;
	
	public String lookup(OrtolangObjectIdentifier identifier) throws RegistryServiceException, IdentifierNotRegisteredException;
	
	public List<String> list(int offset, int limit, String identifierFilter, OrtolangObjectState.Status statusFilter) throws RegistryServiceException;
	
	public long count(String identifierFilter, OrtolangObjectState.Status statusFilter) throws RegistryServiceException;
	
}