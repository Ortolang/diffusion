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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.OrtolangObjectPid;
import fr.ortolang.diffusion.core.entity.TagElement;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.runtime.engine.TransactionnalRuntimeEngineTask;

public class GenerateHandlesTask extends TransactionnalRuntimeEngineTask {

    public static final String NAME = "Generate Handles";

    private static final Logger LOGGER = Logger.getLogger(GenerateHandlesTask.class.getName());

    private Expression wskey;
    private Expression snapshot;

    public GenerateHandlesTask() {
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

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {

        String wskey = (String) getWskey().getValue(execution);
        String snapshot = (String) getSnapshot().getValue(execution);
        
        try {
            LOGGER.log(Level.FINE, "building handles list...");
            Workspace workspace = getCoreService().readWorkspace(wskey);
            TagElement te = workspace.findTagBySnapshot(snapshot);
            Set<OrtolangObjectPid> pids = getCoreService().buildWorkspacePidList(wskey, te.getName());
            LOGGER.log(Level.FINE, "handles list built with: " + pids.size() + " entries");
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "handles list built with: " + pids.size() + " entries"));

            LOGGER.log(Level.FINE, "starting handle creation...");
            StringBuilder report = new StringBuilder();
            for (OrtolangObjectPid pid : pids) {
                try {
                    getHandleStore().recordHandle(pid.getName(), pid.getKey(), pid.getTarget());
                    report.append(pid).append(" OK\r\n");
                } catch (Exception e) {
                    throw new RuntimeEngineTaskException("unexpected error during publish task execution", e);
                }
            }
            LOGGER.log(Level.FINE, "handles created");
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "handle generation done"));
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Handle Generation report : \r\n" + report.toString(), null));
        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to generate handles: " + e.getMessage()));
            throw new RuntimeEngineTaskException("unable to generate handles", e);
        } 
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

    @Override
    public int getTransactionTimeout() {
        return 1200;
    }
}
