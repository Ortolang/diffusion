package fr.ortolang.diffusion.security.authentication;

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
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class UsernamePasswordLoginContextFactory {

	static class NamePasswordCallbackHandler implements CallbackHandler {
		private final String username;
		private final String password;

		private NamePasswordCallbackHandler(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
			for (Callback current : callbacks) {
				if (current instanceof NameCallback) {
					((NameCallback) current).setName(username);
				} else if (current instanceof PasswordCallback) {
					((PasswordCallback) current).setPassword(password.toCharArray());
				} else {
					throw new UnsupportedCallbackException(current);
				}
			}
		}
	}

	static class JBossJaasConfiguration extends Configuration {
		private final String configurationName;

		JBossJaasConfiguration(String configurationName) {
			this.configurationName = configurationName;
		}

		@Override
		public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
			if (!configurationName.equals(name)) {
				throw new IllegalArgumentException("Unexpected configuration name '" + name + "'");
			}

			return new AppConfigurationEntry[] { createClientLoginModuleConfigEntry(), };
		}
		
		private AppConfigurationEntry createClientLoginModuleConfigEntry() {
			Map<String, String> options = new HashMap<String, String>();
			options.put("multi-threaded", "true");
			options.put("restore-login-identity", "true");

			return new AppConfigurationEntry("org.jboss.security.ClientLoginModule", AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
		}
	}

	public static LoginContext createLoginContext(final String username, final String password) throws LoginException {
		final String configurationName = "ortolang";
		
		CallbackHandler cbh = new UsernamePasswordLoginContextFactory.NamePasswordCallbackHandler(username, password);
        Configuration config = new JBossJaasConfiguration(configurationName);

        return new LoginContext(configurationName, new Subject(), cbh, config);
	}

}