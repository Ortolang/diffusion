package fr.ortolang.diffusion.browser;

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
import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.OrtolangObjectProperty;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangObjectVersion;
import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.PropertyNotFoundException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;

public interface BrowserService extends OrtolangService {
	
	public static final String SERVICE_NAME = "browser";
	
	public List<String> list(int limit, int offset, String service, String type, OrtolangObjectState.Status status, boolean itemsOnly) throws BrowserServiceException;
	
	public long count(String service, String type, OrtolangObjectState.Status status, boolean itemOnly) throws BrowserServiceException;
	
	public OrtolangObjectIdentifier lookup(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public List<OrtolangObjectProperty> listProperties(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public OrtolangObjectProperty getProperty(String key, String name) throws BrowserServiceException, KeyNotFoundException, PropertyNotFoundException, AccessDeniedException;
	
	public void setProperty(String key, String name, String value) throws BrowserServiceException, KeyNotFoundException, KeyLockedException, AccessDeniedException;
	
	public OrtolangObjectState getState(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public OrtolangObjectInfos getInfos(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public OrtolangObjectVersion getVersion(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	 
	public List<OrtolangObjectVersion> getHistory(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
	public void reindex(String key) throws BrowserServiceException, KeyNotFoundException, AccessDeniedException;
	
}
