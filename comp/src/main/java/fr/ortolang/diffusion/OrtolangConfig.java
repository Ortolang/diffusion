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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrtolangConfig {

    private static final String CURRENT_CONFIG_VERSION = "7";
    private static final Logger LOGGER = Logger.getLogger(OrtolangConfig.class.getName());
    private static OrtolangConfig config;
    private Properties props;
    private Path home;

    private OrtolangConfig() throws Exception {
        if ( System.getenv("ORTOLANG_HOME") != null ) {
            home = Paths.get(System.getenv("ORTOLANG_HOME"));
        } else {
            home = Paths.get(System.getProperty("user.home"), ".ortolang");
        }
        if ( !Files.exists(home) ) {
            Files.createDirectories(home);
        }
        LOGGER.log(Level.INFO, "ORTOLANG_HOME set to : " + home);

        props = new Properties();
        Path configFilePath = Paths.get(home.toString(), "config.properties");
        if ( !Files.exists(configFilePath) ) {
            Files.copy(OrtolangConfig.class.getClassLoader().getResourceAsStream("config.properties"), configFilePath);
        }
        try (InputStream in = Files.newInputStream(configFilePath) ) {
            props.load(in);
            String fileVersion = this.getProperty(Property.CONFIG_VERSION);
            if ( !fileVersion.equals(CURRENT_CONFIG_VERSION) ) {
                LOGGER.log(Level.SEVERE, "Configuration File Version mismatch with Current Config Version: " + fileVersion + " != " + CURRENT_CONFIG_VERSION + "  --> UPDATE CONFIGURATION FILE");
                throw new Exception("Version mismatch between config file version: " + fileVersion + " and current config version: " + CURRENT_CONFIG_VERSION + " !! Please update your config file");
            }
        }
    }

    public static synchronized OrtolangConfig getInstance() {
        if (config == null) {
            try {
                config = new OrtolangConfig();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "unable to load configuration", e);
                throw new RuntimeException("unable to load configuration", e);
            }
        }
        return config;
    }

    public Path getHomePath() {
        return home;
    }

    public String getProperty(OrtolangConfig.Property property) {
        return props.getProperty(property.key());
    }

    public enum Property {

        CONFIG_VERSION ("config.version"),
        HANDLE_PREFIX ("handle.prefix"),
        DATE_FORMAT_PATTERN ("date.format.pattern"),

        API_URL_SSL ("api.url.ssl"),
        API_URL_NOSSL ("api.url.nossl"),
        API_CONTEXT ("api.context"),
        API_PATH_OBJECTS ("api.path.objects"),
        API_PATH_CONTENT ("api.path.content"),
        API_PATH_SUB ("api.path.sub"),
        API_PATH_OAI ("api.path.oai"),

        AUTH_SERVER_URL ("auth.server.url"),
        AUTH_CLIENT_PUBKEY ("auth.pubkey"),
        AUTH_REALM ("auth.realm"),
        AUTH_CLIENT ("auth.client"),

        RUNTIME_DEFINITIONS ("runtime.definitions"),

        THUMBNAIL_GENERATORS ("thumbnail.generators"),

        FTP_SERVER_HOST ("ftp.server.host"),
        FTP_SERVER_PORT ("ftp.server.port"),

        PIWIK_HOST ("piwik.host"),
        PIWIK_SITE_ID ("piwik.site.id"),

        ZIP_IGNORED_FILES ("zip.ignored.files"),

        STATIC_SITE_VERSION ("static.site.version");

        private final String key;

        private Property(String name) {
            this.key = name;
        }

        public String key() {
            return key;
        }

    }

}