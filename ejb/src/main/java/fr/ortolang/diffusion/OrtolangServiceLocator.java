package fr.ortolang.diffusion;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

public class OrtolangServiceLocator {
	
	private static final String NAMESPACE = "java:global";
	private static final String APPLICATION_NAME = "diffusion-server-ear";
	private static final String MODULE_NAME = "diffusion-server-ejb";
	private static final String SERVICE_SUFFIX = "Service";
	private static final String LOCAL_SERVICE_SUFFIX = "ServiceLocal";
	private static Logger logger = Logger.getLogger(OrtolangServiceLocator.class.getName());
	private static InitialContext jndi;
	
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
			NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/" + APPLICATION_NAME + "/" + MODULE_NAME);
			List<String> results = new ArrayList<String>();
			while (enumeration.hasMoreElements()) {
				String name = ((NameClassPair) enumeration.next()).getName();
				if (name.endsWith(OrtolangServiceLocator.SERVICE_SUFFIX)) {
					logger.log(Level.INFO, "jndi service name found : " + name);
					results.add(name.substring(0, name.indexOf("!")));
				}
				if (name.endsWith(OrtolangServiceLocator.LOCAL_SERVICE_SUFFIX)) {
					logger.log(Level.INFO, "jndi local service name found : " + name);
					results.add("(local) " + name.substring(0, name.indexOf("!")));
				}
			}
			return results;
		} catch (Exception e) {
			throw new OrtolangException(e);
		}
	}
	
	public static OrtolangService findService(String serviceName) throws OrtolangException {
		try {
			NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/" + APPLICATION_NAME + "/" + MODULE_NAME);
			while (enumeration.hasMoreElements()) {
				String name = ((NameClassPair) enumeration.next()).getName();
				if (name.endsWith(OrtolangServiceLocator.SERVICE_SUFFIX)) {
					if (name.substring(0, name.indexOf("!")).equals(serviceName)) {
						return (OrtolangService)getJndiContext().lookup(NAMESPACE + "/" + APPLICATION_NAME + "/" + MODULE_NAME + "/" + name);
					}
				}
			}
			throw new OrtolangException("service not found");
		} catch (Exception e) {
			throw new OrtolangException(e);
		}
	}
		
	public static OrtolangService findLocalService(String serviceName) throws OrtolangException {
		try {
			NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/" + APPLICATION_NAME + "/" + MODULE_NAME);
			while (enumeration.hasMoreElements()) {
				String name = ((NameClassPair) enumeration.next()).getName();
				if (name.endsWith(OrtolangServiceLocator.LOCAL_SERVICE_SUFFIX)) {
					if (name.substring(0, name.indexOf("!")).equals(serviceName)) {
						return (OrtolangService)getJndiContext().lookup(NAMESPACE + "/" + APPLICATION_NAME + "/" + MODULE_NAME + "/" + name);
					}
				}
			}
			throw new OrtolangException("service not found: " + serviceName);
		} catch (Exception e) {
			throw new OrtolangException(e);
		}
	}
	
	public static OrtolangIndexableService findIndexableService(String serviceName) throws OrtolangException {
		try {
			NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/" + APPLICATION_NAME + "/" + MODULE_NAME);
			while (enumeration.hasMoreElements()) {
				String name = ((NameClassPair) enumeration.next()).getName();
				if (name.endsWith(OrtolangServiceLocator.LOCAL_SERVICE_SUFFIX)) {
					if (name.substring(0, name.indexOf("!")).equals(serviceName)) {
						return (OrtolangIndexableService)getJndiContext().lookup(NAMESPACE + "/" + APPLICATION_NAME + "/" + MODULE_NAME + "/" + name);
					}
				}
			}
			throw new OrtolangException("service not found");
		} catch (Exception e) {
			throw new OrtolangException(e);
		}
	}


}
