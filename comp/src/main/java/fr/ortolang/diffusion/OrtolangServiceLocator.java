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

    private static final String NAMESPACE = "java:global/diffusion/components";
    private static final String SERVICE_SUFFIX = "Service";
    private static final String WORKER_SUFFIX = "Worker";

    private static final Logger LOGGER = Logger.getLogger(OrtolangServiceLocator.class.getName());
    private static InitialContext jndi;
    private static Map<String, Object> objects = new HashMap<String, Object> ();
    private static Map<String, OrtolangService> services = new HashMap<String, OrtolangService> ();
    private static Map<String, OrtolangWorker> workers = new HashMap<> ();
    private static Map<String, OrtolangIndexableService> indexableServices = new HashMap<String, OrtolangIndexableService> ();

    private OrtolangServiceLocator() {
    }

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
                String name = enumeration.next().getName();
                if (name.endsWith(SERVICE_SUFFIX)) {
                    LOGGER.log(Level.INFO, "jndi service name found : " + name);
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
                    String name = enumeration.next().getName();
                    if (name.endsWith(SERVICE_SUFFIX) && name.substring(0, name.indexOf("!")).equals(serviceName)) {
                        services.put(serviceName, (OrtolangService) getJndiContext().lookup(NAMESPACE + "/" + name));
                        return services.get(serviceName);
                    }
                }
                throw new OrtolangException("service not found: " + serviceName);
            }
            return services.get(serviceName);
        } catch (Exception e) {
            throw new OrtolangException(e);
        }
    }

    public static List<String> listWorkers() throws OrtolangException {
        try {
            NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/");
            List<String> results = new ArrayList<>();
            while (enumeration.hasMoreElements()) {
                String name = enumeration.next().getName();
                if (name.endsWith(WORKER_SUFFIX)) {
                    LOGGER.log(Level.INFO, "jndi worker name found : " + name);
                    results.add(name.substring(0, name.indexOf("!")));
                }
            }
            return results;
        } catch (Exception e) {
            throw new OrtolangException(e);
        }
    }

    public static OrtolangWorker findWorker(String workerName) throws OrtolangException {
        try {
            if (!workers.containsKey(workerName)) {
                NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/");
                while (enumeration.hasMoreElements()) {
                    String name = enumeration.next().getName();
                    if (name.endsWith(WORKER_SUFFIX) && name.substring(0, name.indexOf("!")).equals(workerName)) {
                        return workers.put(workerName, (OrtolangWorker) getJndiContext().lookup(NAMESPACE + "/" + name));
                    }
                }
                throw new OrtolangException("worker not found: " + workerName);
            }
            return workers.get(workerName);
        } catch (Exception e) {
            throw new OrtolangException(e);
        }
    }

    public static Object lookup(String name, Class<?> clazz) throws OrtolangException {
        try {
            if ( !objects.containsKey(name) ) {
                objects.put(name, getJndiContext().lookup(NAMESPACE + "/" + name + "!" + clazz.getCanonicalName()));
            }
            return objects.get(name);
        } catch (Exception e) {
            throw new OrtolangException(e);
        }
    }

    public static OrtolangIndexableService findIndexableService(String serviceName) throws OrtolangException {
        try {
            if ( !indexableServices.containsKey(serviceName) ) {
                NamingEnumeration<NameClassPair> enumeration = getJndiContext().list(NAMESPACE + "/");
                while (enumeration.hasMoreElements()) {
                    String name = enumeration.next().getName();
                    if (name.endsWith(SERVICE_SUFFIX) && name.substring(0, name.indexOf("!")).equals(serviceName)) {
                        indexableServices.put(serviceName, (OrtolangIndexableService) getJndiContext().lookup(NAMESPACE + "/" + name));
                        return indexableServices.get(serviceName);
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
