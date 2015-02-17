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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangIndexablePlainTextContent;
import fr.ortolang.diffusion.OrtolangIndexableSemanticContent;
import fr.ortolang.diffusion.OrtolangIndexableService;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.index.IndexStoreServiceException;
import fr.ortolang.diffusion.store.triple.TripleStoreService;

@MessageDriven(name = "IndexingTopicMDB", activationConfig = { @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "jms/topic/indexing"),
		@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
@SecurityDomain("ortolang")
public class IndexingListenerBean implements MessageListener {

	private Logger logger = Logger.getLogger(IndexingListenerBean.class.getName());

	@EJB
	private IndexStoreService indexStore;
	@EJB
	private TripleStoreService tripleStore;
	@EJB
	private RegistryService registry;

	public void setIndexStoreService(IndexStoreService store) {
		this.indexStore = store;
	}

	public IndexStoreService getIndexStoreService() {
		return indexStore;
	}

	public TripleStoreService getTripleStoreService() {
		return tripleStore;
	}

	public void setTripleStoreService(TripleStoreService triple) {
		this.tripleStore = triple;
	}

	public RegistryService getRegistry() {
		return registry;
	}

	public void setRegistry(RegistryService registry) {
		this.registry = registry;
	}

	@Override
	@PermitAll
	public void onMessage(Message message) {
		try {
			String action = message.getStringProperty("action");
			String key = message.getStringProperty("key");
			String root = message.getStringProperty("root");
			String path = message.getStringProperty("path");
			String name = message.getStringProperty("name");
			IndexingContext context = new IndexingContext(root, path, name);
			logger.log(Level.FINE, action + " action called on key: " + key);
			try {
				if (action.equals("index"))
					this.addToStore(key, context);
				if (action.equals("reindex"))
					this.updateStore(key, context);
				if (action.equals("remove"))
					this.removeFromStore(key, context);
			} catch (Exception e) {
				logger.log(Level.WARNING, "error during indexation of key " + key, e);
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "unable to index content", e);
		}
	}

	private void addToStore(String key, IndexingContext context) throws IndexingServiceException {
		try {
			OrtolangIndexableObject object = buildIndexableObject(key, context);
			indexStore.index(object);
		} catch (IndexStoreServiceException e) {
			throw new IndexingServiceException("unable to insert object in store", e);
		}
	}

	private void updateStore(String key, IndexingContext context) throws IndexingServiceException {
		try {
			OrtolangIndexableObject object = buildIndexableObject(key, context);
			indexStore.reindex(object);
		} catch (IndexStoreServiceException e) {
			throw new IndexingServiceException("unable to update object in store", e);
		}
	}

	private void removeFromStore(String key, IndexingContext context) throws IndexingServiceException {
		try {
			indexStore.remove(key);
		} catch (IndexStoreServiceException e) {
			throw new IndexingServiceException("unable to remove object from store", e);
		}
	}

	private OrtolangIndexableObject buildIndexableObject(String key, IndexingContext context) throws IndexingServiceException {
		try {
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
			OrtolangIndexablePlainTextContent content = service.getIndexablePlainTextContent(key);
			OrtolangIndexableSemanticContent scontent = service.getIndexableSemanticContent(key);
			OrtolangIndexableObject iobject = new OrtolangIndexableObject();
			iobject.setKey(key);
			iobject.setIdentifier(identifier);
			iobject.setService(identifier.getService());
			iobject.setType(identifier.getType());
			iobject.setHidden(registry.isHidden(key));
			iobject.setStatus(registry.getPublicationStatus(key));
			iobject.setProperties(registry.getProperties(key));
			iobject.setAuthor(registry.getAuthor(key));
			iobject.setCreationDate(registry.getCreationDate(key));
			iobject.setLastModificationDate(registry.getLastModificationDate(key));
			iobject.setName(key);
			iobject.setPlainTextContent(content);
			iobject.setSemanticContent(scontent);
			iobject.setContext(context);
			return iobject;
		} catch (Exception e) {
			throw new IndexingServiceException("unable to get indexable content for object ", e);
		}
	}

}
