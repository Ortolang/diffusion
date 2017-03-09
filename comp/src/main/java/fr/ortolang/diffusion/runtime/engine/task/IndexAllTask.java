package fr.ortolang.diffusion.runtime.engine.task;

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

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectState.Status;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.Profile;
import fr.ortolang.diffusion.message.MessageService;
import fr.ortolang.diffusion.message.entity.Message;
import fr.ortolang.diffusion.message.entity.Thread;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.referential.ReferentialServiceException;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import org.activiti.engine.delegate.DelegateExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class IndexAllTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(IndexAllTask.class.getName());

    public static final String NAME = "Index All";

    private static final String ALL_TYPES = "all";

    @Override
    public String getTaskName() {
        return NAME;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        if (execution.getVariable(INITIER_PARAM_NAME, String.class).equals(MembershipService.SUPERUSER_IDENTIFIER)) {
            try {
                BrowserService browser = getBrowserService();

                String types = execution.getVariable(INDEXING_TYPES_PARAM_NAME, String.class);
                String phaseVariable = execution.getVariable(INDEXING_PHASE_PARAM_NAME, String.class);
                Integer phase = phaseVariable != null ? Integer.valueOf(phaseVariable) : -1;
                boolean all = false;
                if (types == null || types.isEmpty() || types.contains(ALL_TYPES)) {
                    types = "";
                    all = true;
                }

                LOGGER.log(Level.INFO, "Starting Index All Task for types " + types + " (phase " + phase + ")");

                // REFERENTIAL ENTITIES
                if (all || types.contains(ReferentialEntity.OBJECT_TYPE)) {
                    ReferentialService referential = getReferentialService();
                    if (phase == 1) {
                        LOGGER.log(Level.INFO, "Indexing all referential entities of type: ORGANIZATION");
                        List<ReferentialEntity> entities = referential.listEntities(ReferentialEntityType.ORGANIZATION);
                        indexOrtolangObjects(entities, ReferentialEntityType.ORGANIZATION.name());
                        LOGGER.log(Level.INFO, "Indexing all referential entities of type: STATUSOFUSE");
                        entities = referential.listEntities(ReferentialEntityType.STATUSOFUSE);
                        indexOrtolangObjects(entities, ReferentialEntityType.STATUSOFUSE.name());
                    }
                    if (phase == 2) {
                        for (ReferentialEntityType type : ReferentialEntityType.values()) {
                            LOGGER.log(Level.INFO, "Indexing all referential entities of type: " + type);
                            if (type != ReferentialEntityType.ORGANIZATION && type != ReferentialEntityType.STATUSOFUSE) {
                                List<ReferentialEntity> entities = referential.listEntities(type);
                                indexOrtolangObjects(entities, type.name());
                            }
                        }
                    }
                }

                // PROFILES
                if ((all && phase == 1) || types.contains(Profile.OBJECT_TYPE)) {
                    LOGGER.log(Level.INFO, "Indexing all profiles");
                    List<String> profiles = browser.list(0, -1, MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, Status.DRAFT);
                    indexKeys(profiles, Profile.OBJECT_TYPE);
                }

                // GROUPS
                if ((all && phase == 1) || types.contains(Group.OBJECT_TYPE)) {
                    LOGGER.log(Level.INFO, "Indexing all groups");
                    List<String> groups = browser.list(0, -1, MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, Status.DRAFT);
                    indexKeys(groups, Group.OBJECT_TYPE);
                }

                // WORKSPACES
                if ((all && phase == 1) || types.contains(Workspace.OBJECT_TYPE)) {
                    LOGGER.log(Level.INFO, "Indexing all workspaces");
                    List<String> workspaces = browser.list(0, -1, CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, Status.DRAFT);
                    indexKeys(workspaces, Workspace.OBJECT_TYPE);
                }

                // OBJECTS
                if ((all && phase == 1) || types.contains(DataObject.OBJECT_TYPE)) {
                    LOGGER.log(Level.INFO, "Indexing all data objects");
                    List<String> objects = browser.list(0, -1, CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, Status.DRAFT);
                    objects.addAll(browser.list(0, -1, CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, Status.PUBLISHED));
                    indexKeys(objects, DataObject.OBJECT_TYPE);
                }

                // METADATA
                if ((all && phase == 1) || types.contains(MetadataObject.OBJECT_TYPE)) {
                    LOGGER.log(Level.INFO, "Indexing all metadata objects");
                    List<String> metadatas = browser.list(0, -1, CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, Status.PUBLISHED);
//                    objects.addAll(browser.list(0, -1, CoreService.SERVICE_NAME, MetadataObject.OBJECT_TYPE, Status.PUBLISHED));
                    indexKeys(metadatas, MetadataObject.OBJECT_TYPE);
                }

                // LINKS
                if ((all && phase == 1) || types.contains(Link.OBJECT_TYPE)) {
                    LOGGER.log(Level.INFO, "Indexing all links");
                    List<String> links = browser.list(0, -1, CoreService.SERVICE_NAME, Link.OBJECT_TYPE, Status.DRAFT);
                    links.addAll(browser.list(0, -1, CoreService.SERVICE_NAME, Link.OBJECT_TYPE, Status.PUBLISHED));
                    indexKeys(links, Link.OBJECT_TYPE);
                }

                // COLLECTIONS
                if (all || types.contains(Collection.OBJECT_TYPE)) {
                    CoreService core = getCoreService();
                    List<Collection> collections = core.systemListCollections();
                    RegistryService registry = getRegistryService();
                    List<String> published = new ArrayList<>();
                    List<String> publishedRoot = new ArrayList<>();
                    List<String> draft = new ArrayList<>();
                    List<String> draftRoot = new ArrayList<>();
                    for (Collection collection : collections) {
                        try {
                            if (registry.isHidden(collection.getKey())) {
                                continue;
                            }
                            if (Status.PUBLISHED.value().equals(registry.getPublicationStatus(collection.getKey()))) {
                                if (collection.isRoot()) {
                                    publishedRoot.add(collection.getKey());
                                } else {
                                    published.add(collection.getKey());
                                }
                            } else {
                                if (collection.isRoot()) {
                                    draftRoot.add(collection.getKey());
                                } else {
                                    draft.add(collection.getKey());
                                }
                            }
                        } catch (KeyNotFoundException e) {
                            LOGGER.log(Level.FINE, e.getMessage());
                        }
                    }
                    if (phase == 1) {
                        LOGGER.log(Level.INFO, "Indexing all published collections");
                        indexKeys(published, Collection.OBJECT_TYPE);
                        LOGGER.log(Level.INFO, "Indexing all draft collections");
                        indexKeys(draft, Collection.OBJECT_TYPE);
                        LOGGER.log(Level.INFO, "Indexing all draft root collections");
                        indexKeys(draftRoot, Collection.OBJECT_TYPE);
                    }
                    if (phase == 2) {
                        LOGGER.log(Level.INFO, "Indexing all published root collections");
                        indexKeys(publishedRoot, Collection.OBJECT_TYPE);
                    }
                }

                // THREADS
                if ((all && phase == 1) || types.contains(Thread.OBJECT_TYPE)) {
                    LOGGER.log(Level.INFO, "Indexing all threads");
                    List<String> threads = browser.list(0, -1, MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, Status.DRAFT);
                    indexKeys(threads, Thread.OBJECT_TYPE);
                }

                // MESSAGES
                if ((all && phase == 1) || types.contains(Message.OBJECT_TYPE)) {
                    LOGGER.log(Level.INFO, "Indexing all messages");
                    List<String> messages = browser.list(0, -1, MessageService.SERVICE_NAME, Message.OBJECT_TYPE, Status.DRAFT);
                    indexKeys(messages, Message.OBJECT_TYPE);
                }

                LOGGER.log(Level.INFO, "Indexing event sent");

            } catch (BrowserServiceException | CoreServiceException | RegistryServiceException | ReferentialServiceException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                throw new RuntimeEngineTaskException(e.getMessage(), e);
            }
        } else {
            throw new RuntimeEngineTaskException("only " + MembershipService.SUPERUSER_IDENTIFIER + " can perform this task !!");
        }
    }

    private void indexOrtolangObjects(List<? extends OrtolangObject> objects, String type) throws RuntimeEngineTaskException {
        indexKeys(objects.stream().map(OrtolangObject::getObjectKey).collect(Collectors.toList()), type);
    }

    private void indexKeys(List<String> keys, String type) throws RuntimeEngineTaskException {
        IndexingService indexing = getIndexingService();
        for (String key : keys) {
            try {
                indexing.index(key);
            } catch (IndexingServiceException e) {
                LOGGER.log(Level.WARNING, "Unexpected error while trying to index key [" + key  + "] of type [" + type + "]: " + e.getMessage());
            }
        }
    }
}
