package fr.ortolang.diffusion.security.authorisation;

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
import java.util.Map;
import java.util.Set;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.security.authorisation.entity.AuthorisationPolicy;
import fr.ortolang.diffusion.security.authorisation.entity.AuthorisationPolicyTemplate;

public interface AuthorisationService extends OrtolangService {
	
	String SERVICE_NAME = "authorisation";
	
	void createPolicyTemplate(String name, String description, String policykey) throws AuthorisationServiceException;
	
	AuthorisationPolicyTemplate getPolicyTemplate(String name) throws AuthorisationServiceException;
	
	List<AuthorisationPolicyTemplate> listPolicyTemplates() throws AuthorisationServiceException;
	
	boolean isPolicyTemplateExists(String name) throws AuthorisationServiceException;
	
	void createPolicy(String key, String owner) throws AuthorisationServiceException;
	
	void clonePolicy(String key, String origin) throws AuthorisationServiceException;
	
	void copyPolicy(String from, String to) throws AuthorisationServiceException;
	
	void copyPolicy(String from, Set<String> to) throws AuthorisationServiceException;
	
	void updatePolicyOwner(String key, String owner) throws AuthorisationServiceException;
	
	String getPolicyOwner(String key) throws AuthorisationServiceException;
	
	void setPolicyRules(String key, Map<String, List<String>> rules) throws AuthorisationServiceException;
	
	Map<String, List<String>> getPolicyRules(String key) throws AuthorisationServiceException;
	
	void checkPermission(String key, List<String> subjects, String permission) throws AuthorisationServiceException, AccessDeniedException;
	
	void checkOwnership(String key, List<String> subjects) throws AuthorisationServiceException, AccessDeniedException;
	
	void checkAuthentified(List<String> subjects) throws AuthorisationServiceException, AccessDeniedException;
	
	void checkSuperUser(String identifier) throws AuthorisationServiceException, AccessDeniedException;
	
	void systemRestorePolicy(AuthorisationPolicy policy, boolean override) throws AuthorisationServiceException;
	
}
