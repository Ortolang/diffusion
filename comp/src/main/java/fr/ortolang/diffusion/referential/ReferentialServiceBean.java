package fr.ortolang.diffusion.referential;

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
 * Copyright (C) 2013 - 2016 Ortolang Team
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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangEvent;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.OrtolangObjectState;
import fr.ortolang.diffusion.OrtolangObjectXmlExportHandler;
import fr.ortolang.diffusion.OrtolangObjectXmlImportHandler;
import fr.ortolang.diffusion.OrtolangSearchResult;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.MembershipServiceException;
import fr.ortolang.diffusion.notification.NotificationService;
import fr.ortolang.diffusion.notification.NotificationServiceException;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
import fr.ortolang.diffusion.referential.xml.ReferentialEntityExportHandler;
import fr.ortolang.diffusion.registry.IdentifierAlreadyRegisteredException;
import fr.ortolang.diffusion.registry.IdentifierNotRegisteredException;
import fr.ortolang.diffusion.registry.KeyAlreadyExistsException;
import fr.ortolang.diffusion.registry.KeyLockedException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.security.authorisation.AuthorisationService;
import fr.ortolang.diffusion.security.authorisation.AuthorisationServiceException;
import fr.ortolang.diffusion.store.index.IndexStoreDocumentBuilder;
import fr.ortolang.diffusion.store.index.IndexStoreService;
import fr.ortolang.diffusion.store.index.IndexStoreServiceException;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContentProperty;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;

@Local(ReferentialService.class)
@Stateless(name = ReferentialService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class ReferentialServiceBean implements ReferentialService {

    private static final Logger LOGGER = Logger.getLogger(ReferentialServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] { ReferentialEntity.OBJECT_TYPE };
    private static final String[][] OBJECT_PERMISSIONS_LIST = new String[][] { { ReferentialEntity.OBJECT_TYPE, "read,update,delete" } };

    @EJB
    private RegistryService registry;
    @EJB
    private NotificationService notification;
    @EJB
    private MembershipService membership;
    @EJB
    private AuthorisationService authorisation;
    @EJB
    private IndexingService indexing;
    @EJB
    private IndexStoreService indexStore;
    @PersistenceContext(unitName = "ortolangPU")
    private EntityManager em;
    @Resource
    private SessionContext ctx;

    public ReferentialServiceBean() {
    }

    public RegistryService getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryService registry) {
        this.registry = registry;
    }

    public NotificationService getNotification() {
        return notification;
    }

    public void setNotification(NotificationService notification) {
        this.notification = notification;
    }

    public MembershipService getMembership() {
        return membership;
    }

    public void setMembership(MembershipService membership) {
        this.membership = membership;
    }

    public AuthorisationService getAuthorisation() {
        return authorisation;
    }

    public void setAuthorisation(AuthorisationService authorisation) {
        this.authorisation = authorisation;
    }

    public IndexingService getIndexing() {
        return indexing;
    }

    public void setIndexing(IndexingService indexing) {
        this.indexing = indexing;
    }

    public IndexStoreService getIndexStore() {
        return indexStore;
    }

    public void setIndexStore(IndexStoreService indexStore) {
        this.indexStore = indexStore;
    }

    public EntityManager getEm() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }

    public SessionContext getCtx() {
        return ctx;
    }

    public void setCtx(SessionContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        for (int i = 0; i < OBJECT_PERMISSIONS_LIST.length; i++) {
            if (OBJECT_PERMISSIONS_LIST[i][0].equals(type)) {
                return OBJECT_PERMISSIONS_LIST[i][1].split(",");
            }
        }
        throw new OrtolangException("Unable to find object permissions list for object type : " + type);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public OrtolangObject findObject(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);

            if (!identifier.getService().equals(ReferentialService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            if (identifier.getType().equals(ReferentialEntity.OBJECT_TYPE)) {
                return readEntity(key.replaceFirst(SERVICE_NAME + ":", ""));
            }
            throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
        } catch (ReferentialServiceException | RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        return null;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ReferentialEntity> listEntities(ReferentialEntityType type) throws ReferentialServiceException {
        LOGGER.log(Level.FINE, "Listing all entities");
        try {
            TypedQuery<ReferentialEntity> query = em.createNamedQuery("findAllEntitiesWithType", ReferentialEntity.class).setParameter("type", type);

            List<ReferentialEntity> refEntitys = query.getResultList();
            List<ReferentialEntity> rrefEntitys = new ArrayList<ReferentialEntity>();
            for (ReferentialEntity refEntity : refEntitys) {
                try {
                    String ikey = registry.lookup(refEntity.getObjectIdentifier());
                    refEntity.setKey(ikey);
                    rrefEntitys.add(refEntity);
                } catch (IdentifierNotRegisteredException e) {
                    LOGGER.log(Level.FINE, "unregistered entity found in storage for id: " + refEntity.getId());
                }
            }
            return rrefEntitys;
        } catch (RegistryServiceException e) {
            LOGGER.log(Level.SEVERE, "unexpected error occured while listing eEntities", e);
            throw new ReferentialServiceException("unable to list entities", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ReferentialEntity createEntity(String name, ReferentialEntityType type, String content) throws ReferentialServiceException, KeyAlreadyExistsException, AccessDeniedException {
        LOGGER.log(Level.FINE, "creating ReferentielEntity for identifier name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            authorisation.checkAuthentified(subjects);

            String key = SERVICE_NAME + ":" + name;

            ReferentialEntity refEntity = new ReferentialEntity();
            refEntity.setId(UUID.randomUUID().toString());
            refEntity.setKey(key);
            refEntity.setType(type);
            refEntity.setContent(content);
            refEntity.setBoost(1L);

            registry.register(key, refEntity.getObjectIdentifier(), caller);
            registry.setPublicationStatus(key, OrtolangObjectState.Status.PUBLISHED.value());

            em.persist(refEntity);
            indexing.index(key);
            authorisation.createPolicy(key, caller);

            notification.throwEvent(key, caller, ReferentialEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentialService.SERVICE_NAME, ReferentialEntity.OBJECT_TYPE, "create"));

            return refEntity;
        } catch (NotificationServiceException | RegistryServiceException | IdentifierAlreadyRegisteredException | AuthorisationServiceException | MembershipServiceException | KeyNotFoundException
                | KeyLockedException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new ReferentialServiceException("unable to create ReferentielEntity with name [" + name + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public ReferentialEntity readEntity(String name) throws ReferentialServiceException, KeyNotFoundException {
        LOGGER.log(Level.FINE, "reading ReferentialEntity for name [" + name + "]");
        try {

            String key = SERVICE_NAME + ":" + name;
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, ReferentialEntity.OBJECT_TYPE);
            ReferentialEntity refEntity = em.find(ReferentialEntity.class, identifier.getId());
            if (refEntity == null) {
                throw new ReferentialServiceException("unable to find a ReferentialEntity for id " + identifier.getId());
            }
            refEntity.setKey(key);

            return refEntity;
        } catch (RegistryServiceException e) {
            throw new ReferentialServiceException("unable to read the ReferentialEntity with name [" + name + "]", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateEntity(String name, ReferentialEntityType type, String content) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException {
        updateEntity(name, type, content, null);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateEntity(String name, ReferentialEntityType type, String content, Long boost) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "updating ReferentialEntity for name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            String key = SERVICE_NAME + ":" + name;
            authorisation.checkPermission(key, subjects, "update");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, ReferentialEntity.OBJECT_TYPE);
            ReferentialEntity refEntity = em.find(ReferentialEntity.class, identifier.getId());
            if (refEntity == null) {
                throw new ReferentialServiceException("unable to find a ReferentialEntity for id " + identifier.getId());
            }
            if (type == null) {
                throw new ReferentialServiceException("unable to find the ReferentialEntityType");
            }
            refEntity.setType(type);
            refEntity.setContent(content);
            if (boost != null)
                refEntity.setBoost(boost);

            registry.update(key);
            em.merge(refEntity);
            indexing.index(key);

            notification.throwEvent(key, caller, ReferentialEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentialService.SERVICE_NAME, ReferentialEntity.OBJECT_TYPE, "update"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new ReferentialServiceException("error while trying to update the ReferentialEntity with name [" + name + "]");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deleteEntity(String name) throws ReferentialServiceException, KeyNotFoundException, AccessDeniedException {
        LOGGER.log(Level.FINE, "deleting ReferentialEntity for name [" + name + "]");
        try {
            String caller = membership.getProfileKeyForConnectedIdentifier();
            List<String> subjects = membership.getConnectedIdentifierSubjects();
            String key = SERVICE_NAME + ":" + name;
            authorisation.checkPermission(key, subjects, "delete");

            OrtolangObjectIdentifier identifier = registry.lookup(key);
            checkObjectType(identifier, ReferentialEntity.OBJECT_TYPE);
            ReferentialEntity refEntity = em.find(ReferentialEntity.class, identifier.getId());
            if (refEntity == null) {
                throw new ReferentialServiceException("unable to find a ReferentialEntity for id " + identifier.getId());
            }

            em.remove(refEntity);

            registry.delete(key);
            indexing.remove(key);

            notification.throwEvent(key, caller, ReferentialEntity.OBJECT_TYPE, OrtolangEvent.buildEventType(ReferentialService.SERVICE_NAME, ReferentialEntity.OBJECT_TYPE, "delete"));
        } catch (KeyLockedException | NotificationServiceException | RegistryServiceException | AuthorisationServiceException | MembershipServiceException | IndexingServiceException e) {
            ctx.setRollbackOnly();
            throw new ReferentialServiceException("unable to delete object with name [" + name + "]", e);
        }
    }

    /**
     * Finds entities by looking into the index-store.
     * 
     * @param type
     *            the type of the entities
     * @param term
     *            the term which you looking for
     * @param lang
     *            the language id of the text looking
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ReferentialEntity> findEntitiesByTerm(ReferentialEntityType type, String term, String lang) throws ReferentialServiceException {
        String query = new StringBuilder().append(IndexStoreDocumentBuilder.CONTENT_PROPERTY_FIELD_PREFIX).append(ReferentialEntity.CONTENT_TEXT).append(lang.toUpperCase()).append(":").append(term)
                .append("* ").append("AND ").append(IndexStoreDocumentBuilder.SERVICE_FIELD).append(":").append(ReferentialService.SERVICE_NAME).append(" ").append("AND ")
                .append(IndexStoreDocumentBuilder.CONTENT_PROPERTY_FIELD_PREFIX).append(ReferentialEntity.CONTENT_TYPE).append(":").append(type.toString().toLowerCase()).toString();

        List<ReferentialEntity> entities = new ArrayList<ReferentialEntity>();
        try {
            for (OrtolangSearchResult result : indexStore.search(query)) {
                entities.add(readEntity(result.getKey().replaceFirst(SERVICE_NAME + ":", "")));
            }
        } catch (IndexStoreServiceException | KeyNotFoundException e) {
            throw new ReferentialServiceException("error while looking for a ReferentialEntity with term [" + term + "]");
        }
        return entities;
    }

    private void checkObjectType(OrtolangObjectIdentifier identifier, String objectType) throws ReferentialServiceException {
        if (!identifier.getService().equals(getServiceName())) {
            throw new ReferentialServiceException("object identifier " + identifier + " does not refer to service " + getServiceName());
        }

        if (!identifier.getType().equals(objectType)) {
            throw new ReferentialServiceException("object identifier " + identifier + " does not refer to an object of type " + objectType);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IndexablePlainTextContent getIndexablePlainTextContent(String key) throws OrtolangException, NotIndexableContentException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(ReferentialService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }
            IndexablePlainTextContent content = new IndexablePlainTextContent();

            if (identifier.getType().equals(ReferentialEntity.OBJECT_TYPE)) {
                ReferentialEntity referentielEntity = em.find(ReferentialEntity.class, identifier.getId());
                if (referentielEntity == null) {
                    throw new OrtolangException("unable to load ReferentialEntity with id [" + identifier.getId() + "] from storage");
                }
                content.setName(key.replaceFirst(SERVICE_NAME + ":", ""));
                content.addProperties(new IndexablePlainTextContentProperty(ReferentialEntity.CONTENT_TYPE, referentielEntity.getType().toString()));

                if (referentielEntity.getType().equals(ReferentialEntityType.LANGUAGE)) {
                    StringReader reader = new StringReader(referentielEntity.getContent());
                    JsonReader jsonReader = Json.createReader(reader);
                    try {
                        JsonObject jsonObj = jsonReader.readObject();

                        content.setBoost(referentielEntity.getBoost());
                        content.addContentPart(jsonObj.getString("id"));
                        if (jsonObj.containsKey("labels")) {
                            for (JsonObject lang : jsonObj.getJsonArray("labels").getValuesAs(JsonObject.class)) {
                                content.addContentPart(lang.getString("value"));
                                content.addProperties(new IndexablePlainTextContentProperty(ReferentialEntity.CONTENT_TEXT + lang.getString("lang"), lang.getString("value")));
                            }
                        }
                    } catch (IllegalStateException | NullPointerException | ClassCastException e) {
                        LOGGER.log(Level.WARNING, "No property requested in json object", e);
                    } catch (JsonException e) {
                        LOGGER.log(Level.SEVERE, "No property requested in json object", e);
                    } finally {
                        jsonReader.close();
                        reader.close();
                    }
                } else if (referentielEntity.getType().equals(ReferentialEntityType.PERSON)) {
                    StringReader reader = new StringReader(referentielEntity.getContent());
                    JsonReader jsonReader = Json.createReader(reader);
                    try {
                        JsonObject jsonObj = jsonReader.readObject();

                        content.setBoost(referentielEntity.getBoost());
                        content.addContentPart(jsonObj.getString("id"));
                        content.addContentPart(jsonObj.getString("fullname"));
                        content.addProperties(new IndexablePlainTextContentProperty(ReferentialEntity.CONTENT_TEXT + "FR", jsonObj.getString("fullname")));
                    } catch (IllegalStateException | NullPointerException | ClassCastException e) {
                        LOGGER.log(Level.WARNING, "No property requested in json object", e);
                    } catch (JsonException e) {
                        LOGGER.log(Level.SEVERE, "No property requested in json object", e);
                    } finally {
                        jsonReader.close();
                        reader.close();
                    }
                } else if (referentielEntity.getType().equals(ReferentialEntityType.ORGANIZATION)) {
                    StringReader reader = new StringReader(referentielEntity.getContent());
                    JsonReader jsonReader = Json.createReader(reader);
                    try {
                        JsonObject jsonObj = jsonReader.readObject();

                        content.setBoost(referentielEntity.getBoost());
                        content.addContentPart(jsonObj.getString("id"));
                        content.addContentPart(jsonObj.getString("fullname"));
                        content.addProperties(new IndexablePlainTextContentProperty(ReferentialEntity.CONTENT_TEXT + "FR", jsonObj.getString("fullname")));
                    } catch (IllegalStateException | NullPointerException | ClassCastException e) {
                        LOGGER.log(Level.WARNING, "No property requested in json object", e);
                    } catch (JsonException e) {
                        LOGGER.log(Level.SEVERE, "No property requested in json object", e);
                    } finally {
                        jsonReader.close();
                        reader.close();
                    }
                }
            }
            return content;
        } catch (RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public IndexableJsonContent getIndexableJsonContent(String key) throws OrtolangException, NotIndexableContentException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(ReferentialService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }
            IndexableJsonContent content = new IndexableJsonContent();

            if (identifier.getType().equals(ReferentialEntity.OBJECT_TYPE)) {
                ReferentialEntity referentielEntity = em.find(ReferentialEntity.class, identifier.getId());
                if (referentielEntity == null) {
                    throw new OrtolangException("unable to load ReferentialEntity with id [" + identifier.getId() + "] from storage");
                }
                String json = referentielEntity.getContent();
                content.put("ortolang-referential-json", json);
            }
            return content;
        } catch (RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to find an object for key " + key);
        }
    }

    @Override
    public OrtolangObjectXmlExportHandler getObjectXmlExportHandler(String key) throws OrtolangException {
        try {
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            if (!identifier.getService().equals(ReferentialService.SERVICE_NAME)) {
                throw new OrtolangException("object identifier " + identifier + " does not refer to service " + getServiceName());
            }

            switch (identifier.getType()) {
            case ReferentialEntity.OBJECT_TYPE:
                ReferentialEntity referentielEntity = em.find(ReferentialEntity.class, identifier.getId());
                if (referentielEntity == null) {
                    throw new OrtolangException("unable to load ReferentialEntity with id [" + identifier.getId() + "] from storage");
                }
                return new ReferentialEntityExportHandler(referentielEntity);
            }

        } catch (RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to build object export handler " + key, e);
        }
        throw new OrtolangException("unable to build object export handler for key " + key);
    }

    @Override
    public OrtolangObjectXmlImportHandler getObjectXmlImportHandler() throws OrtolangException {
        // TODO
        throw new OrtolangException("NOT IMPLEMENTED");
    }

}
