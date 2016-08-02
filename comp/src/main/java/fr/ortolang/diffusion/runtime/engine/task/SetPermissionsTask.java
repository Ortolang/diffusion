package fr.ortolang.diffusion.runtime.engine.task;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;

import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;

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

public class SetPermissionsTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(UpdateProcessStatusTask.class.getName());
    public static final String NAME = "Set Permissions";

    private Expression key;
    private Expression permissions;
    private Expression subject;

    public SetPermissionsTask() {
    }

    public Expression getKey() {
        return key;
    }

    public void setKey(Expression key) {
        this.key = key;
    }

    public Expression getPermissions() {
        return permissions;
    }

    public void setPermissions(Expression permissions) {
        this.permissions = permissions;
    }

    public Expression getSubject() {
        return subject;
    }

    public void setSubject(Expression subject) {
        this.subject = subject;
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        try {
            String skey = (String) getKey().getValue(execution);
            String ssubject = (String) getSubject().getValue(execution);
            List<String> slpermissions = Arrays.asList(((String) getPermissions().getValue(execution)).split(","));
            LOGGER.log(Level.FINE, "setting permissions for key: " + skey + ", and subject: " + ssubject + ", to: " + slpermissions);
            getSecurityService().systemSetRule(skey, ssubject, slpermissions);
        } catch (Exception e) {
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "unable to set permissions: " + e.getMessage()));
            throw new RuntimeEngineTaskException("error while setting permissions", e);
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

}