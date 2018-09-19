package fr.ortolang.diffusion.api.config;

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
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.OrtolangConfig.Property;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.template.TemplateEngineException;
import org.jboss.resteasy.annotations.GZIP;

import static fr.ortolang.diffusion.OrtolangConfig.getInstance;

@Path("/config")
@Produces({ MediaType.APPLICATION_JSON })
public class ConfigResource {

    private static final Logger LOGGER = Logger.getLogger(ConfigResource.class.getName());
    private static String version = null;
    private static String clientConfig = null;
    private static String clientAuthConfig = null;
    private static String adminConfig = null;
    private static String adminAuthConfig = null;

    @Context
    private ServletContext ctx;

    @GET
    @Path("/ping")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response ping() {
        return Response.ok("pong").build();
    }

    public static void clear() {
        clientConfig = null;
        clientAuthConfig = null;
        adminConfig = null;
        adminAuthConfig = null;
    }

    @GET
    @Path("/version")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response version() throws OrtolangException {
        if ( version == null ) {
            try {
                InputStream manifestStream = ctx.getResourceAsStream("META-INF/MANIFEST.MF");
                Manifest manifest = new Manifest(manifestStream);
                Attributes attributes = manifest.getMainAttributes();
                version = attributes.getValue("API-Version");
            } catch(IOException ex) {
                LOGGER.log(Level.WARNING, "Error while reading version: " + ex.getMessage());
                throw new OrtolangException("Unable to read version");
            }
        }
        return Response.ok(version).build();
    }

    @GET
    @Path("/client")
    @Produces({ MediaType.TEXT_PLAIN })
    @GZIP
    public Response getClientConfig() throws TemplateEngineException {
        if (clientConfig == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("var OrtolangConfig = {};\r\n");
            builder.append("OrtolangConfig.apiServerUrlDefault='").append(getInstance().getProperty(Property.API_URL_SSL)).append("';\r\n");
            builder.append("OrtolangConfig.apiServerUrlNoSSL='").append(getInstance().getProperty(Property.API_URL_NOSSL)).append("';\r\n");
            builder.append("OrtolangConfig.apiContentPath='").append(getInstance().getProperty(Property.API_PATH_CONTENT)).append("';\r\n");
            builder.append("OrtolangConfig.apiSubPath='").append(getInstance().getProperty(Property.API_PATH_SUB)).append("';\r\n");
            builder.append("OrtolangConfig.piwikHost='").append(getInstance().getProperty(Property.PIWIK_HOST)).append("';\r\n");
            builder.append("OrtolangConfig.piwikSiteId='").append(getInstance().getProperty(Property.PIWIK_SITE_ID)).append("';\r\n");
            builder.append("OrtolangConfig.keycloakConfigLocation ='").append(getInstance().getProperty(Property.API_URL_SSL)).append("/config/client/auth").append("';\r\n");
            builder.append("OrtolangConfig.staticSiteVersion ='").append(getInstance().getProperty(Property.STATIC_SITE_VERSION)).append("';\r\n");
            builder.append("OrtolangConfig.handlePrefix ='").append(getInstance().getProperty(Property.HANDLE_PREFIX)).append("';\r\n");
            builder.append("OrtolangConfig.marketServerUrl ='").append(getInstance().getProperty(Property.MARKET_SERVER_URL)).append("';\r\n");
            builder.append("OrtolangConfig.cacheVersion ='").append(getInstance().getProperty(Property.CACHE_VERSION)).append("';\r\n");
            clientConfig = builder.toString();
        }
        return Response.ok(clientConfig).build();
    }

    @GET
    @Path("/client/auth")
    @Produces({ MediaType.APPLICATION_JSON })
    @GZIP
    public Response getClientKeycloakConfig() throws TemplateEngineException {
        if (clientAuthConfig == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("{\r\n");
            builder.append("\t\"realm\": \"").append(getInstance().getProperty(Property.AUTH_REALM)).append("\",\r\n");
            if (getInstance().getProperty(Property.AUTH_CLIENT_PUBKEY) != null) {
            	builder.append("\t\"realm-public-key\": \"").append(getInstance().getProperty(Property.AUTH_CLIENT_PUBKEY)).append("\",\r\n");
            }
            builder.append("\t\"auth-server-url\": \"").append(getInstance().getProperty(Property.AUTH_SERVER_URL)).append("\",\r\n");
            builder.append("\t\"ssl-required\": \"external\",\r\n");
            builder.append("\t\"resource\": \"").append(getInstance().getProperty(Property.AUTH_CLIENT)).append("\",\r\n");
            builder.append("\t\"public-client\": true\r\n");
            builder.append("}");
            clientAuthConfig = builder.toString();
        }
        return Response.ok(clientAuthConfig).build();
    }

    @GET
    @Path("/admin")
    @Produces({ MediaType.TEXT_PLAIN })
    @GZIP
    public Response getAdminConfig() throws TemplateEngineException {
        if (adminConfig == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("var OrtolangConfig = {};\r\n");
            builder.append("OrtolangConfig.apiServerUrlDefault='").append(getInstance().getProperty(Property.API_URL_SSL)).append("';\r\n");
            builder.append("OrtolangConfig.apiServerUrlNoSSL='").append(getInstance().getProperty(Property.API_URL_NOSSL)).append("';\r\n");
            builder.append("OrtolangConfig.apiContentPath='").append(getInstance().getProperty(Property.API_PATH_CONTENT)).append("';\r\n");
            builder.append("OrtolangConfig.apiSubPath='").append(getInstance().getProperty(Property.API_PATH_SUB)).append("';\r\n");
            builder.append("OrtolangConfig.piwikHost='").append(getInstance().getProperty(Property.PIWIK_HOST)).append("';\r\n");
            builder.append("OrtolangConfig.piwikSiteId='").append(getInstance().getProperty(Property.PIWIK_SITE_ID)).append("';\r\n");
            builder.append("OrtolangConfig.keycloakConfigLocation ='").append(getInstance().getProperty(Property.API_URL_SSL)).append("/config/admin/auth").append("';\r\n");
            builder.append("OrtolangConfig.staticSiteVersion ='").append(getInstance().getProperty(Property.STATIC_SITE_VERSION)).append("';\r\n");
            builder.append("OrtolangConfig.marketServerUrl ='").append(getInstance().getProperty(Property.MARKET_SERVER_URL)).append("';\r\n");
            adminConfig = builder.toString();
        }
        return Response.ok(adminConfig).build();
    }

    @GET
    @Path("/admin/auth")
    @Produces({ MediaType.APPLICATION_JSON })
    @GZIP
    public Response getAdminKeycloakConfig() throws TemplateEngineException {
        if (adminAuthConfig == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("{\r\n");
            builder.append("\t\"realm\": \"").append(getInstance().getProperty(Property.AUTH_REALM)).append("\",\r\n");
            if (getInstance().getProperty(Property.AUTH_CLIENT_PUBKEY) != null) {
            	builder.append("\t\"realm-public-key\": \"").append(getInstance().getProperty(Property.AUTH_CLIENT_PUBKEY)).append("\",\r\n");
            }
            builder.append("\t\"auth-server-url\": \"").append(getInstance().getProperty(Property.AUTH_SERVER_URL)).append("\",\r\n");
            builder.append("\t\"ssl-required\": \"external\",\r\n");
            builder.append("\t\"resource\": \"admin\",\r\n");
            builder.append("\t\"public-client\": true\r\n");
            builder.append("}");
            adminAuthConfig = builder.toString();
        }
        return Response.ok(adminAuthConfig).build();
    }

}
