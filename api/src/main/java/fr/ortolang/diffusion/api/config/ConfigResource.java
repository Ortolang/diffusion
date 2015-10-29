package fr.ortolang.diffusion.api.config;

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
	public Response getClientConfig() throws TemplateEngineException {
		StringBuilder builder = new StringBuilder();
		builder.append("var OrtolangConfig = {};\r\n");
		builder.append("OrtolangConfig.logoutRedirectUrl='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.AUTH_LOGOUT_REDIRECT)).append("';\r\n");
		builder.append("OrtolangConfig.apiServerUrlDefault='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_SSL)).append("';\r\n");
		builder.append("OrtolangConfig.apiServerUrlNoSSL='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_NOSSL)).append("';\r\n");
		builder.append("OrtolangConfig.apiContentPath='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_PATH_CONTENT)).append("';\r\n");
		builder.append("OrtolangConfig.apiSubPath='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_PATH_SUB)).append("';\r\n");
		builder.append("OrtolangConfig.piwikHost='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_HOST)).append("';\r\n");
		builder.append("OrtolangConfig.piwikSiteId='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.PIWIK_SITE_ID)).append("';\r\n");
		builder.append("OrtolangConfig.keycloakConfigLocation ='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_SSL)).append("/config/client/auth").append("';\r\n");
		builder.append("OrtolangConfig.staticSiteVersion ='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.STATIC_SITE_VERSION)).append("';\r\n");
		return Response.ok(builder.toString()).build();
	}
	
	@GET
	@Path("/client/auth")
	@Produces({ MediaType.APPLICATION_JSON })
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
	
}
