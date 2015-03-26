package fr.ortolang.diffusion.api.ssh.shell;

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

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;

public class SSHShellFactory implements Factory<Command> {
    
	private static final Logger LOGGER = Logger.getLogger(SSHShellFactory.class.getName());
    private List<String> commands;
    private ThreadFactory threadFactory;
    
    public SSHShellFactory(ThreadFactory threadFactory) {
    	commands = new Vector<String>();
    	this.threadFactory = threadFactory;
    }

    @Override
    public SSHShell create() {
        LOGGER.debug("creating a new shell");
        return new SSHShell(commands, threadFactory);
    }
    
    public void importCommands(String packageName) throws SSHShellException {
        if (commands.contains(packageName)) {
            throw new SSHShellException("A package with name " + packageName + " is already imported");
        }
        commands.add(packageName);
    }
}
