package fr.ortolang.diffusion.api.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.api.template.TemplateEngineException;

@Path("/config")
@Produces({ MediaType.APPLICATION_JSON })
public class ConfigResource {
	
	private static final Logger LOGGER = Logger.getLogger(ConfigResource.class.getName());
	private static String version = null;
	
	@Context
	private ServletContext ctx;
	
	@GET
	@Path("/ping")
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
		builder.append("OrtolangConfig.keycloakConfigLocation ='").append(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_URL_SSL)).append("/config/client/auth").append("';\r\n");
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
	
	@GET
    @Path("/server/exists")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response fileExists(@QueryParam("path") String path) throws TemplateEngineException {
	    SecurityManager manager = System.getSecurityManager();
        StringBuilder builder = new StringBuilder();
        builder.append("{\r\n");
        builder.append("\t\"security-manager-exists\": \"").append(manager != null).append("\",\r\n");
        File file = new File(path);
        java.nio.file.Path p = Paths.get(path);
        String encUsed = Charset.defaultCharset().displayName();
        builder.append("\t\"charset used\": \"").append(encUsed).append("\",\r\n");
        builder.append("\t\"file class name\": \"").append(file.getClass().getName()).append("\",\r\n");
        builder.append("\t\"query-path\": \"").append(path).append("\",\r\n");
        builder.append("\t\"file.getAbsolutePath()\": \"").append(file.getAbsolutePath()).append("\",\r\n");
        builder.append("\t\"file.getAbsolutePath() (in bytes)\": \"");
        for ( byte b : file.getAbsolutePath().getBytes() ) {
            builder.append(b + " ");
        }
        builder.append("\",\r\n");
        builder.append("\t\"file.exists()\": \"").append(file.exists()).append("\",\r\n");
        builder.append("\t\"file.getFreeSpace()\": \"").append(file.getFreeSpace()).append("\",\r\n");
        builder.append("\t\"path.isAbsolute()\": \"").append(p.isAbsolute()).append("\",\r\n");
        try {
            builder.append("\t\"path.getFileSystem()\": \"").append(p.getFileSystem()).append("\",\r\n");
            builder.append("\t\"path.toFile().exists\": \"").append(p.toFile().exists()).append("\",\r\n");
        } catch (Exception e) {
            //
        }
        builder.append("\t\"path.toString() (in bytes)\": \"");
        for ( byte b : p.toString().getBytes() ) {
            builder.append(b + " ");
        }
        builder.append("\",\r\n");
        builder.append("\t\"Files.exists(path)\": \"").append(Files.exists(p)).append("\",\r\n");
        builder.append("\t\"Files.notExists(path)\": \"").append(Files.notExists(p)).append("\",\r\n");
        builder.append("\t\"Files.getFileStore(path).name()\": \"");
        try {
            builder.append(Files.getFileStore(p).name());
        } catch (IOException e) {
            //
        }
        builder.append("\",\r\n");
        builder.append("\t\"Files.isDirectory(path)\": \"").append(Files.isDirectory(p)).append("\",\r\n");
        builder.append("\t\"Files.isRegularFile(path)\": \"").append(Files.isRegularFile(p)).append("\"\r\n");
        builder.append("}");
        return Response.ok(builder.toString()).build();
    }

}
