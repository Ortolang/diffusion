package fr.ortolang.diffusion.api;

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
	
	private static Logger logger = Logger.getLogger(OrtolangServiceLookupSample.class.getName());

	@Test
	public void testListServices() throws NamingException {
		logger.log(Level.INFO, "Ortolang available services : ");
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
				logger.log(Level.INFO, "jndi service name found : " + name);
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
					logger.log(Level.INFO, service.getServiceName());
					BrowserService browser = (BrowserService) service;
					logger.log(Level.INFO, "nb keys : " + browser.count("", "", null, false));
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
		logger.log(Level.INFO, browser.getServiceName());
	}
	
	

}
