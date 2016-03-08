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

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.template.TemplateEngineException;
import org.jboss.resteasy.annotations.GZIP;

@Path("/config")
@Produces({ MediaType.APPLICATION_JSON })
public class ConfigResource {

    private static final Logger LOGGER = Logger.getLogger(ConfigResource.class.getName());
    private static String version = null;

    @Context
    private ServletContext ctx;

    @GET
    @Path("/ping")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response ping() {
        return Response.ok("pong").build();
    }

    @GET
    @Path("/version")
    @Produces({ MediaType.TEXT_PLAIN })
    public Response version() throws Exception {
        if ( version == null ) {
            try {
                InputStream manifestStream = ctx.getResourceAsStream("META-INF/MANIFEST.MF");
                Manifest manifest = new Manifest(manifestStream);
                Attributes attributes = manifest.getMainAttributes();
                version = attributes.getValue("API-Version");
            } catch(IOException ex) {
                LOGGER.log(Level.WARNING, "Error while reading version: " + ex.getMessage());
                throw new Exception("Unable to read version");
            }
        }
        return Response.ok(version).build();
    }

    @GET
    @Path("/client")
    @Produces({ MediaType.TEXT_PLAIN })
    @GZIP
    public Response getClientConfig() throws TemplateEngineException {
        StringBuilder builder = new StringBuilder();
        builder.append("var OrtolangConfig = {};\r\n");
        builder.append("OrtolangConfig.apiServerUrlDefault='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_SSL)).append("';\r\n");
        builder.append("OrtolangConfig.apiServerUrlNoSSL='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_NOSSL)).append("';\r\n");
        builder.append("OrtolangConfig.apiContentPath='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_PATH_CONTENT)).append("';\r\n");
        builder.append("OrtolangConfig.apiSubPath='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_PATH_SUB)).append("';\r\n");
        builder.append("OrtolangConfig.piwikHost='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_HOST)).append("';\r\n");
        builder.append("OrtolangConfig.piwikSiteId='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_SITE_ID)).append("';\r\n");
        builder.append("OrtolangConfig.keycloakConfigLocation ='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_SSL)).append("/config/client/auth").append("';\r\n");
        builder.append("OrtolangConfig.staticSiteVersion ='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.STATIC_SITE_VERSION)).append("';\r\n");
        builder.append("OrtolangConfig.handlePrefix ='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX)).append("';\r\n");
        builder.append("OrtolangConfig.marketServerUrl ='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.MARKET_SERVER_URL)).append("';\r\n");
        return Response.ok(builder.toString()).build();
    }

    @GET
    @Path("/client/auth")
    @Produces({ MediaType.APPLICATION_JSON })
    @GZIP
    public Response getClientKeycloakConfig() throws TemplateEngineException {
        StringBuilder builder = new StringBuilder();
        builder.append("{\r\n");
        builder.append("\t\"realm\": \"").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.AUTH_REALM)).append("\",\r\n");
        builder.append("\t\"realm-public-key\": \"").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.AUTH_CLIENT_PUBKEY)).append("\",\r\n");
        builder.append("\t\"auth-server-url\": \"").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.AUTH_SERVER_URL)).append("\",\r\n");
        builder.append("\t\"ssl-required\": \"external\",\r\n");
        builder.append("\t\"resource\": \"").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.AUTH_CLIENT)).append("\",\r\n");
        builder.append("\t\"public-client\": true\r\n");
        builder.append("}");
        return Response.ok(builder.toString()).build();
    }

    @GET
    @Path("/admin")
    @Produces({ MediaType.TEXT_PLAIN })
    @GZIP
    public Response getAdminConfig() throws TemplateEngineException {
        StringBuilder builder = new StringBuilder();
        builder.append("var OrtolangConfig = {};\r\n");
        builder.append("OrtolangConfig.apiServerUrlDefault='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_SSL)).append("';\r\n");
        builder.append("OrtolangConfig.apiServerUrlNoSSL='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_NOSSL)).append("';\r\n");
        builder.append("OrtolangConfig.apiContentPath='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_PATH_CONTENT)).append("';\r\n");
        builder.append("OrtolangConfig.apiSubPath='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_PATH_SUB)).append("';\r\n");
        builder.append("OrtolangConfig.piwikHost='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_HOST)).append("';\r\n");
        builder.append("OrtolangConfig.piwikSiteId='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_SITE_ID)).append("';\r\n");
        builder.append("OrtolangConfig.keycloakConfigLocation ='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_SSL)).append("/config/admin/auth").append("';\r\n");
        builder.append("OrtolangConfig.staticSiteVersion ='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.STATIC_SITE_VERSION)).append("';\r\n");
        return Response.ok(builder.toString()).build();
    }

    @GET
    @Path("/admin/auth")
    @Produces({ MediaType.APPLICATION_JSON })
    @GZIP
    public Response getAdminKeycloakConfig() throws TemplateEngineException {
        StringBuilder builder = new StringBuilder();
        builder.append("{\r\n");
        builder.append("\t\"realm\": \"").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.AUTH_REALM)).append("\",\r\n");
        builder.append("\t\"realm-public-key\": \"").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.AUTH_CLIENT_PUBKEY)).append("\",\r\n");
        builder.append("\t\"auth-server-url\": \"").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.AUTH_SERVER_URL)).append("\",\r\n");
        builder.append("\t\"ssl-required\": \"external\",\r\n");
        builder.append("\t\"resource\": \"admin\",\r\n");
        builder.append("\t\"public-client\": true\r\n");
        builder.append("}");
        return Response.ok(builder.toString()).build();
    }

}
