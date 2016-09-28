package fr.ortolang.diffusion.store.es;

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

import fr.ortolang.diffusion.*;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.membership.entity.ProfileDataVisibility;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import net.didion.jwnl.data.Exc;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.jboss.ejb3.annotation.SecurityDomain;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Local(ElasticSearchService.class)
@Singleton(name = ElasticSearchService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ElasticSearchServiceBean implements ElasticSearchService {

    private static final Logger LOGGER = Logger.getLogger(ElasticSearchServiceBean.class.getName());

    @EJB
    private RegistryService registry;
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;

    private TransportClient client;

    public ElasticSearchServiceBean() {
        LOGGER.log(Level.FINE, "Instantiating Elastic Search Service");
    }

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initializing Elastic Search Service");
        try {
            client = TransportClient.builder().build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300))
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9301))
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9302));
        } catch (UnknownHostException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        LOGGER.log(Level.INFO, "Shutting down Elastic Search Service");
        client.close();
    }

    @Override
    public IndexResponse index(String key) throws KeyNotFoundException, RegistryServiceException, OrtolangException {
        OrtolangObjectIdentifier identifier = registry.lookup(key);
        OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
        Map<String, Object> source = service.getElasticSearchContent(key);
        if (source != null && !source.isEmpty()) {
            return client.prepareIndex(identifier.getService(), identifier.getType(), key).setSource(source).get();
        }
        return null;
    }

    @Override
    public String getServiceName() {
        return ElasticSearchService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

    @Override
    public String[] getObjectTypeList() {
        return new String[0];
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        return new String[0];
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }
}
