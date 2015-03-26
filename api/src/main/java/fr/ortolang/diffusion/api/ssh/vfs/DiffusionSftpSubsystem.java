package fr.ortolang.diffusion.api.ssh.vfs;

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

import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.sftp.SftpSubsystem;

public class DiffusionSftpSubsystem extends SftpSubsystem {
	
	protected final static Logger LOGGER = Logger.getLogger(DiffusionSftpSubsystem.class.getName());
	private ThreadFactory factory;
	
    public static class Factory implements NamedFactory<Command> {

    	private ThreadFactory threadFactory;
    	
        public Factory(ThreadFactory threadFactory) {
        	this.threadFactory = threadFactory;
        }

        public Command create() {
        	DiffusionSftpSubsystem dsftp = new DiffusionSftpSubsystem();
        	dsftp.setThreadFactory(threadFactory);
        	return dsftp;
        }

        public String getName() {
            return "sftp";
        }
    }
    
    public void setThreadFactory(ThreadFactory factory) {
    	this.factory = factory;
    }
    
    @Override
    public void start(Environment env) throws IOException {
    	LOGGER.log(Level.INFO, "Creating new thread for diffusion sftp subsystem");
    	Thread thread = factory.newThread(this);
    	thread.start();
    }

}
