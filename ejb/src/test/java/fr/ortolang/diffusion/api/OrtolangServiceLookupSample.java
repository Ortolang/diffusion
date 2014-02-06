package fr.ortolang.diffusion.api;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.junit.Test;

import fr.ortolang.diffusion.browser.BrowserService;

public class OrtolangServiceLookupSample {

	@Test
	public void lookup() throws NamingException {
		final Hashtable<String, String> jndiProperties = new Hashtable<String, String> ();
		jndiProperties.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
		final Context context = new InitialContext(jndiProperties);
		NamingEnumeration<NameClassPair> list = context.list("fr.ortolang.diffusion.browser.BrowserService");
		while ( list.hasMore() ) {
			System.out.println(list.next());
		}
		BrowserService browser = (BrowserService) context.lookup("ejb:diffusion-server-ear/diffusion-server-ejb/Browser!fr.ortolang.diffusion.browser.BrowserService");
		System.out.println(browser.getServiceName());
	}

}
