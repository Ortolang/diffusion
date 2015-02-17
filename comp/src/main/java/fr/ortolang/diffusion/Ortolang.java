package fr.ortolang.diffusion;

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
