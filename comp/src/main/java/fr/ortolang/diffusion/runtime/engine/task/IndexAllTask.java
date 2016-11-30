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

import fr.ortolang.diffusion.OrtolangObjectState.Status;
import fr.ortolang.diffusion.browser.BrowserService;
import fr.ortolang.diffusion.browser.BrowserServiceException;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.CoreServiceException;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.DataObject;
import fr.ortolang.diffusion.core.entity.Link;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.indexing.IndexingService;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.membership.MembershipService;
import fr.ortolang.diffusion.membership.entity.Group;
import fr.ortolang.diffusion.membership.entity.Profile;
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
                boolean all = false;
                if (types == null || types.isEmpty() || types.contains(ALL_TYPES)) {
                    all = true;
                }

                // PROFILES
                if (all || types.contains(Profile.OBJECT_TYPE)) {
                    List<String> profiles = browser.list(0, -1, MembershipService.SERVICE_NAME, Profile.OBJECT_TYPE, Status.DRAFT);
                    indexKeys(profiles, Profile.OBJECT_TYPE);
                }

                // GROUP
                if (all || types.contains(Group.OBJECT_TYPE)) {
                    List<String> groups = browser.list(0, -1, MembershipService.SERVICE_NAME, Group.OBJECT_TYPE, Status.DRAFT);
                    indexKeys(groups, Group.OBJECT_TYPE);
                }

                // WORKSPACES
                if (all || types.contains(Workspace.OBJECT_TYPE)) {
                    List<String> workspaces = browser.list(0, -1, CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE, Status.DRAFT);
                    indexKeys(workspaces, Workspace.OBJECT_TYPE);
                }

                // OBJECTS
                if (all || types.contains(DataObject.OBJECT_TYPE)) {
                    List<String> objects = browser.list(0, -1, CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, Status.DRAFT);
                    objects.addAll(browser.list(0, -1, CoreService.SERVICE_NAME, DataObject.OBJECT_TYPE, Status.PUBLISHED));
                    indexKeys(objects, DataObject.OBJECT_TYPE);
                }

                // LINKS
                if (all || types.contains(Link.OBJECT_TYPE)) {
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
                    List<String> draft = new ArrayList<>();
                    for (Collection collection : collections) {
                        try {
                            if (registry.isHidden(collection.getKey())) {
                                continue;
                            }
                            if (Status.PUBLISHED.value().equals(registry.getPublicationStatus(collection.getKey()))) {
                                published.add(collection.getKey());
                            } else {
                                draft.add(collection.getKey());
                            }
                        } catch (KeyNotFoundException e) {
                            LOGGER.log(Level.FINE, e.getMessage());
                        }
                    }
                    indexKeys(draft, Collection.OBJECT_TYPE);
                    indexKeys(published, Collection.OBJECT_TYPE);
                }

            } catch (BrowserServiceException | CoreServiceException | RegistryServiceException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                throw new RuntimeEngineTaskException(e.getMessage(), e);
            }
        } else {
            throw new RuntimeEngineTaskException("only " + MembershipService.SUPERUSER_IDENTIFIER + " can perform this task !!");
        }
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
