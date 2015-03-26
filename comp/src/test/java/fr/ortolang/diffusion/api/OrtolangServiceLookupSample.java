package fr.ortolang.diffusion.api;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.junit.Test;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;

public class OrtolangServiceLookupSample {
	
	private static final Logger LOGGER = Logger.getLogger(OrtolangServiceLookupSample.class.getName());

	@Test
	public void testListServices() throws NamingException {
		LOGGER.log(Level.INFO, "Ortolang available services : ");
		Properties jndiProps = new Properties();
		jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
		jndiProps.put(Context.PROVIDER_URL,"http-remoting://localhost:8080");
		jndiProps.put("jboss.naming.client.ejb.context", true);
		Context jndi = new InitialContext(jndiProps);
		NamingEnumeration<NameClassPair> enumeration = jndi.list("diffusion-server/components");
		List<String> results = new ArrayList<String>();
		while (enumeration.hasMoreElements()) {
			String name = ((NameClassPair) enumeration.next()).getName();
			if (name.endsWith("Service")) {
				LOGGER.log(Level.INFO, "jndi service name found : " + name);
				results.add(name.substring(0, name.indexOf("!")));
			}
		}
	}
	
	@Test
	public void testFindServiceByServiceName() throws NamingException, BrowserServiceException {
		Properties jndiProps = new Properties();
		jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
		jndiProps.put(Context.PROVIDER_URL,"http-remoting://localhost:8080");
		jndiProps.put("jboss.naming.client.ejb.context", true);
		Context jndi = new InitialContext(jndiProps);
		NamingEnumeration<NameClassPair> enumeration = jndi.list("diffusion-server/components");
		while (enumeration.hasMoreElements()) {
			String name = ((NameClassPair) enumeration.next()).getName();
			if (name.endsWith("Service")) {
				if (name.substring(0, name.indexOf("!")).equals("browser")) {
					OrtolangService service = (OrtolangService) jndi.lookup("diffusion-server/components/" + name);
					LOGGER.log(Level.INFO, service.getServiceName());
					BrowserService browser = (BrowserService) service;
					LOGGER.log(Level.INFO, "nb keys : " + browser.count("", "", null, false));
				}
			}
		}
	}
	
	@Test
	public void testFindServiceByFullname() throws NamingException {
		Properties jndiProps = new Properties();
		jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
		jndiProps.put(Context.PROVIDER_URL,"http-remoting://localhost:8080");
		jndiProps.put("jboss.naming.client.ejb.context", true);
		Context jndi = new InitialContext(jndiProps);
		BrowserService browser = (BrowserService) jndi.lookup("diffusion-server/components/browser!fr.ortolang.diffusion.browser.BrowserService");
		LOGGER.log(Level.INFO, browser.getServiceName());
	}
	
	

}
