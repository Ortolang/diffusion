package fr.ortolang.diffusion.core.indexing;

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

import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.SnapshotElement;
import fr.ortolang.diffusion.core.entity.TagElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.indexing.IndexingServiceException;

import java.util.*;
import java.util.stream.Stream;

public class WorkspaceIndexableContent extends OrtolangObjectIndexableContent {

    private static final String[] WORKSPACE_MAPPING;

    static {
        WORKSPACE_MAPPING = Stream.concat(Arrays.stream(ORTOLANG_OBJECT_MAPPING),
                Arrays.stream(new String[] {
                        "alias",
                        "type=keyword",
                        "head",
                        "type=keyword",
                        "archive",
                        "type=boolean",
                        "snapshots",
                        "type=nested"
                }))
                .toArray(String[]::new);
    }

    public WorkspaceIndexableContent(Workspace workspace) throws IndexingServiceException {
        super(workspace, CoreService.SERVICE_NAME, Workspace.OBJECT_TYPE);
        content.put("alias", workspace.getAlias());
        content.put("head", workspace.getHead());
        content.put("archive", workspace.isArchive());
        List<Map<String, String>> snapshots = new ArrayList<>();
        for (SnapshotElement snapshotElement : workspace.getSnapshots()) {
            Map<String, String> snapshot = new HashMap<>();
            snapshot.put("key", snapshotElement.getKey());
            snapshot.put("name", snapshotElement.getName());
            TagElement tagElement = workspace.findTagBySnapshot(snapshotElement.getName());
            if (tagElement != null) {
                snapshot.put("tag", tagElement.getName());
            }
            snapshots.add(snapshot);
        }
        content.put("snapshots", snapshots);
        setContent(content);
    }

    @Override
    public Object[] getMapping() {
        return WORKSPACE_MAPPING;
    }
}
