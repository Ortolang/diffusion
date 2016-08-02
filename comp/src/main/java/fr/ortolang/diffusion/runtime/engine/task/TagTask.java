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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

public class TagTask extends RuntimeEngineTask {

    public static final String NAME = "Tag";

    private static final Logger LOGGER = Logger.getLogger(TagTask.class.getName());

    private Expression wskey;
    private Expression snapshot;
    private Expression tag;

    public TagTask() {
    }

    public Expression getWskey() {
        return wskey;
    }

    public void setWskey(Expression wskey) {
        this.wskey = wskey;
    }

    public Expression getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(Expression snapshot) {
        this.snapshot = snapshot;
    }

    public Expression getTag() {
        return tag;
    }

    public void setTag(Expression tag) {
        this.tag = tag;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {

        String wskey = (String) getWskey().getValue(execution);
        String snapshot = (String) getSnapshot().getValue(execution);
        String tag = (String) getTag().getValue(execution);

        try {
            LOGGER.log(Level.FINE, "tagging workspace snapshot...");
            getCoreService().tagWorkspace(wskey, tag, snapshot);
            LOGGER.log(Level.FINE, "workspace snapshot [" + snapshot + "] tagged as [" + tag + "]");
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Workspace Snapshot [" + snapshot + "] tagged as [" + tag + "]"));
        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to tag workspace: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unexpected error while trying to tag workspace snapshot", e);
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}
