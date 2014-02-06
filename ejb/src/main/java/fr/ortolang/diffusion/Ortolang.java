package fr.ortolang.diffusion;


public class Ortolang {
	
//	private static Logger logger = Logger.getLogger(Ortolang.class.getName());
//	private static InitialContext jndi;

//	private static synchronized InitialContext getJndiContext() throws OrtolangException {
//		try {
//			if (jndi == null) {
//				Properties properties = new Properties();
//				properties.put(Context.INITIAL_CONTEXT_FACTORY,  org.jboss.naming.remote.client.InitialContextFactory.class.getName());
//				properties.put(Context.PROVIDER_URL, "remote://" +  OrtolangConfig.getInstance().getProperty("naming.host") + ":" + OrtolangConfig.getInstance().getProperty("naming.port"));
//				jndi = new InitialContext(properties);
//			}
//
//			return jndi;
//		} catch (Exception e) {
//			throw new OrtolangException(e);
//		}
//	}
//
//	public static String[] listServices() throws OrtolangException {
//		try {
//			NamingEnumeration<NameClassPair> enumeration = getJndiContext().list("");
//			Vector<String> result = new Vector<String>();
//
//			while (enumeration.hasMoreElements()) {
//				String name = ((NameClassPair) enumeration.next()).getName();
//
//				if (name.startsWith(OrtolangNamingConvention.SERVICE_PREFIX)) {
//					logger.log(Level.FINE, "jndi service name found : " + name);
//					result.add(OrtolangNamingConvention.getServiceNameFromJNDI(name));
//				}
//			}
//
//			return result.toArray(new String[result.size()]);
//		} catch (Exception e) {
//			throw new OrtolangException(e);
//		}
//	}
//
//	public static OrtolangService findService(String serviceName) throws OrtolangException {
//		try {
//			logger.log(Level.FINE, "looking in jndi: " + OrtolangNamingConvention.getJNDINameForService(serviceName));
//
//			return (OrtolangService) getJndiContext().lookup(OrtolangNamingConvention.getJNDINameForService(serviceName));
//		} catch (Exception e) {
//			throw new OrtolangException(e);
//		}
//	}
//
//	public static OrtolangService findLocalService(String serviceName) throws OrtolangException {
//		try {
//			logger.log(Level.FINE, "looking in jndi: " + OrtolangNamingConvention.getJNDINameForLocalService(serviceName));
//
//			return (OrtolangService) getJndiContext().lookup(OrtolangNamingConvention.getJNDINameForLocalService(serviceName));
//		} catch (Exception e) {
//			throw new OrtolangException(e);
//		}
//	}


}
