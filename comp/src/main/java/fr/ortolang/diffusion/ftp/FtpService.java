package fr.ortolang.diffusion.ftp;

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

import fr.ortolang.diffusion.OrtolangService;

public interface FtpService extends OrtolangService {
    
    public static final String SERVICE_NAME = "ftp";
    
    public static final String INFO_SERVER_HOST = "ftp.server.host";
    public static final String INFO_SERVER_PORT = "ftp.server.port";
    public static final String INFO_SERVER_STATE = "ftp.server.state";
    public static final String INFO_LOGIN_FAILURE_DELAY = "ftp.login.failure.delay";
    public static final String INFO_MAX_ANON_LOGIN = "ftp.login.anon.max";
    public static final String INFO_MAX_LOGIN_FAILURES = "ftp.login.failures.max";
    public static final String INFO_MAX_LOGIN = "ftp.login.max";
    public static final String INFO_MAX_THREADS = "ftp.threads.max";
    public static final String INFO_ANON_LOGIN_ENABLES = "ftp.login.anon.enabled";
    
	
	public void suspend();
	
	public void resume();
	
	public boolean checkUserExistence(String username) throws FtpServiceException;
	
	public boolean checkUserAuthentication(String username, String password) throws FtpServiceException;
	
	public String getInternalAuthenticationPassword(String username) throws FtpServiceException;

}
