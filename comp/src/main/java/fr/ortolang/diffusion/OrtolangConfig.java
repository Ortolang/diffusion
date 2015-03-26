package fr.ortolang.diffusion;

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

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrtolangConfig {

	private static final Logger LOGGER = Logger.getLogger(OrtolangConfig.class.getName());
	private static OrtolangConfig config;
	private Properties props;
	
	private OrtolangConfig(String configFilePath) throws Exception {
        props = new Properties();
        FileInputStream in = null;
        try {
        	in = new FileInputStream(configFilePath);
        	props.load(in);
        } finally {
        	if ( in != null ) {
        		in.close();
        	}
        }
    }

    private OrtolangConfig(URL configFileURL) throws Exception {
        props = new Properties();
        InputStream in = null;
        try {
        	in = configFileURL.openStream();
        	props.load(in);
        } finally {
        	if ( in != null ) {
        		in.close();
        	}
        }
    }

    public static synchronized OrtolangConfig getInstance() {
        try {
            if (config == null) {
                String configFilePath = System.getProperty("ortolang.config.file");
                if (configFilePath != null && configFilePath.length() != 0) {
                    config = new OrtolangConfig(configFilePath);
                    LOGGER.log(Level.INFO, "using custom config file : " + configFilePath);
                } else {
                    URL configFileURL = OrtolangConfig.class.getClassLoader().getResource("config.properties");
                    config = new OrtolangConfig(configFileURL);
                    LOGGER.log(Level.INFO, "using default config file : " + configFileURL.getPath());
                }
            }

            return config;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to load configuration", e);
        }
        return null;
    }
    
    public String getHome() {
    	String home = this.getProperty("home");
		if ( home.startsWith("~") ) {
			home = System.getProperty("user.home") + home.substring(1);
		}
		if ( home.startsWith("$HOME") ) {
			home = System.getProperty("user.home") + home.substring(5);
		}
		return home;
    }

	public String getProperty(String name) {
		return props.getProperty(name);
	}
	
	public Properties getProperties() {
		return props;
	}

}