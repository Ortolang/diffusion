package fr.ortolang.diffusion.indexing;

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

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Topic;

import org.jboss.ejb3.annotation.SecurityDomain;

@Local(IndexingService.class)
@Stateless(name = IndexingService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class IndexingServiceBean implements IndexingService {
	
	private static final Logger LOGGER = Logger.getLogger(IndexingServiceBean.class.getName());
	
    @Resource(mappedName = "java:jboss/exported/jms/topic/indexing")
	private Topic indexingTopic;
	@Resource
	private SessionContext sessionCtx;
	@Inject
	private JMSContext context;
	
	public IndexingServiceBean() {
	}

	@Override
	public void index(String key) throws IndexingServiceException {
		sendMessage("index", key);
	}

	@Override
	public void remove(String key) throws IndexingServiceException {
		sendMessage("remove", key);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	private void sendMessage(String action, String key) throws IndexingServiceException {
		try {
			Message message = context.createMessage();
			message.setStringProperty("action", action);
			message.setStringProperty("key", key);
			context.createProducer().send(indexingTopic, message);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "unable to send indexing message", e);
			throw new IndexingServiceException("unable to send indexing message", e);
		}
	}
	
	//Service methods
    
    @Override
    public String getServiceName() {
        return IndexingService.SERVICE_NAME;
    }
    
    @Override
    public Map<String, String> getServiceInfos() {
        //TODO provide infos about number of indexed document or maybe index queue status...
        return Collections.emptyMap();
    }

}
