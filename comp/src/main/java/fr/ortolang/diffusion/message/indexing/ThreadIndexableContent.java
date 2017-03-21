package fr.ortolang.diffusion.message.indexing;

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

import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.message.MessageService;
import fr.ortolang.diffusion.message.entity.Thread;

import java.util.HashMap;
import java.util.Map;

public class ThreadIndexableContent extends OrtolangIndexableContent {

    private static final Object[] THREAD_MAPPING;

    static {
        THREAD_MAPPING = new String[]{
                "key",
                "type=keyword",
                "title",
                "type=string",
                "lastActivity",
                "type=date",
                "question",
                "type=string",
                "answer",
                "type=keyword",
                "workspace",
                "type=keyword"
        };
    }

    public ThreadIndexableContent(Thread thread) throws IndexingServiceException {
        super(MessageService.SERVICE_NAME, Thread.OBJECT_TYPE, thread.getKey());
        Map<String, Object> content = new HashMap<>();
        content.put("key", thread.getKey());
        content.put("title", thread.getTitle());
        content.put("lastActivity", thread.getLastActivity().getTime());
        content.put("question", thread.getQuestion());
        content.put("answer", thread.getAnswer());
        content.put("workspace", thread.getWorkspace());
        setContent(content);
    }

//    @Override
//    public Object[] getMapping() {
//        return THREAD_MAPPING;
//    }
}
