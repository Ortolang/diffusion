package fr.ortolang.diffusion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

public class OrtolangServiceLocator {
	
	private static final String NAMESPACE = "java:module";
	private static final String SERVICE_SUFFIX = "Service";
	
	private static Logger logger = Logger.getLogger(OrtolangServiceLocator.class.getName());
	private static InitialContext jndi;
	private static Map<String, OrtolangService> services = new HashMap<String, OrtolangService> ();
	private static Map<String, OrtolangIndexableService> indexableServices = new HashMap<String, OrtolangIndexableService> ();
	
	private static synchronized InitialContext getJndiContext() throws OrtolangException {
		try {
			if (jndi == null) {
				jndi = new InitialContext();
			}
			return jndi;
		} catch (Exception e) {
			throw new OrtolangException(e);
		}
	}
	
	public static List<String> listServices() throws OrtolangException {
		try {
			NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/");
			List<String> results = new ArrayList<String>();
			while (enumeration.hasMoreElements()) {
				String name = ((NameClassPair) enumeration.next()).getName();
				if (name.endsWith(OrtolangServiceLocator.SERVICE_SUFFIX)) {
					logger.log(Level.INFO, "jndi service name found : " + name);
					results.add(name.substring(0, name.indexOf("!")));
				}
			}
			return results;
		} catch (Exception e) {
			throw new OrtolangException(e);
		}
	}
	
	public static OrtolangService findService(String serviceName) throws OrtolangException {
		try {
			if ( !services.containsKey(serviceName) ) {
				NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/");
				while (enumeration.hasMoreElements()) {
					String name = ((NameClassPair) enumeration.next()).getName();
					if (name.endsWith(OrtolangServiceLocator.SERVICE_SUFFIX)) {
						if (name.substring(0, name.indexOf("!")).equals(serviceName)) {
							services.put(serviceName, (OrtolangService)getJndiContext().lookup(NAMESPACE + "/" + name));
							return services.get(serviceName);
						}
					}
				}
				throw new OrtolangException("service not found: " + serviceName);
			}
			return services.get(serviceName);
		} catch (Exception e) {
			throw new OrtolangException(e);
		}
	}
	
	public static Object lookup(String name) throws OrtolangException {
		try {
			return getJndiContext().lookup(NAMESPACE + "/" + name);
		} catch (Exception e) {
			throw new OrtolangException(e);
		}
	}
	
	public static OrtolangIndexableService findIndexableService(String serviceName) throws OrtolangException {
		try {
			if ( !indexableServices.containsKey(serviceName) ) {
				NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/");
				while (enumeration.hasMoreElements()) {
					String name = ((NameClassPair) enumeration.next()).getName();
					if (name.endsWith(OrtolangServiceLocator.SERVICE_SUFFIX)) {
						if (name.substring(0, name.indexOf("!")).equals(serviceName)) {
							indexableServices.put(serviceName, (OrtolangIndexableService)getJndiContext().lookup(NAMESPACE + "/" + name));
							return indexableServices.get(serviceName);
						}
					}
				}
				throw new OrtolangException("service not found: " + serviceName);
			} 
			return indexableServices.get(serviceName);
		} catch (Exception e) {
			throw new OrtolangException(e);
		}
	}
	
}
