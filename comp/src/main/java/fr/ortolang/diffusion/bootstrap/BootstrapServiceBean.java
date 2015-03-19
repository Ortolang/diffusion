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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.RunAsPrincipal;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.InvalidPathException;
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
import fr.ortolang.diffusion.store.binary.DataCollisionException;

@Startup
@Singleton(name = BootstrapService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
@RunAs("user")
@RunAsPrincipal(value = MembershipService.SUPERUSER_IDENTIFIER)
public class BootstrapServiceBean implements BootstrapService {

	private static Logger logger = Logger.getLogger(BootstrapServiceBean.class.getName());

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
	@Resource
	private SessionContext ctx;

	public BootstrapServiceBean() {
		logger.log(Level.FINE, "new bootstrap service instance created");
	}

	@PostConstruct
	public void init() throws Exception {
		bootstrap();
	}

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void bootstrap() throws BootstrapServiceException {
		logger.log(Level.INFO, "checking bootstrap status...");
		try {
			registry.lookup(BootstrapService.WORKSPACE_KEY);
			logger.log(Level.INFO, "bootstrap key found, nothing to do.");
		} catch (KeyNotFoundException e) {
			try {
				logger.log(Level.INFO, "bootstrap key not found, bootstraping plateform...");

				Map<String, List<String>> anonReadRules = new HashMap<String, List<String>>();
				anonReadRules.put(MembershipService.UNAUTHENTIFIED_IDENTIFIER, Arrays.asList(new String[] { "read" }));

				logger.log(Level.FINE, "creating root profile");
				membership.createProfile(MembershipService.SUPERUSER_IDENTIFIER, "Super", "User", "root@ortolang.org", ProfileStatus.ACTIVATED);
				membership.addProfilePublicKey(MembershipService.SUPERUSER_IDENTIFIER, "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDqv8kWdXIgWbFJfOiu9fQYiazwfnpZogatgo3278PIAQ4eaj6p+sjQMQX0hew+rHXCqvi6FaG6Lql7fkJv/NJpSyhyCKYNCxmYKwUvaViOuRLDmnpziEX39WDmiWBE0Q+DNuKIZMx3yZNX/BeBp0FfooKkCnZWEDo4pzcYVp2RlwZuEDZQcQ6KP2S9+z2WQPmsE9tcyPNL12hp8tiG8J/XsPXxnn1mgJxyiwQmEYDxXZTAazeewqftz4GU3Xc9qWOa4GXK/2l0GB/XVuFLoyrXve+hnsiFpeIslJuGl0+AAX+lCULjDcA72r4aT30Z4HV+wxiQxk/j+2CtCw/vfeit achile-laptop");

				logger.log(Level.FINE, "creating anonymous profile");
				membership.createProfile(MembershipService.UNAUTHENTIFIED_IDENTIFIER, "Anonymous", "User", "anon@ortolang.org", ProfileStatus.ACTIVATED);
				logger.log(Level.FINE, "change owner of anonymous profile to root and set anon read rules");
				authorisation.updatePolicyOwner(MembershipService.UNAUTHENTIFIED_IDENTIFIER, MembershipService.SUPERUSER_IDENTIFIER);
				authorisation.setPolicyRules(MembershipService.UNAUTHENTIFIED_IDENTIFIER, anonReadRules);

				logger.log(Level.FINE, "creating moderators group");
				membership.createGroup(MembershipService.MODERATOR_GROUP_KEY, "Publication Moderators", "Moderators of the plateform can publish content");
				membership.addMemberInGroup(MembershipService.MODERATOR_GROUP_KEY, MembershipService.SUPERUSER_IDENTIFIER);
				logger.log(Level.FINE, "set anon read rules");
				authorisation.setPolicyRules(MembershipService.MODERATOR_GROUP_KEY, anonReadRules);

				logger.log(Level.FINE, "create system workspace");
				core.createWorkspace(BootstrapService.WORKSPACE_KEY, "System Workspace", "system");
				Properties props = new Properties();
				props.setProperty("bootstrap.status", "done");
				props.setProperty("bootstrap.timestamp", System.currentTimeMillis() + "");
				props.setProperty("bootstrap.version", BootstrapService.VERSION);
				String hash = core.put(new ByteArrayInputStream(props.toString().getBytes()));
				core.createDataObject(BootstrapService.WORKSPACE_KEY, "/bootstrap.txt", "bootstrap file", hash);

				logger.log(Level.FINE, "import process types");
				runtime.importProcessTypes();

				logger.log(Level.FINE, "import forms");
				logger.log(Level.FINE, "import form : test-process-start-form");
				InputStream is = getClass().getClassLoader().getResourceAsStream("forms/test-process-start-form.json");
				String jsonDefinition = IOUtils.toString(is);
				form.createForm("test-process-start-form", "Test Process Start Form", jsonDefinition);
				logger.log(Level.FINE, "import form : test-process-confirm-form");
				InputStream is2 = getClass().getClassLoader().getResourceAsStream("forms/test-process-confirm-form.json");
				String jsonDefinition2 = IOUtils.toString(is2);
				form.createForm("test-process-confirm-form", "Test Process Confirm Form", jsonDefinition2);

				logger.log(Level.FINE, "import schemas");
				logger.log(Level.FINE, "import schema : ortolang-item");
				InputStream schemaInputStream = getClass().getClassLoader().getResourceAsStream("schema/ortolang-item-schema.json");
				String schemaHash = core.put(schemaInputStream);
				core.createMetadataFormat("ortolang-item-json", schemaHash);
				
				logger.log(Level.INFO, "bootstrap done.");
			} catch (MembershipServiceException | ProfileAlreadyExistsException | AuthorisationServiceException | CoreServiceException | KeyAlreadyExistsException | IOException
					| AccessDeniedException | KeyNotFoundException | InvalidPathException | DataCollisionException | RuntimeServiceException | FormServiceException e1) {
				logger.log(Level.SEVERE, "unexpected error occured while bootstraping plateform", e1);
				throw new BootstrapServiceException("unable to bootstrap plateform", e1);
			}
		} catch (RegistryServiceException e) {
			throw new BootstrapServiceException("unable to check plateform bootstrap status", e);
		}
	}

}
