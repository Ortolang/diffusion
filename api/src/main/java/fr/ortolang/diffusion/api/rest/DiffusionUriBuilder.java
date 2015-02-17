package fr.ortolang.diffusion.api.rest;

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

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import fr.ortolang.diffusion.OrtolangConfig;

public class DiffusionUriBuilder {
	
	public static UriBuilder getRestUriBuilder() {
		try {
			StringBuffer path = new StringBuffer();
			if ( OrtolangConfig.getInstance().getProperty("server.context") != null && OrtolangConfig.getInstance().getProperty("server.context").length() > 0 ) {
				path.append(OrtolangConfig.getInstance().getProperty("server.context"));
			}
			if ( OrtolangConfig.getInstance().getProperty("api.rest.base.path") != null && OrtolangConfig.getInstance().getProperty("api.rest.base.path").length() > 0 ) {
				path.append(OrtolangConfig.getInstance().getProperty("api.rest.base.path"));
			}
			URI uri;
			if ( OrtolangConfig.getInstance().getProperty("server.port") != null && OrtolangConfig.getInstance().getProperty("server.port").length() > 0 ) {
				uri = new URI(OrtolangConfig.getInstance().getProperty("server.protocol"), null, OrtolangConfig.getInstance().getProperty("server.host"), Integer.parseInt(OrtolangConfig.getInstance().getProperty("server.port")), path.toString(), null, null);
			} else {
				uri = new URI(OrtolangConfig.getInstance().getProperty("server.protocol"), OrtolangConfig.getInstance().getProperty("server.host"), path.toString(), null);
			}
			return UriBuilder.fromUri(uri);
		} catch (Exception e) {
		}
		return null;
	}
	
	public static UriBuilder getBaseUriBuilder() {
		try {
			StringBuffer path = new StringBuffer();
			if ( OrtolangConfig.getInstance().getProperty("server.context") != null && OrtolangConfig.getInstance().getProperty("server.context").length() > 0 ) {
				path.append(OrtolangConfig.getInstance().getProperty("server.context"));
			}
			URI uri;
			if ( OrtolangConfig.getInstance().getProperty("server.port") != null && OrtolangConfig.getInstance().getProperty("server.port").length() > 0 ) {
				uri = new URI(OrtolangConfig.getInstance().getProperty("server.protocol"), null, OrtolangConfig.getInstance().getProperty("server.host"), Integer.parseInt(OrtolangConfig.getInstance().getProperty("server.port")), path.toString(), null, null);
			} else {
				uri = new URI(OrtolangConfig.getInstance().getProperty("server.protocol"), OrtolangConfig.getInstance().getProperty("server.host"), path.toString(), null);
			}
			return UriBuilder.fromUri(uri);
		} catch (Exception e) {
		}
		return null;
	}

}
